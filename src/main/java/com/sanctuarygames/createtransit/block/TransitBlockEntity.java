package com.sanctuarygames.createtransit.block;

import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TransitBlockEntity extends BlockEntity implements CabinColors {
    private int paint=DyeColor.WHITE.getId(), accent=DyeColor.WHITE.getId();
    private int travel, previousTravel, warning;
    private boolean powered, legacyLayout;
    public TransitBlockEntity(BlockPos p,BlockState s) {super(ModEntities.TRANSIT.get(),p,s);}
    @Override public void onLoad() {
        super.onLoad();
        if(level!=null&&!level.isClientSide)level.scheduleTick(worldPosition,getBlockState().getBlock(),1);
    }
    public boolean legacyLayout() {return legacyLayout;}
    public record LayoutData(int paint,int accent,int travel,int warning,boolean powered) {}
    public LayoutData layoutData() {return new LayoutData(paint,accent,travel,warning,powered);}
    public void restoreLayout(LayoutData data) {
        paint=data.paint();accent=data.accent();travel=data.travel();previousTravel=travel;warning=data.warning();powered=data.powered();legacyLayout=false;changed();
    }
    public void finishLayout() {legacyLayout=false;setChanged();}
    public int paint() {return paint;}
    public int accent() {return accent;}
    public boolean master() { return getBlockState().getValue(TransitBarrierBlock.ROW)==0&&getBlockState().getValue(TransitBarrierBlock.COLUMN)==0; }
    public float progress(float partial) {return DoorTiming.position((previousTravel+(travel-previousTravel)*partial)/DoorTiming.TRAVEL);}
    public void setColors(int paint,int accent) {this.paint=paint;this.accent=accent;changed();}
    private void changed() {setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    public void setPower(boolean next) {
        if(level==null||level.isClientSide||!master()||!(getBlockState().getBlock() instanceof TransitBarrierBlock b)||!b.isDoor()||legacyLayout||next==powered)return;
        powered=next;
        if(next) { warning=0; setStatus(travel==DoorTiming.TRAVEL?2:1); com.sanctuarygames.createtransit.WideDoorSounds.play(level,worldPosition,true); }
        else if(travel>0) {
            warning=DoorTiming.WARNING;setStatus(travel==DoorTiming.TRAVEL?3:5);
            com.sanctuarygames.createtransit.WideDoorSounds.play(level,worldPosition,false);
        }
        changed();
    }
    private void setStatus(int status) {
        if(level==null)return;
        BlockState s=getBlockState();var b=(TransitBarrierBlock)s.getBlock();
        if(s.getValue(TransitBarrierBlock.STATUS)==status)return;
        for(int c=0;c<b.width();c++)for(int r=0;r<b.rows();r++) {
            BlockPos p=b.part(worldPosition,s.getValue(TransitBarrierBlock.FACING),c,r);
            if(b.belongs(level,p,worldPosition))level.setBlock(p,level.getBlockState(p).setValue(TransitBarrierBlock.STATUS,status),Block.UPDATE_CLIENTS);
        }
    }
    public static void tick(Level level,BlockPos pos,BlockState s,TransitBlockEntity be) {
        if(!be.master()||!(s.getBlock() instanceof TransitBarrierBlock b)||!b.isDoor()||be.legacyLayout)return;
        be.previousTravel=be.travel;
        if(!level.isClientSide&&level.getGameTime()%10==0)TransitNetwork.refresh(level,pos);
        if(be.warning>0) {
            be.warning--;
            if(!level.isClientSide&&be.warning==0)be.setStatus(4);
        } else {
            int before=be.travel;
            be.travel=DoorTiming.advance(be.travel,be.powered,false);
            if(!level.isClientSide&&be.travel!=before) {
                be.setChanged();
                if(be.travel==DoorTiming.TRAVEL)be.setStatus(2);
                else if(be.travel==0)be.setStatus(0);
                if(be.travel%4==0||before==0||before==DoorTiming.TRAVEL)be.changed();
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);tag.putInt("panel",getBlockState().getValue(TransitBarrierBlock.PANEL));tag.putInt("layoutVersion",legacyLayout?1:2);tag.putInt("paint",paint);tag.putInt("accent",accent);
        tag.putInt("travel",travel);tag.putInt("warning",warning);tag.putBoolean("powered",powered);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        legacyLayout=tag.getInt("layoutVersion")<2;
        if(tag.contains("paint"))paint=Math.clamp(tag.getInt("paint"),0,15);
        if(tag.contains("accent"))accent=Math.clamp(tag.getInt("accent"),0,15);
        travel=Math.clamp(tag.getInt("travel"),0,DoorTiming.TRAVEL);previousTravel=travel;
        warning=Math.clamp(tag.getInt("warning"),0,DoorTiming.WARNING);powered=tag.getBoolean("powered");
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider registries) {
        super.onDataPacket(connection,packet,registries);
        if(level!=null&&level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider p) {return saveWithoutMetadata(p);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
}
