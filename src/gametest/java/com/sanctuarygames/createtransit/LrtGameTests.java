package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.lrt.*;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.ModCreativeTabs;
import com.simibubi.create.*;
import com.simibubi.create.content.trains.bogey.*;
import com.simibubi.create.content.trains.track.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class LrtGameTests {
 static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
 @GameTest(template="empty",timeoutTicks=40)
 public static void lrt_single_car_has_white_dyeable_parts_and_uses_existing_bogeys(GameTestHelper h){
  var l=h.getLevel();var tab=ModCreativeTabs.VEHICLES.get();tab.buildContents(new CreativeModeTab.ItemDisplayParameters(net.minecraft.world.flag.FeatureFlags.VANILLA_SET,true,l.registryAccess()));
  var entries=LrtRegistry.parts().stream().map(b->new ItemStack(b.get())).toList();
  check(tab.getDisplayItems().stream().noneMatch(s->entries.stream().anyMatch(old->old.is(s.getItem()))),"Legacy LRT IDs remain loadable but are retired from the creative catalog");
  check(entries.size()==46,"46 LRT modules including lights, windows and skirts");var unique=new HashSet<Item>();var p=h.absolutePos(new BlockPos(4,2,4));int parts=0;
  for(var stack:entries){
   check(unique.add(stack.getItem()),"One entry per LRT part");if(!(stack.getItem() instanceof TransitBlockItem item))continue;parts++;
   check(!stack.has(DataComponents.CUSTOM_DATA)&&TransitBlockItem.paint(stack)==DyeColor.WHITE&&TransitBlockItem.accent(stack)==DyeColor.WHITE,"Creative base parts are white");
   var b=item.getBlock();var s=b instanceof LrtDoorBlock d?d.placementState():b.defaultBlockState();l.setBlock(p,s,3);b.setPlacedBy(l,p,s,null,TransitBlockItem.finished(b,2,5));
   check(l.getBlockEntity(p)instanceof CabinColors c&&c.paint()==2&&c.accent()==5,"All LRT modules accept separate body/accent dye");
   var pick=b.getCloneItemStack(l,p,l.getBlockState(p));check(TransitBlockItem.paint(pick).getId()==2&&TransitBlockItem.accent(pick).getId()==5,"Pick block retains both dyes");l.destroyBlock(p,false);
  }
  check(parts==46,"Every body module is dyeable");check(AllBogeyStyles.BOGEY_STYLES.keySet().stream().noneMatch(id->id.getNamespace().equals("createtransit")&&id.getPath().startsWith("lrt")),"No LRT custom bogey style remains");
  check(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(LrtRegistry.id("lrt_bogey")).isEmpty(),"Retired LRT bogey block is removed");
  check(AllBogeyStyles.BOGEY_STYLES.get(AllBogeyStyles.STANDARD.id)==AllBogeyStyles.STANDARD,"Create standard style remains available");h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=40)
 public static void lrt_compact_two_wide_two_high_doors_link_and_keep_dye(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));var b=LrtRegistry.DOOR.get();var s=b.placementState().setValue(DoorBlock.FACING,Direction.NORTH);l.setBlock(p,s,3);b.setPlacedBy(l,p,s,null,TransitBlockItem.finished(b,3,4));
  for(int col=0;col<2;col++)for(int row=0;row<2;row++){var q=p.east(col).above(row);var v=l.getBlockState(q);check(v.is(b)&&v.getValue(LrtDoorBlock.COLUMN)==col&&v.getValue(LrtDoorBlock.ROW)==row,"Door reserves exactly two by two cells");check(CabinColors.get(l,q,v)!=null&&CabinColors.get(l,q,v).accent()==4,"Every cell resolves dyed controller");}
  check(!l.getBlockState(p.east(2)).is(b)&&!l.getBlockState(p.above(2)).is(b),"Door does not reserve a third column or fourth row");
  l.setBlock(p.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  h.runAfterDelay(3,()->{for(int col=0;col<2;col++)for(int row=0;row<2;row++)check(l.getBlockState(p.east(col).above(row)).getValue(DoorBlock.OPEN),"Redstone opens all four compact cells together");
   var be=(LrtDoorBlockEntity)l.getBlockEntity(p);for(int i=0;i<40;i++)be.advance(true);check(be.centerSignal(1)==WideDoorTiming.GREEN,"Open center lamps are green");for(int i=0;i<45;i++)be.advance(false);
   l.setBlock(p.below(),Blocks.AIR.defaultBlockState(),3);h.runAfterDelay(3,()->{for(int i=0;i<45;i++)be.advance(false);check(be.centerSignal(1)==0,"Closed center lamps are off");l.destroyBlock(p.east().above(),false);check(l.getBlockState(p).isAir()&&l.getBlockState(p.east().above()).isAir(),"Removing any cell removes the complete doorway");h.succeed();});});
 }
 @GameTest(template="empty",timeoutTicks=30)
 public static void lrt_windows_and_angled_windshield_connect_across_five_blocks(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(3,2,3));
  for(var b:List.of(LrtRegistry.WINDOW.get(),LrtRegistry.CAB_WINDSHIELD_LOWER.get(),LrtRegistry.CAB_WINDSHIELD_UPPER.get())){
   for(int x=0;x<5;x++)l.setBlock(p.east(x),b.defaultBlockState(),3);
   var s=b.connected(l.getBlockState(p.east(2)),l,p.east(2));check(s.getValue(LrtBlock.LEFT)&&s.getValue(LrtBlock.RIGHT),"Five-wide glazing removes internal side borders");
   for(int x=0;x<5;x++)l.setBlock(p.east(x),Blocks.AIR.defaultBlockState(),3);
  }
  var floor=LrtRegistry.FLOOR.get().defaultBlockState().getShape(l,p).bounds();var roof=LrtRegistry.ROOF.get().defaultBlockState().getShape(l,p.above(4)).bounds();check(floor.maxY==1&&roof.minY==0,"Floor top and row-four roof underside leave three clear blocks");check((p.above(4).getY()+roof.minY)-(p.getY()+floor.maxY)==3,"Interior height is exactly three clear blocks");check(floor.minX==0&&floor.maxX==1&&floor.minZ==0&&floor.maxZ==1,"Five placed floor cells occupy the full five-block width");h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=40)
 public static void lrt_window_connections_update_in_both_axes_for_every_facing(GameTestHelper h){
  var l=h.getLevel();var center=h.absolutePos(new BlockPos(5,2,4));var b=LrtRegistry.WINDOW.get();
  for(var f:Direction.Plane.HORIZONTAL){var p=center.relative(f,3);for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)l.setBlock(p.relative(f.getClockWise(),x).above(y),b.defaultBlockState().setValue(LrtBlock.FACING,f),3);}
  h.runAfterDelay(3,()->{
   for(var f:Direction.Plane.HORIZONTAL){var p=center.relative(f,3).above();var s=l.getBlockState(p);check(s.getValue(LrtBlock.LEFT)&&s.getValue(LrtBlock.RIGHT)&&s.getValue(LrtBlock.TOP)&&s.getValue(LrtBlock.BOTTOM),"Actual placed window grid removes every shared border for "+f);
    var q=p.relative(f.getClockWise());l.setBlock(q,l.getBlockState(q).setValue(LrtBlock.FACING,f.getOpposite()),3);check(!l.getBlockState(p).getValue(LrtBlock.RIGHT),"A different glass plane restores the exposed right frame");l.setBlock(q,l.getBlockState(q).setValue(LrtBlock.FACING,f),3);
   }
   h.runAfterDelay(2,()->{for(var f:Direction.Plane.HORIZONTAL)check(l.getBlockState(center.relative(f,3).above()).getValue(LrtBlock.RIGHT),"Matching plane reconnects after neighbor rotation");h.succeed();});
  });
 }
 @GameTest(template="empty",timeoutTicks=40)
 public static void lrt_glass_corners_join_both_flat_legs_and_stack_for_every_facing(GameTestHelper h){
  var l=h.getLevel();var center=h.absolutePos(new BlockPos(5,2,4));var b=LrtRegistry.WINDOW.get();var corner=LrtRegistry.WINDOW_CORNER.get();
  for(var f:Direction.Plane.HORIZONTAL){var p=center.relative(f,2);for(int y=0;y<2;y++)l.setBlock(p.above(y),corner.defaultBlockState().setValue(LrtBlock.FACING,f),3);
   l.setBlock(p.relative(f.getClockWise()),b.defaultBlockState().setValue(LrtBlock.FACING,f),3);l.setBlock(p.relative(f.getOpposite()),b.defaultBlockState().setValue(LrtBlock.FACING,f.getCounterClockWise()),3);
  }
  h.runAfterDelay(3,()->{
   for(var f:Direction.Plane.HORIZONTAL){var p=center.relative(f,2);var s=l.getBlockState(p);check(s.getValue(LrtBlock.LEFT)&&s.getValue(LrtBlock.RIGHT)&&s.getValue(LrtBlock.TOP),"Both corner planes join the correct flat windows and stacked corner for "+f);
    check(l.getBlockState(p.above()).getValue(LrtBlock.BOTTOM),"Upper corner joins the lower glass");check(l.getBlockState(p.relative(f.getClockWise())).getValue(LrtBlock.LEFT),"North leg connection is reciprocal");check(l.getBlockState(p.relative(f.getOpposite())).getValue(LrtBlock.RIGHT),"West leg connection is reciprocal");
    l.setBlock(p.relative(f.getClockWise()),Blocks.AIR.defaultBlockState(),3);check(!l.getBlockState(p).getValue(LrtBlock.RIGHT)&&l.getBlockState(p).getValue(LrtBlock.LEFT),"Removing one leg restores just that corner end frame");
    l.setBlock(p.relative(f.getClockWise()),b.defaultBlockState().setValue(LrtBlock.FACING,f.getOpposite()),3);check(!l.getBlockState(p).getValue(LrtBlock.RIGHT),"Wrong-facing glass does not connect to a corner");
   }h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=40)
 public static void lrt_saved_window_flags_repair_on_load_without_losing_dyes(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));var b=LrtRegistry.WINDOW.get();var s=b.defaultBlockState();l.setBlock(p,s,3);l.setBlock(p.east(),s,3);
  ((LrtBlockEntity)l.getBlockEntity(p)).setColors(5,3);l.setBlock(p,s,Block.UPDATE_CLIENTS);l.setBlock(p.east(),s,Block.UPDATE_CLIENTS);
  ((LrtBlockEntity)l.getBlockEntity(p)).onLoad();((LrtBlockEntity)l.getBlockEntity(p.east())).onLoad();
  h.runAfterDelay(3,()->{check(l.getBlockState(p).getValue(LrtBlock.RIGHT)&&l.getBlockState(p.east()).getValue(LrtBlock.LEFT),"Stale stored connection flags repair after chunk load");var colors=(LrtBlockEntity)l.getBlockEntity(p);check(colors.paint()==5&&colors.accent()==3,"Connection repair keeps both saved dyes");h.succeed();});
 }
 @GameTest(template="empty",timeoutTicks=40)
 public static void lrt_pantograph_folds_with_redstone_and_roof_mount_is_flush(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,4,4));var b=LrtRegistry.PANTOGRAPH.get();l.setBlock(p,b.defaultBlockState(),3);var raised=b.defaultBlockState().getShape(l,p).bounds();var folded=b.defaultBlockState().setValue(LrtBlock.RAISED,false).getShape(l,p).bounds();
  check(Math.abs(raised.minY+13.0/16)<.001&&folded.maxY<raised.maxY,"Mount sits on the three-pixel roof and folds below raised height");l.setBlock(p.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  h.runAfterDelay(2,()->{check(l.getBlockState(p).getValue(LrtBlock.RAISED),"Power raises pantograph");l.setBlock(p.east(),Blocks.AIR.defaultBlockState(),3);h.runAfterDelay(2,()->{check(!l.getBlockState(p).getValue(LrtBlock.RAISED),"Removing power folds pantograph");h.succeed();});});
 }
 @GameTest(template="empty",timeoutTicks=40)
 public static void lrt_cab_profiles_round_the_end_columns_and_join_side_glazing(GameTestHelper h){
  var l=h.getLevel();var center=h.absolutePos(new BlockPos(5,2,5));
  for(var f:Direction.Plane.HORIZONTAL){var p=center.relative(f,3);var across=f.getClockWise();
   for(int x=-2;x<=2;x++){
    for(var pair:List.of(com.mojang.datafixers.util.Pair.of(0,LrtRegistry.CAB_LOWER.get()),com.mojang.datafixers.util.Pair.of(1,LrtRegistry.CAB_WINDSHIELD_LOWER.get()),com.mojang.datafixers.util.Pair.of(2,LrtRegistry.CAB_UPPER.get())))l.setBlock(p.relative(across,x).above(pair.getFirst()),pair.getSecond().defaultBlockState().setValue(LrtBlock.FACING,f),3);
    l.setBlock(p.relative(across,x).below(),LrtRegistry.CAB_FLOOR.get().defaultBlockState().setValue(LrtBlock.FACING,f),3);
    l.setBlock(p.relative(across,x).above(3),LrtRegistry.CAB_ROOF.get().defaultBlockState().setValue(LrtBlock.FACING,f),3);
   }
  }
  h.runAfterDelay(3,()->{for(var f:Direction.Plane.HORIZONTAL){var p=center.relative(f,3);var across=f.getClockWise();
   for(int x=-2;x<=2;x++)for(int y:new int[]{-1,0,1,2,3}){var s=l.getBlockState(p.relative(across,x).above(y));check(s.getValue(LrtBlock.LEFT)==(x>-2)&&s.getValue(LrtBlock.RIGHT)==(x<2),"Floor, nose, glazing and roof share the same faceted end profiles for "+f);}}
   h.succeed();});
 }
 @GameTest(template="empty",timeoutTicks=30)
 public static void lrt_crn_housings_leave_native_board_volumes_empty(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));
  for(boolean side:new boolean[]{false,true}){var b=(side?LrtRegistry.SIDE_DISPLAY_MOUNT:LrtRegistry.DISPLAY_MOUNT).get();var s=b.defaultBlockState().setValue(LrtBlock.LEFT,true).setValue(LrtBlock.RIGHT,true);var housing=s.getShape(l,p).move(0,0,1);
   var board=Block.box(0,0,side?0:8,16,16,side?8:16);check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(housing,board,net.minecraft.world.phys.shapes.BooleanOp.AND),"Cab/side housings do not overlap the native CRN board block");}
  h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=30)
 public static void lrt_legacy_tall_doors_keep_all_six_cells(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));var b=LrtRegistry.DOOR.get();var s=b.placementState().setValue(LrtDoorBlock.TALL,true);l.setBlock(p,s,3);b.setPlacedBy(l,p,s,null,TransitBlockItem.finished(b,2,4));
  for(int x=0;x<2;x++)for(int y=0;y<3;y++)check(l.getBlockState(p.east(x).above(y)).is(b),"Saved tall variant keeps its six-cell geometry");l.destroyBlock(p.east().above(2),false);check(l.getBlockState(p).isAir()&&l.getBlockState(p.east().above()).isAir(),"Legacy tall frame removes completely");h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=30)
 public static void lrt_gangway_joins_floor_collisions_and_has_three_clear_blocks(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,3));var b=LrtRegistry.ARTICULATION_ANCHOR.get();var a=b.defaultBlockState().setValue(LrtBlock.FACING,Direction.SOUTH);var z=b.defaultBlockState().setValue(LrtBlock.FACING,Direction.NORTH);l.setBlock(p,a,3);l.setBlock(p.south(3),z,3);
  var pair=LrtArticulationLink.findPlaced(l,p,a);check(pair!=null,"Opposing end anchors form one gangway");var quads=LrtBellowsGeometry.build(pair);check(!quads.isEmpty(),"Three-high bellows geometry exists");
  for(var q:quads)for(var v:List.of(q.a(),q.b(),q.c(),q.d()))check(v.y>=p.getY()+1&&v.y<=p.getY()+4,"Gangway stays within three clear interior rows");
  var floor=net.minecraft.world.phys.shapes.Shapes.or(a.getCollisionShape(l,p),z.getCollisionShape(l,p.south(3)).move(0,0,3));var passage=net.minecraft.world.phys.shapes.Shapes.box(-.5,.8,1,1.5,.9,3);
  check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(passage,floor,net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST),"Overlapping floor extensions support the center of the two-block gap");
  l.setBlock(p.south(3),a,3);check(LrtArticulationLink.findPlaced(l,p,a)==null,"Same-facing anchors cannot link");h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=30)
 public static void lrt_horn_and_directional_headlight_rules(GameTestHelper h){
  var s=LrtRegistry.HORN.get().defaultBlockState();check(s.getBlock() instanceof LrtBlock b&&b.kind()==LrtBlock.Kind.HORN,"Roof horn registered as dyeable LRT part");
  check(LrtHeadlightMovement.leading(Direction.NORTH,Direction.NORTH,false),"North cab leads forward");
  check(!LrtHeadlightMovement.leading(Direction.SOUTH,Direction.NORTH,false),"South cab shows tail red forward");
  check(LrtHeadlightMovement.leading(Direction.SOUTH,Direction.NORTH,true)&&!LrtHeadlightMovement.leading(Direction.NORTH,Direction.NORTH,true),"Both cabs reverse colors together");
  var p=h.absolutePos(new BlockPos(4,2,4));h.getLevel().setBlock(p,LrtRegistry.ARTICULATION_ANCHOR.get().defaultBlockState().setValue(LrtBlock.FACING,Direction.SOUTH),3);h.getLevel().setBlock(p.south(3),LrtRegistry.ARTICULATION_ANCHOR.get().defaultBlockState().setValue(LrtBlock.FACING,Direction.NORTH),3);
  ((LrtBlockEntity)h.getLevel().getBlockEntity(p)).setColors(DyeColor.BLUE.getId(),DyeColor.YELLOW.getId());var pair=LrtArticulationLink.findPlaced(h.getLevel(),p,h.getLevel().getBlockState(p));
  var quads=LrtBellowsGeometry.build(pair);check(quads.stream().anyMatch(q->q.tint()==0&&q.texture().equals("lrt_bellows_dye")),"Fabric pleats accept body dye");check(quads.stream().anyMatch(q->q.tint()==1),"Ridge trim accepts accent dye");check(quads.stream().map(LrtBellowsGeometry.Quad::shade).distinct().count()>1,"Pleats use distinct shading for fold depth");h.succeed();
 }
}
