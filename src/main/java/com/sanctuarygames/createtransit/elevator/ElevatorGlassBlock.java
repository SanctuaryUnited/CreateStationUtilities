package com.sanctuarygames.createtransit.elevator;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
public class ElevatorGlassBlock extends ElevatorPartBlock {
 public static final BooleanProperty LEFT=BooleanProperty.create("left"),RIGHT=BooleanProperty.create("right"),UP=BooleanProperty.create("up"),DOWN=BooleanProperty.create("down");
 public ElevatorGlassBlock(Properties p,Kind k){super(p,k);registerDefaultState(defaultBlockState().setValue(LEFT,false).setValue(RIGHT,false).setValue(UP,false).setValue(DOWN,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(LEFT,RIGHT,UP,DOWN);}
 private static boolean plane(BlockState s,Direction f){return s.getBlock() instanceof ElevatorGlassBlock b&&(s.getValue(FACING)==f||b.kind==Kind.GLASS_CORNER&&s.getValue(FACING).getCounterClockWise()==f);}
 private BlockState connect(BlockState s,LevelAccessor l,BlockPos p){Direction f=s.getValue(FACING);return s.setValue(LEFT,plane(l.getBlockState(p.relative(f.getCounterClockWise())),f)).setValue(RIGHT,plane(l.getBlockState(p.relative(f.getClockWise())),f)).setValue(UP,l.getBlockState(p.above()).is(this)&&l.getBlockState(p.above()).getValue(FACING)==f).setValue(DOWN,l.getBlockState(p.below()).is(this)&&l.getBlockState(p.below()).getValue(FACING)==f);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return connect(super.getStateForPlacement(c),c.getLevel(),c.getClickedPos());}
 @Override protected BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos q){return connect(s,l,p);}
 @Override protected boolean skipRendering(BlockState s,BlockState n,Direction side){if(!(n.getBlock() instanceof ElevatorGlassBlock))return super.skipRendering(s,n,side);Direction f=s.getValue(FACING);if(side.getAxis()==Direction.Axis.Y)return n.is(this)&&n.getValue(FACING)==f;return f.getAxis()!=side.getAxis()&&plane(n,f)||kind==Kind.GLASS_CORNER&&f.getCounterClockWise().getAxis()!=side.getAxis()&&plane(n,f.getCounterClockWise());}
}
