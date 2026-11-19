package com.sanctuarygames.createtransit.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
/** Mount occupies its own cell ABOVE the display. It never replaces a CRN screen. */
public final class DisplayMountBlock extends TransitBarrierBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final boolean post;
    public DisplayMountBlock(Properties p,boolean post){super(p,false,false);this.post=post;}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new DisplayMountBlock(p,post));}
    @Override public int rows(){return 1;}
    @Override public double pixelsHigh(){return 16;}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return post?TransitBarrierBlock.rotated(s.getValue(FACING),6,0,5,10,16,11):TransitBarrierBlock.rotated(s.getValue(FACING),0,12,5,16,16,11);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
}
