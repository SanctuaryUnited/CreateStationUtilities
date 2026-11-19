package com.sanctuarygames.createtransit.elevator;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public final class ElevatorRingBlock extends ElevatorPartBlock {
 public static final BooleanProperty NORTH=BooleanProperty.create("north"),EAST=BooleanProperty.create("east"),SOUTH=BooleanProperty.create("south"),WEST=BooleanProperty.create("west"),NW=BooleanProperty.create("nw"),NE=BooleanProperty.create("ne"),SE=BooleanProperty.create("se"),SW=BooleanProperty.create("sw");
 public ElevatorRingBlock(Properties p){super(p,Kind.CEILING_RING);registerDefaultState(defaultBlockState().setValue(NORTH,false).setValue(EAST,false).setValue(SOUTH,false).setValue(WEST,false).setValue(NW,false).setValue(NE,false).setValue(SE,false).setValue(SW,false));}
 public static BooleanProperty side(Direction d){return switch(d){case NORTH->NORTH;case EAST->EAST;case SOUTH->SOUTH;case WEST->WEST;default->throw new IllegalArgumentException("Horizontal direction required");};}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(NORTH,EAST,SOUTH,WEST,NW,NE,SE,SW);}
 private boolean joins(LevelAccessor l,BlockPos p){return l.getBlockState(p).is(this);}
 public BlockState connected(BlockState s,LevelAccessor l,BlockPos p){return s.setValue(NORTH,joins(l,p.north())).setValue(EAST,joins(l,p.east())).setValue(SOUTH,joins(l,p.south())).setValue(WEST,joins(l,p.west())).setValue(NW,joins(l,p.north().west())).setValue(NE,joins(l,p.north().east())).setValue(SE,joins(l,p.south().east())).setValue(SW,joins(l,p.south().west()));}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return connected(super.getStateForPlacement(c),c.getLevel(),c.getClickedPos());}
 @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q){return connected(s,l,p);}
 @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){super.onPlace(s,l,p,old,moving);refresh(l,p);}
 @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){super.onRemove(s,l,p,next,moving);if(!next.is(this))refresh(l,p);}
 private void refresh(Level l,BlockPos p){if(l.isClientSide)return;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){BlockPos q=p.offset(x,0,z);var state=l.getBlockState(q);if(state.is(this)){var connected=connected(state,l,q);if(state!=connected)l.setBlock(q,connected,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);}}}
 @Override protected BlockState rotate(BlockState s,Rotation r){var out=super.rotate(s,r);for(var d:Direction.Plane.HORIZONTAL)out=out.setValue(side(r.rotate(d)),s.getValue(side(d)));BooleanProperty[] diag={NE,SE,SW,NW};int turns=switch(r){case CLOCKWISE_90->1;case CLOCKWISE_180->2;case COUNTERCLOCKWISE_90->3;default->0;};for(int i=0;i<4;i++)out=out.setValue(diag[(i+turns)%4],s.getValue(diag[i]));return out;}
 @Override protected BlockState mirror(BlockState s,Mirror m){var out=s.setValue(FACING,m.mirror(s.getValue(FACING)));for(var d:Direction.Plane.HORIZONTAL)out=out.setValue(side(m.mirror(d)),s.getValue(side(d)));return m==Mirror.LEFT_RIGHT?out.setValue(NE,s.getValue(SE)).setValue(NW,s.getValue(SW)).setValue(SE,s.getValue(NE)).setValue(SW,s.getValue(NW)):m==Mirror.FRONT_BACK?out.setValue(NE,s.getValue(NW)).setValue(SE,s.getValue(SW)).setValue(NW,s.getValue(NE)).setValue(SW,s.getValue(SE)):out;}
}
