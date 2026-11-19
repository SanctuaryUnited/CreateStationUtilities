package com.sanctuarygames.createtransit.block;

import com.sanctuarygames.createtransit.registry.ModEntities;
import com.simibubi.create.content.decoration.slidingDoor.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** One leaf per item. Place adjacent opposite-hinge leaves for a two-wide doorway. */
public final class GondolaDoorBlock extends SlidingDoorBlock {
    public static final BooleanProperty PAIRED=BooleanProperty.create("paired");
    public GondolaDoorBlock(Properties p){super(p,BlockSetType.IRON,false);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(PAIRED);}
    private BlockState paired(BlockState s,BlockGetter l,BlockPos p){var f=s.getValue(FACING);var next=l.getBlockState(p.relative(s.getValue(HINGE)==DoorHingeSide.LEFT?f.getClockWise():f.getCounterClockWise()));return s.setValue(PAIRED,next.is(this)&&next.getValue(FACING)==f&&next.getValue(HINGE)!=s.getValue(HINGE)&&next.getValue(HALF)==s.getValue(HALF));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:paired(s,c.getLevel(),c.getClickedPos());}
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos q){var next=super.updateShape(s,d,neighbor,l,p,q);return next.is(this)?paired(next,l,p):next;}
    @Override public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType(){return ModEntities.GONDOLA_DOOR.get();}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER?null:new GondolaDoorBlockEntity(p,s);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){super.setPlacedBy(l,p,s,e,stack);CabinColors.place(l,p,s,stack);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return CabinColors.dye(stack,s,l,p,player);}
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return CabinColors.pick(l,p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return TransitBarrierBlock.rotated(s.getValue(FACING),0,0,0,16,16,3);}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(OPEN)?Shapes.empty():getShape(s,l,p,c);}
}
