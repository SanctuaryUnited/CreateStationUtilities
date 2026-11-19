package com.sanctuarygames.createtransit.elevator;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Continuous metal walls: lamps appear only at the bottom and top of a column. */
public final class ElevatorSolidBlock extends ElevatorPartBlock {
 public static final BooleanProperty UP=BooleanProperty.create("up"), DOWN=BooleanProperty.create("down");
 public ElevatorSolidBlock(Properties p,Kind kind){super(p,kind);registerDefaultState(defaultBlockState().setValue(UP,false).setValue(DOWN,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(UP,DOWN);}
 private boolean joins(BlockState s,BlockState other){return other.getBlock() instanceof ElevatorSolidBlock && other.getValue(FACING)==s.getValue(FACING);}
 private BlockState connect(BlockState s,LevelAccessor l,BlockPos p){var below=l.getBlockState(p.below());boolean doorBelow=below.getBlock() instanceof ElevatorDoorBlock d && d.cabin() && below.getValue(FACING)==s.getValue(FACING);return s.setValue(UP,joins(s,l.getBlockState(p.above()))).setValue(DOWN,joins(s,below)||doorBelow);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return connect(super.getStateForPlacement(c),c.getLevel(),c.getClickedPos());}
 public static void refresh(Level l,BlockPos p){if(!l.isClientSide)for(var q:new BlockPos[]{p,p.above(),p.below()}){if(!l.hasChunkAt(q))continue;var current=l.getBlockState(q);if(current.getBlock() instanceof ElevatorSolidBlock wall){var connected=wall.connect(current,l,q);if(!connected.equals(current))l.setBlock(q,connected,2);}}}
 @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){super.onPlace(s,l,p,old,moving);refresh(l,p);}
 @Override protected BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos q){return connect(s,l,p);}
}
