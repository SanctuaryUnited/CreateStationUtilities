package com.sanctuarygames.createtransit.elevator;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The normal and marker blocks use exactly the same rail profile and mounting. */
public class ElevatorGuideRailBlock extends ElevatorPartBlock {
    public static final BooleanProperty UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
    public ElevatorGuideRailBlock(Properties properties) { this(properties, Kind.RAIL); }
    protected ElevatorGuideRailBlock(Properties properties, Kind kind) {
        super(properties, kind);
        registerDefaultState(defaultBlockState().setValue(UP, false).setValue(DOWN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(UP, DOWN);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){if(stack.getItem() instanceof com.sanctuarygames.createtransit.elevator.model.ModelCabinItem item){item.useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,hit));return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);}return super.useItemOn(stack,state,level,pos,player,hand,hit);}
    public static boolean rail(BlockState state) { return state.getBlock() instanceof ElevatorGuideRailBlock; }
    public static boolean compatible(BlockState a, BlockState b) {
        return rail(a) && rail(b) && a.getValue(FACING) == b.getValue(FACING);
    }
    private BlockState connections(BlockState state, LevelAccessor level, BlockPos pos) {
        return state.setValue(UP, compatible(state, level.getBlockState(pos.above())))
            .setValue(DOWN, compatible(state, level.getBlockState(pos.below())) || level.getBlockState(pos.below()).is(ElevatorRegistry.DRIVE.get()));
    }
    public static void refresh(Level level, BlockPos pos) {
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof ElevatorGuideRailBlock rail){var next=rail.connections(state,level,pos);if(next!=state)level.setBlock(pos,next,Block.UPDATE_CLIENTS);}
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);if(context.getClickedFace().getAxis().isHorizontal())state=state.setValue(FACING,context.getClickedFace().getOpposite());
        for (var pos : new BlockPos[]{context.getClickedPos().below(), context.getClickedPos().above()}) {
            var adjacent = context.getLevel().getBlockState(pos);
            if (rail(adjacent)) { state = state.setValue(FACING, adjacent.getValue(FACING)); break; }
        }
        return connections(state, context.getLevel(), context.getClickedPos());
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return turn(box(5,0,0,11,16,1),s.getValue(FACING));}
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connections(state, level, pos);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving); ElevatorNetwork.railsChanged(level, pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving); ElevatorNetwork.railsChanged(level, pos);
    }
    public static ElevatorDriveEntity drive(Level level, BlockPos pos) {
        var original = level.getBlockState(pos);
        for (int n = 1; pos.getY()-n>=level.getMinBuildHeight(); n++) {
            var p = pos.below(n); if (!level.hasChunkAt(p)) return null;
            var state = level.getBlockState(p);
            if (state.is(ElevatorRegistry.DRIVE.get())) return level.getBlockEntity(p) instanceof ElevatorDriveEntity d ? d : null;
            if (!compatible(original, state)) return null;
        }
        return null;
    }
}
