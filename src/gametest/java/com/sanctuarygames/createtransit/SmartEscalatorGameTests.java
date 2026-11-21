package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.TransitBlockItem;
import com.sanctuarygames.createtransit.escalator.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import static com.sanctuarygames.createtransit.escalator.EscalatorBlock.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class SmartEscalatorGameTests {
 private static List<BlockPos> ramp(BlockPos bottom,Direction f,int inclines){
  var positions=new ArrayList<BlockPos>();positions.add(bottom);
  for(int i=0;i<inclines;i++)positions.add(bottom.relative(f,i+1).above(i));
  positions.add(bottom.relative(f,inclines+1).above(inclines));return positions;
 }
 private static EscalatorEntity be(GameTestHelper h,BlockPos p){return (EscalatorEntity)h.getLevel().getBlockEntity(p);}
 private static void put(GameTestHelper h,List<BlockPos> positions,SmartEscalatorBlock block,Direction facing,boolean reverse){for(var p:positions)h.getLevel().setBlock(p,block.defaultBlockState().setValue(FACING,facing).setValue(REVERSED,reverse),Block.UPDATE_ALL);}
 private static void clear(GameTestHelper h,List<BlockPos> positions){for(var p:positions){h.getLevel().removeBlock(p,false);h.getLevel().removeBlock(p.below(),false);}}
 private static void checkRamp(GameTestHelper h,List<BlockPos> positions,boolean powered){
  double phase=be(h,positions.getFirst()).phase(0);
  for(int i=0;i<positions.size();i++){
   var p=positions.get(i);var s=h.getLevel().getBlockState(p);var expected=i==0?Kind.LOWER_LANDING:i==positions.size()-1?Kind.UPPER_LANDING:Kind.INCLINE;
   h.assertTrue(kind(s)==expected,"Automatic shape at "+i+": expected "+expected+", actual "+kind(s));
   h.assertTrue(s.getValue(STOPPED)==powered,"Default operation follows this item's power mode");
   h.assertTrue(Math.abs(be(h,p).phase(0)-phase)<1e-7,"All smart shapes share the same exact phase");
   if(expected!=Kind.INCLINE)h.assertTrue(s.getValue(SLOPE_JOIN),"Landing selects the matching mitered handrail joint");
  }
  h.assertTrue(EscalatorNetwork.group(h.getLevel(),positions.getFirst()).containsAll(positions),"Whole smart ramp belongs to one control network");
 }

 @GameTest(template="escalator_strip",batch="smart_escalators",timeoutTicks=110)
 public static void all_skins_power_modes_facings_and_widths_choose_correct_shapes(GameTestHelper h){
  var origin=h.absolutePos(new BlockPos(12,1,12));var old=new ArrayList<BlockPos>();int scenario=0;
  for(boolean powered:new boolean[]{false,true})for(boolean glass:new boolean[]{true,false})for(var f:Direction.Plane.HORIZONTAL){
   final int time=scenario++*6+1;var lane=ramp(origin,f,3);var side=lane.stream().map(p->p.relative(f.getClockWise())).toList();
   h.runAtTickTime(time,()->{clear(h,old);old.clear();old.addAll(lane);old.addAll(side);put(h,lane,EscalatorRegistry.smart(glass,powered),f,false);put(h,side,EscalatorRegistry.smart(glass,powered),f,false);});
   h.runAtTickTime(time+4,()->{checkRamp(h,lane,powered);checkRamp(h,side,powered);h.assertTrue(EscalatorNetwork.group(h.getLevel(),lane.getFirst()).size()==lane.size()*2,"Both lanes share one complete network");for(int i=0;i<lane.size();i++){var a=h.getLevel().getBlockState(lane.get(i));var b=h.getLevel().getBlockState(side.get(i));h.assertTrue(a.getValue(RIGHT)&&b.getValue(LEFT),"Widened smart shapes remove only the shared handrail");h.assertTrue(!a.getValue(LEFT)&&!b.getValue(RIGHT),"Outside rails remain present");}});
  }
  h.runAtTickTime(100,h::succeed);
 }

 private static void itemPlacement(GameTestHelper h,boolean topDown){
  var origin=h.absolutePos(new BlockPos(12,1,12));var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);var old=new ArrayList<BlockPos>();int scenario=0;
  for(boolean powered:new boolean[]{false,true})for(boolean glass:new boolean[]{true,false}){
   var f=Direction.from2DDataValue(scenario);final int time=scenario++*8+1;var positions=ramp(origin,f,3);var block=EscalatorRegistry.smart(glass,powered);
   h.runAtTickTime(time,()->{
    clear(h,old);old.clear();old.addAll(positions);player.setShiftKeyDown(false);player.setYRot(f.toYRot());
    player.setItemInHand(InteractionHand.MAIN_HAND,TransitBlockItem.finished(block,14,9));player.getMainHandItem().setCount(positions.size()+1);
    var order=new ArrayList<>(positions);if(topDown)Collections.reverse(order);
    for(var p:order){
     h.getLevel().removeBlock(p,false);h.getLevel().setBlock(p.below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);player.setPos(Vec3.atCenterOf(p).add(0,3,0));
     var hit=new BlockHitResult(Vec3.atCenterOf(p.below()).add(0,.5,0),Direction.UP,p.below(),false);
     var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
     h.assertTrue(result.consumesAction()&&h.getLevel().getBlockState(p).is(block),"Native BlockItem placement result="+result+" block="+h.getLevel().getBlockState(p)+" target="+p+" facing="+f+" stack="+player.getMainHandItem()+" player="+player.position());
    }
    h.assertTrue(player.getMainHandItem().getCount()==1,"One item consumed per section, no automatic extra blocks");
   });
   h.runAtTickTime(time+5,()->{checkRamp(h,positions,powered);for(var p:positions){h.assertTrue(be(h,p).paint()==14&&be(h,p).accent()==9,"Automatic shape preserves both placed dye channels");var state=h.getLevel().getBlockState(p);var picked=block.getCloneItemStack(state,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false),h.getLevel(),p,player);h.assertTrue(picked.is(block.asItem())&&TransitBlockItem.paint(picked).getId()==14&&TransitBlockItem.accent(picked).getId()==9,"Every shape picks the same dyed item");}});
  }
  h.runAtTickTime(35,h::succeed);
 }
 @GameTest(template="escalator_strip",batch="smart_escalators",timeoutTicks=50) public static void one_item_places_full_ramp_bottom_up(GameTestHelper h){itemPlacement(h,false);}
 @GameTest(template="escalator_strip",batch="smart_escalators",timeoutTicks=50) public static void one_item_places_full_ramp_top_down(GameTestHelper h){itemPlacement(h,true);}

 @GameTest(template="escalator_strip",batch="smart_escalators",timeoutTicks=65)
 public static void extending_removing_widening_and_wrenching_preserve_entity_colors_and_clock(GameTestHelper h){
  var f=Direction.NORTH;var positions=ramp(h.absolutePos(new BlockPos(12,1,16)),f,2);var block=EscalatorRegistry.smart(true,false);put(h,positions,block,f,false);
  var top=positions.getLast();var reference=be(h,top);reference.setColors(14,9);var extra=top.relative(f).above();var outer=top.east();
  h.runAtTickTime(5,()->{checkRamp(h,positions,false);h.getLevel().setBlock(extra,block.defaultBlockState(),Block.UPDATE_ALL);});
  h.runAtTickTime(10,()->{h.assertTrue(kind(h.getLevel().getBlockState(top))==Kind.INCLINE,"Adding a higher piece turns the former top landing into an incline");h.assertTrue(be(h,top)==reference&&reference.paint()==14&&reference.accent()==9,"No BE replacement or lost dye when the shape changes");h.getLevel().removeBlock(extra,false);});
  h.runAtTickTime(15,()->{checkRamp(h,positions,false);h.assertTrue(be(h,top)==reference,"Removing the extension restores the landing in place");h.getLevel().setBlock(outer,block.defaultBlockState(),Block.UPDATE_ALL);});
  h.runAtTickTime(20,()->{h.assertTrue(kind(h.getLevel().getBlockState(outer))==Kind.UPPER_LANDING,"An unfinished wider lane inherits its row's landing height");h.assertTrue(h.getLevel().getBlockState(top).getValue(RIGHT)&&h.getLevel().getBlockState(outer).getValue(LEFT),"Side joining follows the resolved shape");var player=TransitTestPlayers.makeMockPlayer(h,GameType.CREATIVE);double before=reference.phase(0);com.sanctuarygames.createtransit.escalator.EscalatorTestAccess.reverse(h.getLevel(),top);for(var p:EscalatorNetwork.group(h.getLevel(),top)){h.assertTrue(h.getLevel().getBlockState(p).getValue(REVERSED),"Wrench reverses all smart sections");h.assertTrue(Math.abs(be(h,p).phase(0)-before)<1e-7,"Wrench reverses without a phase jump");}h.getLevel().removeBlock(outer,false);});
  h.runAtTickTime(25,()->{h.assertTrue(!h.getLevel().getBlockState(top).getValue(RIGHT),"Removing the wider lane restores the outer handrail");h.assertTrue(reference.paint()==14&&reference.accent()==9,"All topology changes retain frame and LED dyes");h.succeed();});
 }

 @GameTest(template="escalator_strip",batch="smart_escalators",timeoutTicks=65)
 public static void redstone_signal_runs_whole_powered_belt_and_modes_remain_independent(GameTestHelper h){
  var f=Direction.NORTH;var a=ramp(h.absolutePos(new BlockPos(10,1,16)),f,2);var b=a.stream().map(BlockPos::east).toList();var normal=a.stream().map(p->p.east(2)).toList();
  put(h,a,EscalatorRegistry.smart(true,true),f,false);put(h,b,EscalatorRegistry.smart(false,true),f,false);put(h,normal,EscalatorRegistry.smart(true,false),f,false);
  var power=b.getLast().below();final double[] phases=new double[2];
  h.runAtTickTime(5,()->{for(var p:a)h.assertTrue(h.getLevel().getBlockState(p).getValue(STOPPED),"Unpowered redstone escalator is stationary");h.assertTrue(EscalatorNetwork.group(h.getLevel(),a.getFirst()).size()==a.size()*2,"Glass and solid powered lanes share one widened run");h.assertTrue(!h.getLevel().getBlockState(b.getFirst()).getValue(RIGHT)&&!h.getLevel().getBlockState(normal.getFirst()).getValue(LEFT),"Different power modes retain their separating rails");phases[0]=be(h,a.getFirst()).phase(0);});
  h.runAtTickTime(12,()->{h.assertTrue(Math.abs(be(h,a.getFirst()).phase(0)-phases[0])<1e-7,"No power freezes treads and handrail clock");h.getLevel().setBlock(power,Blocks.REDSTONE_BLOCK.defaultBlockState(),Block.UPDATE_ALL);});
  h.runAtTickTime(17,()->{for(var p:a)h.assertTrue(!h.getLevel().getBlockState(p).getValue(STOPPED),"A signal at the far landing starts every connected piece");h.assertTrue(!h.getLevel().getBlockState(normal.getFirst()).getValue(STOPPED),"Signal does not spread into the regular belt's network");phases[1]=be(h,a.getFirst()).phase(0);});
  h.runAtTickTime(25,()->{h.assertTrue(Math.abs(be(h,a.getFirst()).phase(0)-phases[1])>.01,"Powered motion really advances");h.getLevel().removeBlock(power,false);});
  h.runAtTickTime(30,()->{for(var p:a)h.assertTrue(h.getLevel().getBlockState(p).getValue(STOPPED),"Power removal stops the whole run");phases[0]=be(h,a.getFirst()).phase(0);});
  h.runAtTickTime(38,()->{h.assertTrue(Math.abs(be(h,a.getFirst()).phase(0)-phases[0])<1e-7,"Stopped clock stays fixed after power removal");h.succeed();});
 }

 @GameTest(template="empty",timeoutTicks=40)
 public static void side_face_placement_inherits_lane_facing_and_reverse(GameTestHelper h){
  var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,1,3));var target=p.south();var block=EscalatorRegistry.smart(true,false);
  level.setBlock(p,block.defaultBlockState().setValue(FACING,Direction.EAST).setValue(REVERSED,true),Block.UPDATE_ALL);level.removeBlock(target,false);
  var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(target).add(0,3,0));player.setYRot(0);player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block.asItem(),2));
  var hit=new BlockHitResult(Vec3.atCenterOf(p).add(0,0,.5),Direction.SOUTH,p,false);
  var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
  h.assertTrue(result.consumesAction()&&level.getBlockState(target).is(block),"Side-face click places one new section");
  h.assertTrue(level.getBlockState(target).getValue(FACING)==Direction.EAST&&level.getBlockState(target).getValue(REVERSED),"Widening inherits the clicked lane even when the player looks elsewhere");
  h.runAtTickTime(4,()->{h.assertTrue(level.getBlockState(p).getValue(RIGHT)&&level.getBlockState(target).getValue(LEFT),"Actual item placement removes the two inside rails");h.succeed();});
 }

 @GameTest(template="escalator_strip",batch="smart_escalators",timeoutTicks=190)
 public static void powered_smart_ramp_carries_a_rider_pauses_and_reaches_upper_landing(GameTestHelper h){
  var positions=ramp(h.absolutePos(new BlockPos(12,1,16)),Direction.NORTH,4);put(h,positions,EscalatorRegistry.smart(true,true),Direction.NORTH,false);
  var lower=positions.getFirst();var upper=positions.getLast();var rider=new net.minecraft.world.entity.decoration.ArmorStand(h.getLevel(),lower.getX()+.5,lower.getY()+EscalatorGeometry.DECK,lower.getZ()+.5);h.getLevel().addFreshEntity(rider);double start=rider.getZ();final double[] paused={0};
  h.runAtTickTime(6,()->{h.assertTrue(Math.abs(rider.getZ()-start)<1e-7,"An unpowered smart escalator does not carry its rider");h.getLevel().setBlock(lower.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),Block.UPDATE_ALL);});
  h.runAtTickTime(40,()->{h.assertTrue(start-rider.getZ()>.9,"Power carries the rider onto the dynamically selected incline");h.getLevel().removeBlock(lower.below(),false);});
  h.runAtTickTime(46,()->{paused[0]=rider.getZ();h.assertTrue(h.getLevel().getBlockState(positions.get(1)).getValue(STOPPED),"Incline and landings stop together");});
  h.runAtTickTime(55,()->{h.assertTrue(Math.abs(rider.getZ()-paused[0])<1e-7,"The rider stays supported on the stationary tread");h.getLevel().setBlock(upper.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),Block.UPDATE_ALL);});
  h.runAtTickTime(168,()->{h.assertTrue(rider.getZ()<upper.getZ()+.9,"A signal at the other end resumes the rider through all smart inclines");h.assertTrue(Math.abs(rider.getY()-(upper.getY()+EscalatorGeometry.DECK))<.2,"Rider reaches the actual upper landing floor");h.succeed();});
 }

 @GameTest(template="empty",timeoutTicks=40)
 public static void smart_shape_uses_confirmed_shared_collision_and_selection_caches(GameTestHelper h){
  var p=h.absolutePos(new BlockPos(2,1,2));
  for(var block:EscalatorRegistry.SMART){
   var state=block.get().defaultBlockState().setValue(SmartEscalatorBlock.SHAPE,Kind.INCLINE).setValue(STOPPED,true);h.getLevel().setBlock(p,state,Block.UPDATE_CLIENTS);var entity=be(h,p);entity.clock(.110123,h.getLevel().getGameTime(),state);
   var shape=state.getCollisionShape(h.getLevel(),p);h.assertTrue(shape==EscalatorShapes.shape(Kind.INCLINE,false,false,Direction.NORTH,.110123),"Smart inclines use the exact same shared cached collision as the FPS fix");h.assertTrue(shape==state.getCollisionShape(h.getLevel(),p),"Repeated look-at queries reuse the BE cache");
   var bounds=entity.renderBounds();h.getLevel().setBlock(p,state.setValue(SmartEscalatorBlock.SHAPE,Kind.UPPER_LANDING),Block.UPDATE_CLIENTS);h.assertTrue(be(h,p)==entity&&bounds==entity.renderBounds()&&bounds.maxY>=p.getY()+2.25,"Shape changes retain cached bounds large enough for any incline");h.assertTrue(shape!=h.getLevel().getBlockState(p).getCollisionShape(h.getLevel(),p),"Resolved flat landing invalidates the former incline collision");h.getLevel().removeBlock(p,false);
  }h.succeed();
 }
}
