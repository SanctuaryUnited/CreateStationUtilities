package com.sanctuarygames.createtransit.escalator;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** One inventory item; the placed state follows the actual ramp topology. */
public final class SmartEscalatorBlock extends EscalatorBlock {
 public static final EnumProperty<Kind> SHAPE=EnumProperty.create("shape",Kind.class);
 public SmartEscalatorBlock(Properties p,boolean glass,boolean powered){
  super(p,Kind.FLAT,glass,powered);
  registerDefaultState(defaultBlockState().setValue(SHAPE,Kind.FLAT));
 }
 @Override protected MapCodec<? extends SmartEscalatorBlock> codec(){return simpleCodec(p->new SmartEscalatorBlock(p,true,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(SHAPE);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){
  // Facing is the uphill direction, like ordinary stairs. Looking uphill
  // starts a new run; clicking a section continues that section's orientation.
  var state=defaultBlockState().setValue(FACING,c.getHorizontalDirection()).setValue(REVERSED,c.isSecondaryUseActive());
  var clicked=c.getClickedPos().relative(c.getClickedFace().getOpposite());
  var neighbor=c.getLevel().getBlockState(clicked);
  if(neighbor.getBlock() instanceof EscalatorBlock b&&b.redstonePowered==redstonePowered){
   state=state.setValue(FACING,neighbor.getValue(FACING));
   if(!c.isSecondaryUseActive())state=state.setValue(REVERSED,neighbor.getValue(REVERSED));
  }
  if(!c.isSecondaryUseActive())for(var q:EscalatorNetwork.candidates(c.getClickedPos(),state)){
   var n=c.getLevel().getBlockState(q);
   if(n.getBlock() instanceof EscalatorBlock b&&b.redstonePowered==redstonePowered&&n.getValue(FACING)==state.getValue(FACING)){
    state=state.setValue(REVERSED,n.getValue(REVERSED)).setValue(STOPPED,n.getValue(STOPPED));break;
   }
  }
  return state.setValue(SHAPE,EscalatorNetwork.automaticKind(c.getLevel(),c.getClickedPos(),state));
 }
}
