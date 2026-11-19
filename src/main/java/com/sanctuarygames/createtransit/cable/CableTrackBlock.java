package com.sanctuarygames.createtransit.cable;

import com.railwayteam.railways.content.custom_tracks.monorail.MonorailTrackBlock;
import com.simibubi.create.content.trains.track.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.*;

public final class CableTrackBlock extends MonorailTrackBlock {
    public CableTrackBlock(Properties properties,TrackMaterial material){super(properties,material);}
    @Override public BlockState getBogeyAnchor(BlockGetter level,BlockPos pos,BlockState state){return CableRailways.GRIP.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS,state.getValue(SHAPE)==TrackShape.XO?Direction.Axis.X:Direction.Axis.Z);}
    // A cable is not a walkable concrete beam. Its selection area stays easy to target.
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(SHAPE)==TrackShape.XO?box(0,-2,6,16,2,10):state.getValue(SHAPE)==TrackShape.ZO?box(6,-2,0,10,2,16):super.getShape(state,level,pos,context);}
    @Override public VoxelShape getInteractionShape(BlockState state,BlockGetter level,BlockPos pos){return getShape(state,level,pos,CollisionContext.empty());}
}
