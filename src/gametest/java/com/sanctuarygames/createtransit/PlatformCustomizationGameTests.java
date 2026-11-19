package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class PlatformCustomizationGameTests {
 private static void check(boolean b,String s){if(!b)throw new GameTestAssertException(s);}
 private static void place(GameTestHelper h,TransitBarrierBlock b,BlockPos p,Direction f,ItemStack item){var s=b.defaultBlockState().setValue(TransitBarrierBlock.FACING,f);h.getLevel().setBlock(p,s,3);b.setPlacedBy(h.getLevel(),p,s,null,item);}
 @GameTest(template="empty",timeoutTicks=45)
 public static void styles_cycle_all_heights_and_survive_item_roundtrip(GameTestHelper h){
  var l=h.getLevel();var player=FakePlayerFactory.getMinecraft(l);var p=h.absolutePos(new BlockPos(5,1,5));
  for(var b:java.util.List.of(ModBlocks.PLATFORM_SCREEN_DOOR.get(),ModBlocks.COMPACT_SCREEN_DOOR.get(),ModBlocks.PLATFORM_GATE.get(),ModBlocks.PLATFORM_WALL.get(),ModBlocks.COMPACT_WALL.get(),ModBlocks.PLATFORM_BARRIER.get())){
   place(h,b,p,Direction.NORTH,new ItemStack(b));var target=b.part(p,Direction.NORTH,b.width()-1,b.rows()-1);var hit=new BlockHitResult(Vec3.atCenterOf(target),Direction.NORTH,target,false);
   player.setPos(Vec3.atCenterOf(target).add(0,0,-2));player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
   for(int expected:new int[]{1,2,0,1,2}){
    player.gameMode.useItemOn(player,l,ItemStack.EMPTY,InteractionHand.MAIN_HAND,hit);
    for(int c=0;c<b.width();c++)for(int r=0;r<b.rows();r++)check(l.getBlockState(b.part(p,Direction.NORTH,c,r)).getValue(TransitBarrierBlock.PANEL)==expected,"All cells switch style: "+b+" / "+expected);
   }
   player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LIME_DYE));player.gameMode.useItemOn(player,l,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
   check(l.getBlockState(target).getValue(TransitBarrierBlock.PANEL)==2,"Accent dye does not cycle style");
   var copy=b.getCloneItemStack(l,target,l.getBlockState(target));check(TransitBlockItem.panel(copy)==2&&TransitBlockItem.accent(copy)==DyeColor.LIME,"Pick block retains style and accent");
   var saved=l.getBlockEntity(p).saveWithoutMetadata(l.registryAccess());check(saved.getInt("panel")==2,"Panel saved for loot");
   l.destroyBlock(target,true);var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(4)).stream().filter(e->e.getItem().is(b.asItem())).toList();
   check(drops.size()==1&&TransitBlockItem.panel(drops.getFirst().getItem())==2,"One solid-style item drops");drops.forEach(ItemEntity::discard);
   place(h,b,p,Direction.NORTH,copy);check(l.getBlockState(p).getValue(TransitBarrierBlock.PANEL)==2,"Replacement keeps solid panel");l.destroyBlock(p,false);
  }
  player.setShiftKeyDown(false);h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=45)
 public static void perpendicular_wall_returns_have_collision_in_every_rotation(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,1,5));
  for(var b:java.util.List.of(ModBlocks.PLATFORM_WALL.get(),ModBlocks.COMPACT_WALL.get(),ModBlocks.PLATFORM_BARRIER.get()))for(Direction f:Direction.Plane.HORIZONTAL)for(boolean right:new boolean[]{false,true}){
   place(h,b,p,f,new ItemStack(b));var q=p.relative(f.getOpposite());place(h,b,q,right?f.getClockWise():f.getCounterClockWise(),new ItemStack(b));
   for(int row=0;row<b.rows();row++){
    var at=p.above(row);var s=l.getBlockState(at);check(s.getValue(TransitBarrierBlock.CORNER)==(right?2:1),"Automatic return on every row / facing");
    var probe=TransitBarrierBlock.rotated(f,right?14:1,1,8,right?15:2,2,9);
    check(Shapes.joinIsNotEmpty(s.getCollisionShape(l,at),probe,BooleanOp.AND),"Return has real collision");
   }
   l.destroyBlock(q,false);check(l.getBlockState(p).getValue(TransitBarrierBlock.CORNER)==0,"Removing neighbor removes return");l.destroyBlock(p,false);
  }
  h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=90)
 public static void redstone_crosses_wall_corner_to_solid_gate(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(3,1,3));var wall=ModBlocks.COMPACT_WALL.get();var door=ModBlocks.COMPACT_SCREEN_DOOR.get();
  place(h,wall,p,Direction.NORTH,new ItemStack(wall));place(h,wall,p.south(),Direction.EAST,new ItemStack(wall));var d=p.south(2);
  place(h,door,d,Direction.EAST,TransitBlockItem.finished(door,0,0,2));l.setBlock(p.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  h.runAfterDelay(30,()->{check(l.getBlockState(d).getValue(TransitBarrierBlock.STATUS)==2,"Power crosses corner and opens solid door");check(l.getBlockState(d).getValue(TransitBarrierBlock.PANEL)==2,"Motion preserves style");h.succeed();});
 }
}
