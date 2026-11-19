package com.sanctuarygames.createtransit.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Stationary floor plate: the supporting block row is the cabin floor row. */
public final class ElevatorFloorMarkerBlock extends ElevatorPartBlock {
 public ElevatorFloorMarkerBlock(Properties p){super(p,Kind.MARKER);}
 @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){
  super.setPlacedBy(l,p,s,e,stack);
  if(!l.isClientSide&&l.getBlockEntity(p) instanceof ElevatorPartEntity marker){
   var drive=ElevatorNetwork.nearest(l,p);
   if(drive!=null&&ElevatorLinking.validDrive(l,p,drive.getBlockPos()))marker.bindMarker(drive.getBlockPos());
  }
 }
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult h){
  if(ElevatorLinking.wrench(stack)){
   if(!l.isClientSide)ElevatorLinking.marker(l,p,player);
   return ItemInteractionResult.sidedSuccess(l.isClientSide);
  }
  return super.useItemOn(stack,s,l,p,player,hand,h);
 }
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){
  if(!l.isClientSide)ElevatorLinking.inspectMarker(l,p,player);
  return InteractionResult.sidedSuccess(l.isClientSide);
 }
}
