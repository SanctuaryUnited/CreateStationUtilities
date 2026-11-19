package com.sanctuarygames.createtransit.lrt;
import com.sanctuarygames.createtransit.block.CabinColors;

import com.sanctuarygames.createtransit.registry.ModEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LrtBlockEntity extends BlockEntity implements CabinColors {
    private int paint=DyeColor.WHITE.getId(),accent=DyeColor.WHITE.getId();
    private final LrtPantographMotion pantograph;
    public LrtBlockEntity(BlockPos p,BlockState s){super(LrtRegistry.PART_ENTITY.get(),p,s);pantograph=new LrtPantographMotion(s.hasProperty(LrtBlock.RAISED)&&s.getValue(LrtBlock.RAISED));}
    public void advancePantograph(boolean raised){pantograph.tick(raised);}
    public float pantographProgress(float partial){return pantograph.progress(partial);}
    @Override public void onLoad(){super.onLoad();if(level!=null&&getBlockState().is(LrtRegistry.ARTICULATION_ANCHOR.get()))LrtGangwayCollision.register(level,worldPosition);if(level!=null&&!level.isClientSide&&getBlockState().getBlock() instanceof LrtBlock b&&b.needsConnections())level.scheduleTick(worldPosition,b,1);}
    @Override public void setRemoved(){if(level!=null&&getBlockState().is(LrtRegistry.ARTICULATION_ANCHOR.get()))LrtGangwayCollision.unregister(level,worldPosition);super.setRemoved();}
    public int paint(){return paint;} public int accent(){return accent;}
    public void setColors(int p,int a){paint=Math.clamp(p,0,15);accent=Math.clamp(a,0,15);setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.putInt("paint",paint);tag.putInt("accent",accent);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);paint=tag.contains("paint")?Math.clamp(tag.getInt("paint"),0,15):DyeColor.WHITE.getId();accent=tag.contains("accent")?Math.clamp(tag.getInt("accent"),0,15):DyeColor.WHITE.getId();}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection c,ClientboundBlockEntityDataPacket p,HolderLookup.Provider r){super.onDataPacket(c,p,r);if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);}
}
