package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.elevator.*;import com.sanctuarygames.createtransit.elevator.model.*;
import net.minecraft.core.*;import net.minecraft.gametest.framework.*;import net.neoforged.neoforge.gametest.*;import net.neoforged.neoforge.common.util.FakePlayerFactory;import net.minecraft.world.*;import net.minecraft.world.item.*;import net.minecraft.world.item.context.UseOnContext;import net.minecraft.world.level.block.Blocks;import net.minecraft.world.phys.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class RefresherGameTests {
 private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
 private static String obstacles(ModelCabinEntity e){var b=e.getBoundingBox().inflate(.1,1,.1);var out=new java.util.ArrayList<String>();for(var p:BlockPos.betweenClosed(BlockPos.containing(b.minX,b.minY,b.minZ),BlockPos.containing(b.maxX,b.maxY,b.maxZ))){var s=e.level().getBlockState(p);if(!s.getCollisionShape(e.level(),p).isEmpty())out.add(p.toShortString()+":"+s.getBlock());}return out.toString();}
 @GameTest(template="elevator2_shaft",batch="compact_refresher",timeoutTicks=80) public static void north_refresher_fits_enclosed_shaft(GameTestHelper h){spawn(h,Direction.NORTH);}
 @GameTest(template="elevator2_shaft",batch="compact_refresher",timeoutTicks=80) public static void east_refresher_fits_enclosed_shaft(GameTestHelper h){spawn(h,Direction.EAST);}
 @GameTest(template="elevator2_shaft",batch="compact_refresher",timeoutTicks=80) public static void south_refresher_fits_enclosed_shaft(GameTestHelper h){spawn(h,Direction.SOUTH);}
 @GameTest(template="elevator2_shaft",batch="compact_refresher",timeoutTicks=80) public static void west_refresher_fits_enclosed_shaft(GameTestHelper h){spawn(h,Direction.WEST);}
 private static void spawn(GameTestHelper h,Direction f){var l=h.getLevel();var center=h.absolutePos(new BlockPos(4,3,4));var rail=center.relative(f); // middle of the wall-side interior row
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=6;y++)if(Math.abs(x)==2||Math.abs(z)==2)l.setBlock(center.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
  var placing=FakePlayerFactory.getMinecraft(l);var railStack=new ItemStack(ElevatorRegistry.GUIDE_RAIL.get());var wallHit=new BlockHitResult(Vec3.atCenterOf(rail.relative(f)),f.getOpposite(),rail.relative(f),false);var placed=ElevatorRegistry.GUIDE_RAIL.get().getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(placing,InteractionHand.MAIN_HAND,railStack,wallHit));check(placed.getValue(ElevatorPartBlock.FACING)==f,"Clicked wall face places rail flush against supporting wall");
  for(int y=0;y<=5;y++)l.setBlock(rail.above(y),(y==0||y==5?ElevatorRegistry.FLOOR_MARKER_RAIL:ElevatorRegistry.GUIDE_RAIL).get().defaultBlockState().setValue(ElevatorPartBlock.FACING,f),3);
  var player=FakePlayerFactory.getMinecraft(l);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ElevatorRegistry.CABIN_ITEM.get()));var clicked=f==Direction.NORTH?rail.above(5):rail;var hit=new BlockHitResult(Vec3.atCenterOf(clicked),f.getOpposite(),clicked,false);var item=ElevatorRegistry.CABIN_ITEM.get();check(item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit))==InteractionResult.SUCCESS,"Refresher creates default cabin in walled shaft");
  var d=(ElevatorDriveEntity)l.getBlockEntity(rail.below());check(d!=null&&d.model().active(),"Driver generated beneath rail and cabin active");var e=d.model().entity();check(e!=null,"Real model entity spawned");check(Math.abs(e.getY()-(clicked.getY()-.125))<1e-8,"Cabin spawns at clicked marker floor");check(Math.abs(e.getBoundingBox().getYsize()-2.5)<1e-8,"Cabin is exactly 2.5 blocks tall");var b=e.getBoundingBox();check(b.minX>=center.getX()-1-1e-8&&b.maxX<=center.getX()+2+1e-8&&b.minZ>=center.getZ()-1-1e-8&&b.maxZ<=center.getZ()+2+1e-8,"Cabin fits 3x3 interior");
  var id=e.getUUID();item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));check(d.model().entity().getUUID().equals(id),"Second click never duplicates cabin");check(d.floorList().size()==2,"Both marker stops discovered without extensions");
  h.runAfterDelay(12,()->{var light=BlockPos.containing(e.world(1.5,1,1.5));check(l.getBlockState(light).is(ElevatorRegistry.CABIN_LIGHT.get()),"Real light source inside cabin");check(l.getBlockState(light).getLightEmission(l,light)==15,"Interior source emits level 15");d.model().deactivate();h.runAfterDelay(15,()->{check(!l.getBlockState(light).is(ElevatorRegistry.CABIN_LIGHT.get()),"Temporary light expires after removal");h.succeed();});});
 }
 @GameTest(template="elevator2_shaft",batch="compact_refresher",timeoutTicks=600)
 public static void compact_manual_doors_and_rider_travel(GameTestHelper h){
  var l=h.getLevel();var rail=h.absolutePos(new BlockPos(4,3,4));
  for(int y=0;y<=10;y++)l.setBlock(rail.above(y),(y==0||y==10?ElevatorRegistry.FLOOR_MARKER_RAIL:ElevatorRegistry.GUIDE_RAIL).get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.NORTH),3);
  var player=FakePlayerFactory.getMinecraft(l);player.getAbilities().mayBuild=true;player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ElevatorRegistry.CABIN_ITEM.get()));
  ElevatorRegistry.CABIN_ITEM.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(rail),Direction.SOUTH,rail,false)));
  var d=(ElevatorDriveEntity)l.getBlockEntity(rail.below());check(d!=null&&d.model().active(),"Default compact cabin created");d.onLoad();
  var e=d.model().entity();player.setYRot(e.facing().toYRot());
  for(var floor:d.floorList()){
   var entrance=d.model().landingPosition(floor.y(),false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ElevatorRegistry.LANDING_ITEM.get()));
   var hit=new BlockHitResult(Vec3.atCenterOf(entrance.below()),Direction.UP,entrance.below(),false);
   check(ElevatorRegistry.LANDING_ITEM.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Manual Landing Kit placed");
   check(l.getBlockEntity(entrance) instanceof ModelLandingEntity landing&&d.getBlockPos().equals(landing.markerDrive()),"Manual landing auto-linked");
  }
  var point=e.world(1.5,.125,1.5);var rider=new net.minecraft.world.entity.decoration.ArmorStand(l,point.x,point.y,point.z);l.addFreshEntity(rider);
  var nativeMove=net.minecraft.world.entity.Entity.collideBoundingBox(rider,new Vec3(0,-.1,0),rider.getBoundingBox(),l,java.util.List.of());check(Math.abs(nativeMove.y)<1e-6,"Native floor sweep blocks gravity");
  int home=rail.getY()-1,destination=home+10;check(d.model().request(destination),"Compact trip requested after manual door installation");boolean[] returning={false};
  h.onEachTick(()->{
   var state=d.model().state();check(state.state()!=CabinMotionController.State.FAULT,"Compact trip fault: "+state+obstacles(e));
   check(Math.abs(rider.getY()-(e.getY()+.125))<.03,"Gravity rider stays on floor at tick "+h.getTick()+": "+rider.getY()+" / "+(e.getY()+.125)+" state="+state.state());
   if(state.state()==CabinMotionController.State.DOORS_OPEN&&state.y()==destination&&!returning[0]){check(d.model().request(home),"Return trip queued");returning[0]=true;}
   if(returning[0]&&state.state()==CabinMotionController.State.DOORS_OPEN&&state.y()==home){check(!rider.isNoGravity(),"Gravity stayed enabled during both trips");rider.discard();h.succeed();}
  });
 }
 @GameTest(template="elevator2_shaft",batch="compact_refresher",timeoutTicks=40) public static void compact_panels_fit_pockets_and_roof(GameTestHelper h){for(int size=2;size<=5;size++)for(var mechanism:CabinConfiguration.Mechanism.values())for(boolean twoSpeed:new boolean[]{false,true}){var p=CabinConfiguration.defaults(size,CabinConfiguration.Variant.SOLID).save();p.setProperty("compact","true");p.setProperty("mechanism",mechanism.name());p.setProperty("twoSpeed",Boolean.toString(twoSpeed));p.setProperty("rearEnabled","true");if(size>=3)p.setProperty("kit3","true");var c=CabinConfiguration.load(p);for(int tick=0;tick<=40;tick++){for(var box:DoorGeometry.panelBoxes(c,.25,.125,tick/40.0))check(box.x0()>=-1e-8&&box.x1()<=size+1e-8,"Panels remain within footprint, size "+size);for(var box:CabinGeometry.collision(c,tick/40.0,0))check(box.y1()<=2.5+1e-8&&box.x0()>=0&&box.x1()<=size&&box.z0()>=0&&box.z1()<=size,"Collision stays in footprint and below roof");}check(CabinGeometry.body(c).stream().flatMap(f->java.util.stream.Stream.of(f.a(),f.b(),f.c(),f.d())).mapToDouble(CabinGeometry.Point::y).max().orElseThrow()<=2.5+1e-8,"Mesh fits roof");}h.succeed();}
}
