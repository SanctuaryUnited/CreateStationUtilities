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
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import static com.sanctuarygames.createtransit.escalator.EscalatorBlock.*;

/** Native item placement, live topology changes, and flat/ramp control together. */
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class TravelatorFamilyGameTests {
 private static EscalatorEntity be(GameTestHelper h,BlockPos p){return (EscalatorEntity)h.getLevel().getBlockEntity(p);}
 private static void put(GameTestHelper h,BlockPos p,SmartEscalatorBlock block,Direction f){h.getLevel().setBlock(p,block.defaultBlockState().setValue(FACING,f),Block.UPDATE_ALL);}
 private static void light(GameTestHelper h,List<BlockPos> positions,boolean on){for(var p:positions){var s=h.getLevel().getBlockState(p);h.assertTrue(s.getValue(STOPPED)!=on,"Run power propagates to flat belt and staircase");h.assertTrue(s.getLightEmission(h.getLevel(),p)==(on?4:0),"Unpowered trim emits no native block light");}}

 @GameTest(template="escalator_strip",batch="travelator_family",timeoutTicks=110)
 public static void same_item_places_level_travelators_all_styles_facings_orders_and_widths(GameTestHelper h){
  var origin=h.absolutePos(new BlockPos(12,1,12));var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);var old=new ArrayList<BlockPos>();int scenario=0;
  for(boolean powered:new boolean[]{false,true})for(boolean glass:new boolean[]{true,false})for(var f:Direction.Plane.HORIZONTAL){
   var block=EscalatorRegistry.smart(glass,powered);var lane=new ArrayList<BlockPos>();for(int i=0;i<6;i++)lane.add(origin.relative(f,i));
   var side=lane.stream().map(p->p.relative(f.getClockWise())).toList();final int time=scenario*6+1;boolean backwards=(scenario++%2)==1;
   h.runAtTickTime(time,()->{
    for(var p:old){h.getLevel().removeBlock(p,false);h.getLevel().removeBlock(p.below(),false);}old.clear();old.addAll(lane);old.addAll(side);
    player.setShiftKeyDown(false);player.setYRot(f.toYRot());player.setItemInHand(InteractionHand.MAIN_HAND,TransitBlockItem.finished(block,14,9));player.getMainHandItem().setCount(lane.size()+side.size()+1);
    var order=new ArrayList<>(old);if(backwards)Collections.reverse(order);
    for(var p:order){
     h.getLevel().removeBlock(p,false);h.getLevel().setBlock(p.below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);player.setPos(Vec3.atCenterOf(p).add(0,3,0));
     var hit=new BlockHitResult(Vec3.atCenterOf(p.below()).add(0,.5,0),Direction.UP,p.below(),false);
     var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
     h.assertTrue(result.consumesAction()&&h.getLevel().getBlockState(p).is(block),"Native placement makes a travelator with the escalator item");
    }
    h.assertTrue(player.getMainHandItem().getCount()==1,"A level travelator consumes exactly one family item per tile");
   });
   h.runAtTickTime(time+4,()->{
    for(var p:old){var s=h.getLevel().getBlockState(p);h.assertTrue(kind(s)==Kind.FLAT,"Rounded end housings never turn a level deck into a step");h.assertTrue(s.getValue(STOPPED)==powered,"Travelator uses the same selected control mode");h.assertTrue(be(h,p).paint()==14&&be(h,p).accent()==9,"Frame and LED dyes survive native flat placement");}
    h.assertTrue(EscalatorNetwork.group(h.getLevel(),lane.getFirst()).size()==12,"Flat length and both lanes form one network");
    for(int i=0;i<lane.size();i++){
     h.assertTrue(h.getLevel().getBlockState(lane.get(i)).getValue(RIGHT)&&h.getLevel().getBlockState(side.get(i)).getValue(LEFT),"Widened travelator removes the internal handrail");
     for(var q:List.of(lane.get(i),side.get(i))){var s=h.getLevel().getBlockState(q);h.assertTrue(s.getValue(FLAT_JOIN)==(i>0)&&s.getValue(SLOPE_JOIN)==(i<lane.size()-1),"Only the two real travelator ends select return housings in every facing and placement order");}
    }
   });
  }h.runAtTickTime(100,h::succeed);
 }

 @GameTest(template="escalator_strip",batch="travelator_family",timeoutTicks=45)
 public static void only_elevation_changes_create_steps_and_removal_restores_dyed_flat_tiles(GameTestHelper h){
  var p=h.absolutePos(new BlockPos(12,1,12));var q=p.north();var high=q.north().above();var block=EscalatorRegistry.smart(true,false);
  put(h,p,block,Direction.NORTH);put(h,q,block,Direction.NORTH);var a=be(h,p);var b=be(h,q);a.setColors(14,9);b.setColors(11,4);
  h.runAtTickTime(5,()->{h.assertTrue(kind(h.getLevel().getBlockState(p))==Kind.FLAT&&kind(h.getLevel().getBlockState(q))==Kind.FLAT,"A lone tile and a level neighbour remain flat");h.getLevel().setBlock(high,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);});
  h.runAtTickTime(10,()->{h.assertTrue(kind(h.getLevel().getBlockState(q))==Kind.FLAT,"An unrelated higher block does not turn the belt into an escalator");put(h,high,block,Direction.NORTH);});
  h.runAtTickTime(15,()->{h.assertTrue(kind(h.getLevel().getBlockState(p))==Kind.LOWER_LANDING&&kind(h.getLevel().getBlockState(q))==Kind.INCLINE&&kind(h.getLevel().getBlockState(high))==Kind.UPPER_LANDING,"Real elevated family section creates both landings and one step tile");h.assertTrue(be(h,p)==a&&be(h,q)==b,"Changing between travelator and staircase retains the same block entities");h.getLevel().removeBlock(high,false);});
  h.runAtTickTime(20,()->{h.assertTrue(kind(h.getLevel().getBlockState(p))==Kind.FLAT&&kind(h.getLevel().getBlockState(q))==Kind.FLAT,"Removing the elevated tile restores a flat travelator");h.assertTrue(a.paint()==14&&a.accent()==9&&b.paint()==11&&b.accent()==4,"Topology changes preserve both dye channels");h.assertTrue(Math.abs(a.phase(0)-b.phase(0))<1e-7,"Flat tiles retain a synchronized running clock");h.getLevel().removeBlock(q,false);});
  h.runAtTickTime(25,()->{h.assertTrue(kind(h.getLevel().getBlockState(p))==Kind.FLAT,"The final solitary tile stays flat");h.succeed();});
 }

 @GameTest(template="escalator_strip",batch="travelator_family",timeoutTicks=65)
 public static void powered_flat_ramp_flat_run_switches_lights_motion_and_continuous_landing_rails(GameTestHelper h){
  var p=h.absolutePos(new BlockPos(12,1,16));var block=EscalatorRegistry.smart(false,true);
  var positions=List.of(p.south(2),p.south(),p,p.north(),p.north(2).above(),p.north(3).above(2),p.north(4).above(2),p.north(5).above(2));
  for(var q:positions){put(h,q,block,Direction.NORTH);be(h,q).setColors(14,9);}
  var first=positions.getFirst();var last=positions.getLast();var power=last.below();var lower=p;var upper=p.north(3).above(2);
  var rider=new net.minecraft.world.entity.decoration.ArmorStand(h.getLevel(),first.getX()+.5,first.getY()+EscalatorGeometry.DECK,first.getZ()+.5);h.getLevel().addFreshEntity(rider);final double[] stopped={rider.getZ()};
  h.runAtTickTime(5,()->{
   light(h,positions,false);h.assertTrue(EscalatorNetwork.group(h.getLevel(),first).size()==positions.size(),"Flat approaches and staircase are one powered run");
   h.assertTrue(kind(h.getLevel().getBlockState(lower))==Kind.LOWER_LANDING&&kind(h.getLevel().getBlockState(upper))==Kind.UPPER_LANDING,"Only the immediate slope transitions become landings");
   for(var q:List.of(lower,upper)){var s=h.getLevel().getBlockState(q);h.assertTrue(s.getValue(FLAT_JOIN)&&s.getValue(SLOPE_JOIN),"Walkway-to-slope handrails use the continuous mitered landing model");}
   h.assertTrue(Math.abs(rider.getZ()-stopped[0])<1e-7,"No power means no flat rider movement");h.getLevel().setBlock(power,Blocks.REDSTONE_BLOCK.defaultBlockState(),Block.UPDATE_ALL);
  });
  h.runAtTickTime(10,()->light(h,positions,true));
  h.runAtTickTime(30,()->{h.assertTrue(stopped[0]-rider.getZ()>.5,"Power at the far end carries a travelator rider");h.getLevel().removeBlock(power,false);});
  h.runAtTickTime(35,()->{light(h,positions,false);stopped[0]=rider.getZ();});
  h.runAtTickTime(42,()->{h.assertTrue(Math.abs(stopped[0]-rider.getZ())<1e-7,"Power removal pauses the travelator rider");for(var q:positions)h.assertTrue(be(h,q).paint()==14&&be(h,q).accent()==9,"Switching trim off does not reset its saved dye");h.getLevel().setBlock(first.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),Block.UPDATE_ALL);});
  h.runAtTickTime(47,()->{light(h,positions,true);h.getLevel().removeBlock(positions.get(1),false);});
  h.runAtTickTime(53,()->{light(h,List.of(first),true);light(h,positions.subList(2,positions.size()),false);h.assertTrue(!h.getLevel().getBlockState(lower).getValue(FLAT_JOIN),"Removing the approach restores the rounded terminal rail");h.succeed();});
 }

 @GameTest(template="empty",timeoutTicks=40)
 public static void old_travelator_blocks_pick_and_drop_the_single_family_item(GameTestHelper h){
  var p=h.absolutePos(new BlockPos(2,1,2));var player=TransitTestPlayers.makeMockPlayer(h,GameType.CREATIVE);
  h.assertTrue(EscalatorRegistry.catalog().size()==4,"Only glass, solid and their two powered variants remain in the catalog");
  for(boolean glass:new boolean[]{true,false}){
   var block=EscalatorRegistry.part(Kind.FLAT,glass);var s=block.defaultBlockState();h.getLevel().setBlock(p,s,Block.UPDATE_ALL);be(h,p).setColors(14,9);
   var picked=block.getCloneItemStack(s,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false),h.getLevel(),p,player);
   h.assertTrue(picked.is(EscalatorRegistry.smart(glass,false).asItem())&&TransitBlockItem.paint(picked).getId()==14&&TransitBlockItem.accent(picked).getId()==9,"Legacy travelator pick returns the dyed smart item");
   var drops=Block.getDrops(s,h.getLevel(),p,be(h,p));h.assertTrue(drops.size()==1&&drops.getFirst().is(EscalatorRegistry.smart(glass,false).asItem()),"Legacy travelator loot returns the shared family item");
   h.getLevel().removeBlock(p,false);
  }h.succeed();
 }

 @GameTest(template="escalator_strip",batch="travelator_family",timeoutTicks=65)
 public static void adding_removing_and_splitting_flat_tiles_moves_only_the_exposed_end_returns(GameTestHelper h){
  var p=h.absolutePos(new BlockPos(12,1,12));var a=p.south();var b=p.north();var block=EscalatorRegistry.smart(true,true);
  put(h,p,block,Direction.NORTH);var reference=be(h,p);reference.setColors(14,9);
  h.runAtTickTime(5,()->{var s=h.getLevel().getBlockState(p);h.assertTrue(!s.getValue(FLAT_JOIN)&&!s.getValue(SLOPE_JOIN),"A lone flat section has both rounded returns");put(h,b,block,Direction.NORTH);});
  h.runAtTickTime(10,()->{var start=h.getLevel().getBlockState(p);var end=h.getLevel().getBlockState(b);h.assertTrue(!start.getValue(FLAT_JOIN)&&start.getValue(SLOPE_JOIN),"Adding forward tile keeps only the entrance return");h.assertTrue(end.getValue(FLAT_JOIN)&&!end.getValue(SLOPE_JOIN),"The new last tile has only the exit return");put(h,a,block,Direction.NORTH);});
  h.runAtTickTime(15,()->{var s=h.getLevel().getBlockState(p);h.assertTrue(s.getValue(FLAT_JOIN)&&s.getValue(SLOPE_JOIN),"The former end is now a clean straight middle tile");h.assertTrue(be(h,p)==reference&&reference.paint()==14&&reference.accent()==9,"End changes retain the exact BE and saved dye");h.getLevel().removeBlock(p,false);});
  h.runAtTickTime(20,()->{for(var q:List.of(a,b)){var s=h.getLevel().getBlockState(q);h.assertTrue(!s.getValue(FLAT_JOIN)&&!s.getValue(SLOPE_JOIN),"A gap restores both returns to each separate single tile");}put(h,p,block,Direction.NORTH);});
  h.runAtTickTime(25,()->{var s=h.getLevel().getBlockState(p);h.assertTrue(s.getValue(FLAT_JOIN)&&s.getValue(SLOPE_JOIN),"Filling the gap removes the two internal end pieces again");h.assertTrue(EscalatorNetwork.group(h.getLevel(),p).size()==3,"The rebuilt flat row reconnects as one control network");h.succeed();});
 }
}
