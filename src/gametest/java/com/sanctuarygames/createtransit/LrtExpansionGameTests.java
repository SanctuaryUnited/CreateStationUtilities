package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.lrt.*;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.entity.MoverType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class LrtExpansionGameTests {
 private static void check(boolean ok,String text){if(!ok)throw new GameTestAssertException(text);}
 @GameTest(template="empty",batch="lrt-expansion",timeoutTicks=40)
 public static void connecting_ceiling_rings_follow_the_exposed_perimeter_and_repair(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,4,4));var b=LrtRegistry.CEILING_RING.get();
  for(var f:Direction.Plane.HORIZONTAL){
   var r=f.getClockWise();var back=f.getOpposite();var s=b.defaultBlockState().setValue(LrtBlock.FACING,f);
   for(var q:new BlockPos[]{p,p.relative(r),p.relative(back),p.relative(r).relative(back)})l.setBlock(q,s,3);
   var v=l.getBlockState(p);check(!v.getValue(LrtBlock.LEFT)&&v.getValue(LrtBlock.RIGHT)&&!v.getValue(LrtBlock.TOP)&&v.getValue(LrtBlock.BOTTOM),"2x2 ring removes internal edges in "+f);
   check(l.getBlockState(p).getValue(LrtBlock.SHORT),"Filled diagonal suppresses the inner corner lamp");
   l.setBlock(p.relative(r).relative(back),Blocks.AIR.defaultBlockState(),3);check(!l.getBlockState(p).getValue(LrtBlock.SHORT),"L-shaped ring restores a continuous concave corner");
   check(l.getBlockState(p.relative(r)).getValue(LrtBlock.LEFT),"Ring connection is reciprocal");
   l.setBlock(p.relative(r),Blocks.AIR.defaultBlockState(),3);check(!l.getBlockState(p).getValue(LrtBlock.RIGHT),"Removing a tile restores the exposed ring edge");
   for(var q:new BlockPos[]{p,p.relative(r),p.relative(back),p.relative(r).relative(back)})l.setBlock(q,Blocks.AIR.defaultBlockState(),3);
  }h.succeed();
 }
 @GameTest(template="empty",batch="lrt-expansion",timeoutTicks=40)
 public static void new_window_frames_join_correct_planes_but_narrow_windows_keep_their_frame(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,4,4));var corner=LrtRegistry.WINDOW_ROUNDED_CORNER.get();
  for(var f:Direction.Plane.HORIZONTAL){
   var s=corner.defaultBlockState().setValue(LrtBlock.FACING,f);l.setBlock(p,s,3);
   l.setBlock(p.relative(f.getClockWise()),LrtRegistry.WINDOW_PANORAMA.get().defaultBlockState().setValue(LrtBlock.FACING,f),3);
   l.setBlock(p.relative(f.getOpposite()),LrtRegistry.WINDOW_TINTED.get().defaultBlockState().setValue(LrtBlock.FACING,f.getCounterClockWise()),3);
   check(l.getBlockState(p).getValue(LrtBlock.LEFT)&&l.getBlockState(p).getValue(LrtBlock.RIGHT),"Rounded corner meets both compatible window planes for "+f);
   l.setBlock(p.relative(f.getClockWise()),LrtRegistry.WINDOW_NARROW.get().defaultBlockState().setValue(LrtBlock.FACING,f),3);
   check(!l.getBlockState(p).getValue(LrtBlock.RIGHT),"Narrow window keeps its solid surround");
   for(var q:new BlockPos[]{p,p.relative(f.getClockWise()),p.relative(f.getOpposite())})l.setBlock(q,Blocks.AIR.defaultBlockState(),3);
  }h.succeed();
 }
 @GameTest(template="empty",batch="lrt-expansion",timeoutTicks=40)
 public static void bogey_covers_keep_a_full_floor_and_leave_the_native_bogey_in_place(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,4,4));var b=LrtRegistry.BOGEY_SKIRT.get();
  l.setBlock(p,AllBlocks.SMALL_BOGEY.getDefaultState(),3);
  for(int z=-1;z<=1;z++)for(int x:new int[]{-2,2}){
   var q=p.offset(x,0,z);var s=b.defaultBlockState().setValue(LrtBlock.FACING,x<0?Direction.WEST:Direction.EAST);l.setBlock(q,s,3);
   var shape=s.getCollisionShape(l,q);check(shape.bounds().minY<0,"Bogey cover reaches below the floor");
   check(shape.bounds().maxY==1,"Cover preserves a full-height floor tile");
  }
  var walker=LrtCollisionGameTests.probe(l,Vec3.atLowerCornerOf(p).add(-1.5,1.01,-.8));
  for(int i=0;i<8;i++){walker.move(MoverType.SELF,new Vec3(0,-.08,.25));check(walker.onGround()&&Math.abs(walker.getY()-(p.getY()+1))<.001,"The skirt floor remains walkable across the bogey area");}
  check(l.getBlockState(p).is(AllBlocks.SMALL_BOGEY.get()),"Skirts never replace the bogey block");h.succeed();
 }
}
