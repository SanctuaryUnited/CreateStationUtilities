package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.elevator.model.*;
import net.minecraft.core.*;import net.minecraft.gametest.framework.*;import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class Elevators30GameTests {
 private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
 private static ElevatorDriveEntity shaft(GameTestHelper h){return shaft(h,10,false);}
 private static ElevatorDriveEntity shaft(GameTestHelper h,int x,boolean middle){var l=h.getLevel();var p=h.absolutePos(new BlockPos(x,2,10));l.setBlock(p,ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.SOUTH),3);var d=(ElevatorDriveEntity)l.getBlockEntity(p);d.onLoad();for(int y=1;y<=9;y++)l.setBlock(p.above(y),(y==1||y==9||middle&&y==5?ElevatorRegistry.FLOOR_MARKER_RAIL:ElevatorRegistry.GUIDE_RAIL).get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.SOUTH),3);d.rescan();check(d.configureModel("size","3")&&d.configureModel("autoCenter","true"),"Shaft config accepted");return d;}
 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=700)
 public static void queued_stop_waits_for_fully_open_dwell(GameTestHelper h){
  var drive=shaft(h,10,true);
  var rider=FakePlayerFactory.getMinecraft(h.getLevel());
  rider.getAbilities().mayBuild=true;
  for(var floor:drive.floorList())check(drive.installModelLanding(rider,floor.y()),"Landing installed");
  check(drive.configureModel("idleCloseTicks","30")&&drive.activateModel(),"Dwell test cabin activated");
  int middle=drive.floorList().get(1).y(),top=drive.floorList().getLast().y();
  check(drive.requestFloor(middle)&&drive.requestFloor(top),"Both passenger floors queued");
  long[] opened={0};
  h.startSequence().thenWaitUntil(()->{
   var motion=drive.model().state();
   check(motion.state()==CabinMotionController.State.DOORS_OPEN&&Math.abs(motion.y()-middle)<.001,"Middle doors fully open");
  }).thenExecute(()->opened[0]=h.getLevel().getGameTime()).thenExecuteAfter(25,()->{
   check(drive.model().state().state()==CabinMotionController.State.DOORS_OPEN,"Queued top stop cannot cut the door dwell short");
   check(drive.model().entity().front(1)>.999,"Door leaves stayed fully open");
  }).thenWaitUntil(()->{
   var motion=drive.model().state();
   check(motion.state()==CabinMotionController.State.DOORS_OPEN&&Math.abs(motion.y()-top)<.001,"Next stop was served");
   check(h.getLevel().getGameTime()-opened[0]>=30,"At least 30 ticks elapsed");
  }).thenSucceed();
 }

 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=100)
 public static void exact_body_grid_landing_and_independent_colors_reload(GameTestHelper h){var d=shaft(h);check(d.activateModel(),"Cabin activates: "+d.model().warning());var e=d.model().entity();check(Math.abs(e.getBoundingBox().getXsize()-2.875)<1e-9&&Math.abs(e.getBoundingBox().getZsize()-2.9375)<1e-9&&Math.abs(e.getBoundingBox().getYsize()-2.8)<1e-9,"Exact entity extents");var player=FakePlayerFactory.getMinecraft(h.getLevel());player.getAbilities().mayBuild=true;for(var floor:d.floorList())check(d.installModelLanding(player,floor.y()),"Landing install: "+d.model().warning());var pos=d.model().landingPosition(d.anchor().getY(),false);var be=(ModelLandingEntity)h.getLevel().getBlockEntity(pos);check(be.offsetAcross==0&&be.offsetDepth==0,"Grid landing has no fractional translation");check(h.getLevel().getBlockState(pos.above(2)).getBlock() instanceof ModelLandingBlock,"Landing remains three rows tall");
  for(int col=0;col<3;col++)for(int row=0;row<3;row++){var cell=ModelLandingBlock.part(pos,e.facing(),col,row);var state=h.getLevel().getBlockState(cell);var shape=state.getCollisionShape(h.getLevel(),cell);if(!shape.isEmpty()){var b=shape.bounds();check(b.minX>=-1e-9&&b.minY>=-1e-9&&b.minZ>=-1e-9&&b.maxX<=1+1e-9&&b.maxY<=1+1e-9&&b.maxZ<=1+1e-9,"Landing collision stays in its block");}}
  check(d.configureModel("lighting","PANEL")&&d.configureModel("ceilingColor","14")&&d.configureModel("lighting","LINEAR")&&d.configureModel("ceilingColor","11")&&d.configureModel("lighting","PANEL"),"Server saves style memory");check(d.model().config().ceilingColor()==14,"Panel color restored");check(d.configureModel("brightness","7")&&d.configureModel("handrail","DARK"),"Brightness and finish set");var copy=new ModelCabinController(d);copy.load(d.model().save());check(copy.config().equals(d.model().config()),"All settings round-trip through Minecraft NBT");e.configure(copy.config());check(CabinNbt.config(CabinNbt.config(e.configuration())).equals(copy.config()),"Entity config serialization");h.succeed();}
 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=100)
 public static void obstructed_placement_is_refused_without_deleting_blocks(GameTestHelper h){var d=shaft(h);var e=new ModelCabinEntity(ElevatorRegistry.MODEL_CABIN.get(),h.getLevel());e.configure(d.model().config());e.bind(d);e.setPos(d.model().placementOrigin(d.anchor().getY()));var pos=BlockPos.containing(e.world(1.5,1,1.5));h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);check(!d.activateModel(),"Static obstruction blocks placement");check(h.getLevel().getBlockState(pos).is(Blocks.STONE),"Player block retained");h.succeed();}
 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=60)
 public static void standalone_three_by_three_landing_owns_all_three_rows(GameTestHelper h){var base=h.absolutePos(new BlockPos(6,2,6));ModelLandingBlock.install(h.getLevel(),base,3,Direction.SOUTH,false,0,11);for(int col=0;col<3;col++)for(int row=0;row<3;row++){var state=h.getLevel().getBlockState(ModelLandingBlock.part(base,Direction.SOUTH,col,row));check(state.getBlock() instanceof ModelLandingBlock&&state.getValue(ModelLandingBlock.KIT3),"Standalone 3x3 assembly owns every rendered row");}h.succeed();}
 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=600)
 public static void unattended_doors_close_after_opening_clear_passage(GameTestHelper h){var d=shaft(h);check(d.activateModel(),"Cabin activates");var player=FakePlayerFactory.getMinecraft(h.getLevel());player.getAbilities().mayBuild=true;for(var floor:d.floorList())check(d.installModelLanding(player,floor.y()),"Landing installed");int destination=d.floorList().getLast().y();check(d.model().request(destination),"Trip accepted");h.startSequence().thenWaitUntil(()->{var state=d.model().state();check(state.state()!=CabinMotionController.State.FAULT,"No trip fault: "+state.fault());check(state.state()==CabinMotionController.State.DOORS_OPEN&&Math.abs(state.y()-destination)<1e-9,"Cabin arrives and opens");check(Math.abs(d.model().entity().getY()-(destination+.875))<1e-9,"Exact floor stop");var c=d.model().config();var sensor=CabinGeometry.doorSensor(c,false);check(sensor.z1()-sensor.z0()==.25,"Sensor spans four pixels of depth");check(CabinGeometry.width(c)==2,"Full two-block aperture");var base=d.model().landingPosition(destination,false);var center=ModelLandingBlock.part(base,d.model().entity().facing(),1,0);check(h.getLevel().getBlockState(center).getCollisionShape(h.getLevel(),center).isEmpty(),"Open landing has no center collision");}).thenWaitUntil(()->{check(d.model().state().state()==CabinMotionController.State.IDLE&&d.model().doorsClosed(),"Unattended doors close automatically");}).thenSucceed();}
 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=220)
 public static void reverts_and_reapplies_without_replacing_cabin_or_landings(GameTestHelper h){
  var d=shaft(h);
  check(d.configureModel("rotate","clockwise")&&d.configureModel("rearEnabled","true"),"Side guide rail and rear entrance enabled");
  check(d.activateModel(),"Cabin activates: "+d.model().warning());
  var entity=d.model().entity();
  var player=FakePlayerFactory.getMinecraft(h.getLevel());
  player.getAbilities().mayBuild=true;
  for(var floor:d.floorList())check(d.installModelLanding(player,floor.y()),"Landing installed: "+d.model().warning());
  var position=entity.position();var id=entity.getUUID();
  var front=d.model().landingPosition(d.floorList().getFirst().y(),false);
  var rear=d.model().landingPosition(d.floorList().getFirst().y(),true);
  var frontEntity=h.getLevel().getBlockEntity(front);
  var rearEntity=h.getLevel().getBlockEntity(rear);
  check(frontEntity instanceof ModelLandingEntity&&rearEntity instanceof ModelLandingEntity,"Both landings placed");
  h.runAfterDelay(3,()->{
   check(d.configureModel("revertCabin","true"),"Revert allowed at stopped floor: "+d.model().warning());
   check(entity.configuration().v13Geometry(),"Revert synced to cabin entity");
   check(Math.abs(entity.getBoundingBox().getYsize()-2.5)<1e-9,"V13 rollback restores the 2.5-block height");
   check(CabinNbt.config(CabinNbt.config(entity.configuration())).v13Geometry(),"Revert persists through NBT");
   check(CabinGeometry.panelStart(entity.configuration(),false)==.055,"V13 front panel plane restored");
   check(d.configureModel("updateCabin","true"),"Reapply V17: "+d.model().warning());
   check(!entity.configuration().legacyGeometry()&&entity.getUUID().equals(id)&&entity.position().equals(position),"Same cabin and position");
   check(h.getLevel().getBlockEntity(front)==frontEntity&&h.getLevel().getBlockEntity(rear)==rearEntity,"Both linked landing assemblies preserved");
   h.succeed();
  });
 }
 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=100)
 public static void front_and_rear_touchscreens_use_one_stop_queue(GameTestHelper h){
  var d=shaft(h);
  check(d.configureModel("rotate","clockwise")&&d.configureModel("rearEnabled","true")&&d.activateModel(),"Cabin activates with both doors: "+d.model().warning());
  var player=FakePlayerFactory.getMinecraft(h.getLevel());
  player.getAbilities().mayBuild=true;
  for(var floor:d.floorList())check(d.installModelLanding(player,floor.y()),"Install both landings");
  var entity=d.model().entity();
  int target=d.floorList().getLast().y();
  h.runAfterDelay(4,()->{
   check(entity.selectorStops().size()==2,"Both reachable floors appear on the screen");
   var original=player.position();
   player.setPos(entity.world(1.5,CabinGeometry.FLOOR_TOP,1.5));
   for(boolean rear:new boolean[]{true,false,false}){
    int tile=rear?1:0;
    double u=(CabinSelectorLayout.tileX(tile)+6)/32.0-.5;
    double v=(CabinSelectorLayout.tileY(tile)+5)/64.0;
    double x=CabinSelectorLayout.center(entity.configuration(),rear)+(rear?-1:1)*u*CabinSelectorLayout.WIDTH;
    double y=CabinSelectorLayout.BOTTOM+(1-v)*CabinSelectorLayout.HEIGHT;
    var ray=entity.world(x,y,CabinGeometry.passageEnd(entity.configuration(),rear)).subtract(player.getEyePosition());
    player.setYRot((float)Math.toDegrees(Math.atan2(-ray.x,ray.z)));
    player.setXRot((float)-Math.toDegrees(Math.atan2(ray.y,Math.hypot(ray.x,ray.z))));
    entity.interact(player,net.minecraft.world.InteractionHand.MAIN_HAND);
    check(d.model().pending(d.floorList().get(tile).y()),"Click selects the destination from the "+(rear?"rear":"front")+" jamb");
   }
   player.setPos(original);
   check(d.model().state().queue().size()==2,"The two screens share one stop queue with duplicate suppression");
   h.runAfterDelay(2,()->{check(entity.destination()==target,"The selected destination is published to the cabin entity");h.succeed();});
  });
 }

 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=100)
 public static void configuration_copy_includes_radio_and_preserves_landing_links(GameTestHelper h){
  var d=shaft(h);
  var player=FakePlayerFactory.getMinecraft(h.getLevel());
  player.getAbilities().mayBuild=true;
  int floor=d.floorList().getFirst().y();
  check(d.installModelLanding(player,floor),"Install a linked landing");
  var position=d.model().landingPosition(floor,false);
  var landing=h.getLevel().getBlockEntity(position);
  check(d.model().config().brightness()==6&&d.model().copySettings().getDouble("Speed")==.5,"New controller defaults");
  d.model().radio.configure("volume","1.2");
  d.model().radio.configure("reverb","true");
  var copied=d.model().copySettings();
  check(!copied.contains("Policies")&&!copied.contains("Cabin")&&!copied.contains("Motion"),"Clipboard excludes shaft identity and traffic");
  check(d.configureModel("brightness","10")&&d.configureModel("speed","0.2"),"Change target settings");
  d.model().radio.configure("volume","0.1");
  check(d.pasteModelSettings(copied),"Paste settings: "+d.model().warning());
  check(d.model().config().brightness()==6&&d.model().copySettings().getDouble("Speed")==.5,"Copied cabin settings restored");
  check(d.model().radio.volume==1.2&&d.model().radio.reverb,"Radio settings restored");
  check(h.getLevel().getBlockEntity(position)==landing,"The linked landing is the same block entity");
  h.succeed();
 }

 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=520)
 public static void bank_serves_opposite_calls_and_assigns_destination(GameTestHelper h){
  var a=shaft(h,10,true);
  var b=shaft(h,14,true);
  var level=h.getLevel();
  var player=FakePlayerFactory.getMinecraft(level);
  player.getAbilities().mayBuild=true;
  for(var d:java.util.List.of(a,b)){
   check(d.activateModel(),"Activate test shaft: "+d.model().warning());
   for(var f:d.floorList())check(d.installModelLanding(player,f.y()),"Install bank landing: "+d.model().warning());
  }
  int low=a.floorList().getFirst().y(),mid=a.floorList().get(1).y(),high=a.floorList().getLast().y();
  Direction facing=a.model().landingFacing(false);
  int gap=(a.model().landingPosition(low,false).getX()+b.model().landingPosition(low,false).getX())/2
    +(ModelLandingBlock.part(BlockPos.ZERO,facing,1,0).getX());
  var base=a.model().landingPosition(low,false);
  var root=new BlockPos(gap,low+2,base.getZ()).relative(facing.getOpposite());
  var middle=root.atY(mid+2);
  var state=ElevatorRegistry.FLOOR_CALL.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,facing.getOpposite());
  level.setBlock(root,state,3);
  level.setBlock(middle,state,3);
  var banks=ElevatorBanks.get(level);
  check(banks.hasService(level,middle),"Panel between same-side landings is detected at "+middle);
  check(banks.request(level,middle,1,null,null)&&banks.request(level,middle,-1,null,null),"Both automatic directions queued");
  check(banks.pending(level,middle)==3,"Each direction has an independent pending light");
  h.startSequence().thenWaitUntil(()->{
   check(banks.pending(level,middle)==0,"Both calls are served");
   for(var d:java.util.List.of(a,b))check(Math.abs(d.model().state().y()-mid)<.001,"Both cars arrive at the middle landing");
   check(a.model().entity().serviceDirection()==-b.model().entity().serviceDirection(),"Cars show opposite service directions");
  }).thenExecute(()->{
   var error=banks.link(level,root,player,true);
   check(error.isEmpty(),"Create linked bank: "+error);
   check(!banks.letter(a.identifier()).equals(banks.letter(b.identifier())),"Two distinct shaft letters");
   level.setBlock(middle,ElevatorRegistry.DESTINATION_SELECTOR.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,facing.getOpposite()),3);
   check(banks.destinations(level,middle).size()==2,"Selector lists both reachable destinations");
   var selector=(ElevatorPartEntity)level.getBlockEntity(middle);
   ElevatorCallBlock.refreshPanel(level,middle,selector);
   check(selector.destinationFloors.length==2&&selector.destinationFloors[1]==high,"Touchscreen shows two reachable stops in order");
   Direction outside=facing.getOpposite();
   var location=Vec3.atLowerCornerOf(middle).add(.645+outside.getStepX()*.4375,ElevatorCallBlock.destinationCenter(1),.5+outside.getStepZ()*.4375);
   var hit=new net.minecraft.world.phys.BlockHitResult(location,outside,middle,false);
   level.getBlockState(middle).useWithoutItem(level,player,hit);
  }).thenWaitUntil(()->{
   check(banks.assignment(level,middle,player.getUUID()).startsWith("Car "),"Selector reports assigned shaft");
  }).thenWaitUntil(()->{
   check(java.util.List.of(a,b).stream().anyMatch(d->d.model().state().state()==CabinMotionController.State.DOORS_OPEN
      &&Math.abs(d.model().state().y()-high)<.001),"Assigned car opens at the destination");
  }).thenSucceed();
 }

 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=100)
 public static void merge_preserves_primary_letters_and_restores_secondary_bank(GameTestHelper h){
  var level=h.getLevel();
  var player=FakePlayerFactory.getMinecraft(level);
  player.getAbilities().mayBuild=true;
  var a=shaft(h,10,true);var b=shaft(h,14,true);
  var c=shaft(h,26,true);var d=shaft(h,30,true);
  for(var shaft:java.util.List.of(a,b,c,d)){
   for(var stop:shaft.floorList())check(shaft.installModelLanding(player,stop.y()),"Install registered landing");
  }
  int low=a.floorList().getFirst().y(),mid=a.floorList().get(1).y();
  var facing=a.model().landingFacing(false);
  var root=a.model().landingPosition(low,false);
  int gap=(root.getX()+b.model().landingPosition(low,false).getX())/2+
      ModelLandingBlock.part(BlockPos.ZERO,facing,1,0).getX();
  var primary=new BlockPos(gap,low+2,root.getZ()).relative(facing.getOpposite());
  var secondary=primary.offset(16,0,0);
  var state=ElevatorRegistry.FLOOR_CALL.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,facing.getOpposite());
  level.setBlock(primary,state,3);level.setBlock(secondary,state,3);
  var banks=ElevatorBanks.get(level);
  check(banks.link(level,primary,player,true).isEmpty(),"Primary created");
  check(banks.link(level,secondary,player,true).isEmpty(),"Secondary created");
  var primaryLetters=java.util.List.of(banks.letter(a.identifier()),banks.letter(b.identifier()));
  var secondaryLetters=java.util.List.of(banks.letter(c.identifier()),banks.letter(d.identifier()));
  check(banks.mergeAction(level,primary,player,24).isEmpty(),"Primary selected");
  check(banks.mergeAction(level,secondary,player,25).isEmpty(),"Secondary selected");
  check(banks.mergePreview(level,secondary,player).contains("D · "),"Preview contains four cars");
  check(banks.mergeAction(level,secondary,player,26).isEmpty(),"Banks merge");
  check(primaryLetters.equals(java.util.List.of(banks.letter(a.identifier()),banks.letter(b.identifier()))),"Primary keeps both letters");
  check(java.util.Set.of(banks.letter(c.identifier()),banks.letter(d.identifier())).equals(java.util.Set.of("C","D")),"Secondary gets next letters");
  var selector=secondary.atY(mid+2);
  level.setBlock(selector,ElevatorRegistry.DESTINATION_SELECTOR.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,facing.getOpposite()),3);
  check(banks.hasService(level,selector)&&banks.destinations(level,selector).size()==2,"Selector detects merged membership at the secondary landing");
  check(banks.mergeAction(level,primary,player,27).isEmpty(),"Unmerge succeeds");
  check(secondaryLetters.equals(java.util.List.of(banks.letter(c.identifier()),banks.letter(d.identifier()))),"Original secondary letters restored");
  h.succeed();
 }
 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=1300)
 public static void ambient_uses_valid_stops_and_yields_to_passenger(GameTestHelper h){
  var d=shaft(h,10,true);
  var player=FakePlayerFactory.getMinecraft(h.getLevel());
  player.getAbilities().mayBuild=true;
  for(var floor:d.floorList())check(d.installModelLanding(player,floor.y()),"Install landing");
  check(d.activateModel(),"Activate car");
  check(!d.model().save().getBoolean("AmbientEnabled"),"Ambient defaults off");
  check(d.configureModel("ambientEnabled","true")&&d.configureModel("ambientFrequency","HIGH"),"Enable High");
  check(d.configureModel("cabinAnimated","true")&&d.configureModel("landingAnimated","true"),"Independent arrow modes accepted");
  int high=d.floorList().getLast().y();
  h.startSequence().thenWaitUntil(()->{
   var motion=d.model().state();
   check(motion.ambient(),"Ambient started");
   check(d.model().servesStop(motion.target())&&motion.queue().isEmpty(),"Ambient target stays outside passenger queue");
  }).thenExecute(()->{
   check(d.requestFloor(high),"Passenger request accepted during ambient trip");
  }).thenWaitUntil(()->{
   var motion=d.model().state();
   check(!motion.ambient()&&motion.state()==CabinMotionController.State.DOORS_OPEN&&Math.abs(motion.y()-high)<.001,
       "Passenger reaches high landing through the normal door sequence");
  }).thenSucceed();
 }
 @GameTest(template="elevator2_shaft",batch="elevators30",timeoutTicks=100)
 public static void tall_shaft_discovers_stops_past_old_limits(GameTestHelper h){
  var d=shaft(h);
  var pos=d.getBlockPos();
  for(int offset=1;offset<=273;offset++){
   var rail=(offset%8==1?ElevatorRegistry.FLOOR_MARKER_RAIL:ElevatorRegistry.GUIDE_RAIL).get();
   h.getLevel().setBlock(pos.above(offset),rail.defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.SOUTH),2);
  }
  d.rescan();
  check(d.floorList().size()==35,"All 35 connected stops are discovered");
  check(d.floorList().getLast().y()==pos.getY()+272,"A marker above the former 256-block cap is accessible");
  check(d.railWarning().isEmpty(),"Long shaft has no rail warning");
  h.succeed();
 }
}
