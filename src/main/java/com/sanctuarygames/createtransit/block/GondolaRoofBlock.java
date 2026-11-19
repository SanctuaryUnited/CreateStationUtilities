package com.sanctuarygames.createtransit.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Vanilla stair placement/neighbor rules with the cabin's shallow roof profile. */
public final class GondolaRoofBlock extends StairBlock implements EntityBlock,IWrenchable {
    public static final com.mojang.serialization.MapCodec<GondolaRoofBlock> CODEC=simpleCodec(GondolaRoofBlock::new);
    public GondolaRoofBlock(Properties p){super(Blocks.IRON_BLOCK.defaultBlockState(),p);}
    @Override public com.mojang.serialization.MapCodec<? extends StairBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new GondolaBlockEntity(p,s);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){CabinColors.place(l,p,s,stack);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return CabinColors.dye(stack,s,l,p,player);}
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return CabinColors.pick(l,p,s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return GondolaShapes.roof(s.getValue(FACING),s.getValue(HALF),s.getValue(SHAPE));}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override public BlockState getRotatedBlockState(BlockState s,Direction face){return face.getAxis()==Direction.Axis.Y?s.setValue(FACING,s.getValue(FACING).getClockWise()):s.cycle(HALF);}
    @Override public BlockState updateAfterWrenched(BlockState s,UseOnContext c){return Block.updateFromNeighbourShapes(s,c.getLevel(),c.getClickedPos());}
}
