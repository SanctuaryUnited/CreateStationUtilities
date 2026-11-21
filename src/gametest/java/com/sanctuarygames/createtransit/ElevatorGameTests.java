package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.elevator.*;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class ElevatorGameTests {
 static void doorPair(GameTestHelper h,BlockPos p,net.minecraft.world.level.block.state.BlockState state){
  for(int row=0;row<2;row++){
   var q=p.above(row);h.getLevel().setBlock(q,state.setValue(ElevatorDoorBlock.ROW,row),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
   if(h.getLevel().getBlockEntity(q) instanceof ElevatorPartEntity be)be.onLoad();
  }
 }
 static void sides(ElevatorDriveEntity d,boolean front,boolean rear){
  for(var info:d.cabin().getContraption().getBlocks().values())if(info.state().getBlock() instanceof ElevatorDoorBlock){
   boolean expected=info.state().getValue(ElevatorPartBlock.FACING)==Direction.SOUTH?front:rear;
   check(info.state().getValue(ElevatorDoorBlock.OPEN)==expected,"Cabin door opens only on served side: "+info.pos());
  }
 }
 @GameTest(template="elevator_shaft",batch="elevators",timeoutTicks=1000)
 public static void through_cabin_front_rear_front_stops_close_unserved_sides_and_double_travel_speed(GameTestHelper h){
  var original=fixture(h,true);var origin=original.getBlockPos().offset(-3,-2,-1);
  // Move the drive and its guide column to the west wall, leaving the rear entry clear.
  for(int y=2;y<31;y++)h.getLevel().setBlock(origin.offset(3,y,1),Blocks.AIR.defaultBlockState(),3);
  var drivePos=origin.offset(1,2,3);
  h.getLevel().setBlock(drivePos,ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.EAST),3);
  for(int y=3;y<31;y++)h.getLevel().setBlock(origin.offset(1,y,3),ElevatorRegistry.GUIDE_RAIL.get().defaultBlockState(),3);
  var d=(ElevatorDriveEntity)h.getLevel().getBlockEntity(drivePos);d.onLoad();
  doorPair(h,origin.offset(3,3,2),ElevatorRegistry.GLASS_DOOR.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.NORTH));
  h.getLevel().setBlock(origin.offset(3,5,2),ElevatorRegistry.DOOR_HEADER.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.NORTH),3);
  for(int row=0;row<2;row++)h.getLevel().setBlock(origin.offset(3,15+row,5),Blocks.AIR.defaultBlockState(),3);
  var rearLanding=origin.offset(3,15,1);
  var rearState=ElevatorRegistry.GLASS_LANDING_DOOR.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.SOUTH);
  doorPair(h,rearLanding,rearState);
  // A nearby landing at the same height must not open without a matching doorway.
  var unrelated=origin.offset(4,15,5);doorPair(h,unrelated,ElevatorRegistry.GLASS_LANDING_DOOR.get().defaultBlockState());
  check(ElevatorDriveEntity.travelDuration(12)==93&&ElevatorDriveEntity.travelDuration(24)==185,"Travel duration uses doubled speed");
  check(ElevatorDriveEntity.travelDuration(0)==15,"Short-trip minimum is halved");
  check(d.assemble(),"Two-sided cabin assembles with side-mounted drive");sides(d,true,false);
  d.requestFloor(d.anchor().getY()+12);
  final int[] stage={0};final int[] movingTicks={0};
  h.onEachTick(()->{
   if(d.phase()==ElevatorDriveEntity.MOVING){sides(d,false,false);if(stage[0]==0)movingTicks[0]++;}
   check(!h.getLevel().getBlockState(unrelated).getValue(ElevatorDoorBlock.OPEN),"Unrelated same-height landing stays closed");
   if(d.phase()!=ElevatorDriveEntity.IDLE)return;
   if(stage[0]==0&&Math.abs(d.offset()-12)<.001){
    check(movingTicks[0]==93,"12-block travel takes exactly 93 moving ticks");sides(d,false,true);
    check(h.getLevel().getBlockState(rearLanding).getValue(ElevatorDoorBlock.OPEN),"Rear landing opens at middle floor");
    // Wrong orientation and incomplete landing pairs fail closed.
    doorPair(h,rearLanding,rearState.setValue(ElevatorPartBlock.FACING,Direction.NORTH));
    d.requestFloor(d.currentFloor());sides(d,false,false);
    doorPair(h,rearLanding,rearState);h.getLevel().setBlock(rearLanding.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
    d.requestFloor(d.currentFloor());sides(d,false,false);
    doorPair(h,rearLanding,rearState);d.requestFloor(d.currentFloor());sides(d,false,true);
    stage[0]=1;d.requestFloor(d.anchor().getY()+24);
   }else if(stage[0]==1&&Math.abs(d.offset()-24)<.001){sides(d,true,false);stage[0]=2;d.requestFloor(d.anchor().getY());}
   else if(stage[0]==2&&Math.abs(d.offset())<.001){sides(d,true,false);restored(d);h.succeed();}
  });
 }

 @GameTest(template="elevator_shaft",batch="elevators",timeoutTicks=800)
 public static void exported_glass_through_quickbuild_selects_landing_sides(GameTestHelper h){exportedThrough(h,true);}
 @GameTest(template="elevator_shaft",batch="elevators",timeoutTicks=800)
 public static void exported_solid_through_quickbuild_selects_landing_sides(GameTestHelper h){exportedThrough(h,false);}
 static void exportedThrough(GameTestHelper h,boolean glass){var d=fixture(h,glass,true);check(d.assemble(),"Actual exported through QuickBuild assembles");sides(d,true,false);d.requestFloor(d.anchor().getY()+12);final int[] stage={0};h.onEachTick(()->{if(d.phase()==ElevatorDriveEntity.MOVING)sides(d,false,false);if(d.phase()!=ElevatorDriveEntity.IDLE)return;if(stage[0]==0&&Math.abs(d.offset()-12)<.001){sides(d,false,true);stage[0]=1;d.requestFloor(d.anchor().getY()+24);}else if(stage[0]==1&&Math.abs(d.offset()-24)<.001){sides(d,true,false);restored(d);h.succeed();}});}
 static void check(boolean ok,String m){if(!ok)throw new GameTestAssertException(m);}
 static ElevatorDriveEntity fixture(GameTestHelper h,boolean glass){return fixture(h,glass,false);}
 static ElevatorDriveEntity fixture(GameTestHelper h,boolean glass,boolean through){try{var stream=ElevatorGameTests.class.getResourceAsStream("/schematics/Elevator-"+(glass?"Glass":"Solid")+(through?"-Through":"")+"-3x3-Three-Floors.nbt");check(stream!=null,"Exported QuickBuild is packaged in tests");var n=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());var palette=n.getList("palette",10);var cells=n.getList("blocks",10);var origin=h.absolutePos(new BlockPos(2,2,2));for(int i=0;i<cells.size();i++){var b=cells.getCompound(i);var pos=b.getList("pos",3);var q=origin.offset(pos.getInt(0),pos.getInt(1),pos.getInt(2));var state=NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),palette.getCompound(b.getInt("state")));h.getLevel().setBlock(q,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);if(h.getLevel().getBlockEntity(q) instanceof ElevatorPartEntity be){be.loadWithComponents(b.getCompound("nbt"),h.getLevel().registryAccess());be.onLoad();}}return (ElevatorDriveEntity)h.getLevel().getBlockEntity(origin.offset(through?1:3,2,through?3:1));}catch(java.io.IOException e){throw new RuntimeException(e);}}
 static void restored(ElevatorDriveEntity d){var c=d.cabin().getContraption();var origin=d.anchor().above((int)Math.round(d.offset()));d.disassemble();for(var info:c.getBlocks().values())check(d.getLevel().getBlockState(origin.offset(info.pos())).is(info.state().getBlock()),"All captured modules including both door halves restore: "+info.pos());}
 static void rider(ElevatorDriveEntity d,boolean ascending){var e=d.cabin();Vec3 floor=e.toGlobalVector(new Vec3(.5,1.005,1.5),1);var probe=new ArmorStand(e.level(),floor.x,floor.y,floor.z);probe.setNoGravity(true);e.level().addFreshEntity(probe);/* A standing rider inherits the cabin velocity, then gravity acts relative to it. */probe.setDeltaMovement(0,e.position().subtract(e.getPrevPositionVec()).y-.14,0);try{var method=ContraptionCollider.class.getDeclaredMethod("collideEntities",AbstractContraptionEntity.class);method.setAccessible(true);method.invoke(null,e);}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}check(probe.onGround()&&e.collidingEntities.containsKey(probe),"Create native collision supports standing rider");double dy=e.position().subtract(e.getPrevPositionVec()).y;probe.move(MoverType.SELF,probe.getDeltaMovement());check(probe.getY()>=e.getY()+Math.min(0,dy)+.99,"Standing rider stays above next-tick floor");check(ascending?e.getContactPointMotion(floor).y>0:e.getContactPointMotion(floor).y<0,"Native contact velocity follows travel direction");probe.discard();}
 @GameTest(template="elevator_shaft",batch="elevators",timeoutTicks=50)
 public static void exported_glass_cabin_uses_dedicated_blocks_complete_floor_and_three_registered_stops(GameTestHelper h){var d=fixture(h,true);var partIds=ElevatorRegistry.parts().stream().map(b->b.getId().getPath()).toList();check(partIds.equals(java.util.List.of("elevator_floor","elevator_solid_wall","elevator_solid_corner","elevator_roof","elevator_ceiling_light","elevator_ceiling_strip","elevator_ceiling_double_strip","elevator_ceiling_panel","elevator_ceiling_bulb","elevator_ceiling_ring","elevator_led_strip","elevator_door_header","elevator_control_panel","elevator_guide_rail","elevator_floor_marker_rail","elevator_glass_wall","elevator_glass_corner","elevator_glass_door","elevator_solid_door","elevator_glass_landing_door","elevator_solid_landing_door","elevator_drive","elevator_floor_call","elevator_destination_selector","elevator_floor_marker")),"25 dedicated elevator items, including the DEV17 destination selector: "+partIds);check(ElevatorNetwork.floors(d).length==3,"Calls register three floors");check(d.assemble(),"QuickBuild assembles without Create pulley");var c=d.cabin().getContraption();check(c instanceof ElevatorContraption&&c.getBlocks().size()==42,"All 42 cabin pieces captured");check(c.bounds.getXsize()==3&&c.bounds.getYsize()==5&&c.bounds.getZsize()==3,"3x3 cabin and three clear interior rows");for(int x=-1;x<2;x++)for(int z=0;z<3;z++)check(c.getBlocks().get(new BlockPos(x,0,z)).state().is(ElevatorRegistry.FLOOR.get()),"Every floor square exists");restored(d);h.succeed();}
 @GameTest(template="elevator_shaft",batch="elevators",timeoutTicks=1200)
 public static void glass_travel_up_down_has_landing_interlocks_and_native_standing_collision(GameTestHelper h){var d=fixture(h,true);check(d.assemble(),"Assemble");d.requestFloor(d.anchor().getY()+24);final boolean[] up={false},down={false};h.onEachTick(()->{if(d.phase()==ElevatorDriveEntity.MOVING){for(var p:ElevatorNetwork.landings(d))check(!h.getLevel().getBlockState(p).getValue(ElevatorDoorBlock.OPEN),"Every landing is closed in travel");if(d.offset()>3&&d.offset()<20&&!up[0]){rider(d,true);up[0]=true;}if(d.offset()>3&&d.offset()<20&&d.targetFloor()==d.anchor().getY()&&!down[0]){rider(d,false);down[0]=true;}}
 if(up[0]&&d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset()-24)<.001)d.requestFloor(d.anchor().getY());if(down[0]&&d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset())<.001){restored(d);h.succeed();}});}
 @GameTest(template="elevator_shaft",batch="elevators",timeoutTicks=1100)
 public static void solid_queued_stops_and_saved_controller_rebind_keep_dye_and_restore_doors(GameTestHelper h){var d=fixture(h,false);var wall=d.anchor().offset(-1,3,1);((ElevatorPartEntity)h.getLevel().getBlockEntity(wall)).setColors(14,5);check(d.assemble(),"Assemble");d.requestFloor(d.anchor().getY()+24);d.requestFloor(d.anchor().getY()+12);final ElevatorDriveEntity[] current={d};final boolean[] saved={false},top={false};h.onEachTick(()->{var drive=current[0];if(drive.phase()==ElevatorDriveEntity.MOVING&&drive.offset()>3&&!saved[0]){var tag=drive.saveWithFullMetadata(h.getLevel().registryAccess());var pos=drive.getBlockPos();var state=drive.getBlockState();h.getLevel().removeBlockEntity(pos);var replacement=new ElevatorDriveEntity(pos,state);replacement.loadWithComponents(tag,h.getLevel().registryAccess());h.getLevel().setBlockEntity(replacement);replacement.onLoad();current[0]=replacement;saved[0]=true;}if(drive.phase()==ElevatorDriveEntity.OPENING&&Math.abs(drive.offset()-24)<.001)top[0]=true;if(top[0]&&drive.phase()==ElevatorDriveEntity.IDLE&&Math.abs(drive.offset()-12)<.001){restored(drive);var be=(ElevatorPartEntity)h.getLevel().getBlockEntity(wall.above(12));check(be.paint()==14&&be.accent()==5,"Dye survives moving save/rebind/restoration at current floor");h.succeed();}});}
 @GameTest(template="elevator_shaft",batch="elevators",timeoutTicks=600)
 public static void blocked_shaft_stops_before_motion_and_redstone_call_retries_after_clearance(GameTestHelper h){var d=fixture(h,false);check(d.assemble(),"Assemble");var blocker=d.anchor().above(8);h.getLevel().setBlock(blocker,Blocks.STONE.defaultBlockState(),3);d.requestFloor(d.anchor().getY()+12);final boolean[] stopped={false};h.onEachTick(()->{if(d.phase()==ElevatorDriveEntity.FAULT&&!stopped[0]){check(d.offset()==0,"No movement enters blocked shaft");h.getLevel().setBlock(blocker,Blocks.AIR.defaultBlockState(),3);var call=d.getBlockPos().offset(2,13,4);h.getLevel().setBlock(call.relative(Direction.EAST),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);check(h.getLevel().getBlockState(call).getValue(ElevatorCallBlock.POWERED),"Rising redstone pulse calls floor");stopped[0]=true;}if(stopped[0]&&d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset()-12)<.001){restored(d);h.succeed();}});}
 @GameTest(template="elevator_shaft",batch="elevators",timeoutTicks=50)
 public static void door_easing_signals_and_drive_break_restore_every_cabin_module(GameTestHelper h){var d=fixture(h,true);var closed=ElevatorRegistry.GLASS_DOOR.get().defaultBlockState();var be=new ElevatorPartEntity(BlockPos.ZERO,closed);check(be.statusColor(1,0)==0x111820,"Closed status is dark");be.setBlockState(closed.setValue(ElevatorDoorBlock.OPEN,true));be.advance(true);check(be.progress(1)>0&&be.progress(1)<.01,"Door starts eased, not snapped");check(be.statusColor(1,0)!=be.statusColor(1,6),"Opening flashes green");for(int i=1;i<40;i++)be.advance(true);check(be.progress(1)==1&&be.statusColor(1,6)==0x32ff69,"Fully open is steady green");be.setBlockState(closed);be.advance(false);check(be.statusColor(1,0)==0xffd22a&&be.statusColor(1,6)==0x362b04,"Closing flashes yellow");check(d.assemble(),"Assemble");var c=d.cabin().getContraption();var origin=d.anchor();h.getLevel().destroyBlock(d.getBlockPos(),false);for(var info:c.getBlocks().values())check(h.getLevel().getBlockState(origin.offset(info.pos())).is(info.state().getBlock()),"Removing drive restores complete cabin");h.succeed();}
}
