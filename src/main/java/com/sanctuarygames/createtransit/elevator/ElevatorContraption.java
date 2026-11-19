package com.sanctuarygames.createtransit.elevator;
import java.util.*;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public final class ElevatorContraption extends TranslatingContraption {
 @Override public ContraptionType getType(){return ElevatorRegistry.CONTRAPTION.get();}
 private String failure="Cabin needs a complete rectangular Elevator Floor";
 public String failure(){return failure;}
 private boolean fail(String message){failure=message;return false;}
 @Override public boolean assemble(Level l,BlockPos seed){
  anchor=seed;bounds=new AABB(BlockPos.ZERO);
  if(!l.getBlockState(seed).is(ElevatorRegistry.FLOOR.get()))return fail("The drive must face an Elevator Floor block at the cabin's floor height");
  Set<BlockPos> floor=new HashSet<>();ArrayDeque<BlockPos> floorTodo=new ArrayDeque<>();floorTodo.add(seed);
  int minX=seed.getX(),maxX=minX,minZ=seed.getZ(),maxZ=minZ;
  while(!floorTodo.isEmpty()){
   var p=floorTodo.remove();if(floor.contains(p)||!l.getBlockState(p).is(ElevatorRegistry.FLOOR.get()))continue;
   if(Math.abs(p.getX()-seed.getX())>8||Math.abs(p.getZ()-seed.getZ())>8)return fail("Cabin floor exceeds the 8-block horizontal assembly reach");
   floor.add(p);minX=Math.min(minX,p.getX());maxX=Math.max(maxX,p.getX());minZ=Math.min(minZ,p.getZ());maxZ=Math.max(maxZ,p.getZ());
   for(var d:Direction.Plane.HORIZONTAL)floorTodo.add(p.relative(d));
  }
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!floor.contains(new BlockPos(x,seed.getY(),z)))return fail("Cabin floor has a hole at "+new BlockPos(x,seed.getY(),z).toShortString());
  Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> todo=new ArrayDeque<>();todo.add(seed);
  while(!todo.isEmpty()){
   var p=todo.remove();if(!seen.add(p)||p.getX()<minX||p.getX()>maxX||p.getZ()<minZ||p.getZ()>maxZ||p.getY()<seed.getY())continue;
   var state=l.getBlockState(p);if(state.isAir())continue;
   if(state.getBlock() instanceof ElevatorPartBlock part&&!part.cabin())continue;
   if(p.getY()>seed.getY()+6)return fail("Cabin is too tall: at most 6 block rows above its floor");
   if(!l.hasChunkAt(p))return fail("Cabin chunk is unloaded");
   if(state.getDestroySpeed(l,p)<0||state.getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.BLOCK)return fail("Immovable block in cabin at "+p.toShortString());
   if(l.getBlockEntity(p)!=null&&!(l.getBlockEntity(p) instanceof ElevatorPartEntity))return fail("Unsupported block entity in cabin at "+p.toShortString()+"; use ordinary walls, glass and decorations");
   if(blocks.size()>=512)return fail("Cabin exceeds 512 blocks");
   addBlock(l,p,capture(l,p));for(var d:Direction.values())todo.add(p.relative(d));
  }
  for(var info:blocks.values())if(info.state().getBlock() instanceof ElevatorDoorBlock){
   int row=info.state().getValue(ElevatorDoorBlock.ROW);var other=blocks.get(info.pos().above(row==0?1:-1));
   if(other==null||!other.state().is(info.state().getBlock())||other.state().getValue(ElevatorDoorBlock.ROW)==row||other.state().getValue(ElevatorPartBlock.FACING)!=info.state().getValue(ElevatorPartBlock.FACING))return fail("Cabin door is incomplete or its halves face different directions");
  }
  startMoving(l);return true;
 }
 @Override protected boolean customBlockPlacement(LevelAccessor l,BlockPos p,BlockState s){return s.getBlock() instanceof ElevatorDoorBlock;}
 @Override public void addBlocksToWorld(Level l,StructureTransform transform){if(disassembled)return;super.addBlocksToWorld(l,transform);boolean drop=!AllConfigs.server().kinetics.noDropWhenContraptionReplaceBlocks.get();Set<BlockPos> allowed=new HashSet<>();
  for(var info:blocks.values())if(info.state().getBlock() instanceof ElevatorDoorBlock){var p=transform.apply(info.pos());if(l.getBlockState(p).getDestroySpeed(l,p)==-1){if(drop)Block.dropResources(info.state(),l,p);continue;}l.destroyBlock(p,drop);allowed.add(p);}
  for(var info:blocks.values())if(info.state().getBlock() instanceof ElevatorDoorBlock){var p=transform.apply(info.pos());if(!allowed.contains(p))continue;var s=transform.apply(info.state());l.setBlock(p,s,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);if(l.getBlockEntity(p) instanceof ElevatorPartEntity be&&info.nbt()!=null){var tag=info.nbt().copy();tag.putInt("x",p.getX());tag.putInt("y",p.getY());tag.putInt("z",p.getZ());be.loadWithComponents(tag,l.registryAccess());}}
  for(var p:allowed){var s=l.getBlockState(p);l.sendBlockUpdated(p,s,s,Block.UPDATE_CLIENTS);l.updateNeighborsAt(p,s.getBlock());}
  // Create restores modules in arbitrary order. Resolve wall edges after every module and door is present.
  for(var info:blocks.values())if(info.state().getBlock() instanceof ElevatorSolidBlock)ElevatorSolidBlock.refresh(l,transform.apply(info.pos()));
 }
}
