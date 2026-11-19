package com.sanctuarygames.createtransit.block;

import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.*;

public final class StationBlockEntity extends BlockEntity implements CabinColors {
    public static final int TRAVEL=10, CLEAR_DELAY=20, DISPENSE=30;
    private int paint=DyeColor.WHITE.getId(),accent=DyeColor.WHITE.getId();
    private int travel,previousTravel,hold,dispense,lastChoice;
    private boolean legacyLayout;
    private final Set<UUID> admissions=new HashSet<>();
    public StationBlockEntity(BlockPos p,BlockState s){super(ModEntities.STATION.get(),p,s);}
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide&&legacyLayout)level.scheduleTick(worldPosition,getBlockState().getBlock(),1);}
    public boolean legacyLayout(){return legacyLayout;}
    public record LayoutData(int paint,int accent,int travel,int hold){}
    public LayoutData layoutData(){return new LayoutData(paint,accent,travel,hold);}
    public void restoreLayout(LayoutData d,boolean motion){paint=d.paint();accent=d.accent();travel=motion?d.travel():0;previousTravel=travel;hold=motion?d.hold():0;legacyLayout=false;changed();}
    public boolean master(){return getBlockState().getValue(StationBlock.COLUMN)==0&&getBlockState().getValue(StationBlock.ROW)==0;}
    public int paint(){return paint;}public int accent(){return accent;}
    public int dispensing(){return dispense;}public int lastChoice(){return lastChoice;}
    public float progress(float partial){return DoorTiming.position((previousTravel+(travel-previousTravel)*partial)/TRAVEL);}
    public float ticketProgress(float partial){return dispense==0?0:1-(dispense-partial)/DISPENSE;}
    public boolean admitted(UUID id){return admissions.contains(id);}
    public void setColors(int p,int a){paint=Math.clamp(p,0,15);accent=Math.clamp(a,0,15);changed();}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    /** Local coordinates in pixels: x across the gate, z from green entrance towards red exit. */
    public Vec3 local(Vec3 point){
        var f=getBlockState().getValue(StationBlock.FACING);double x=(point.x-worldPosition.getX())*16,z=(point.z-worldPosition.getZ())*16;
        var local=switch(f){case EAST->new Vec3(z,(point.y-worldPosition.getY())*16,16-x);case SOUTH->new Vec3(16-x,(point.y-worldPosition.getY())*16,16-z);case WEST->new Vec3(16-z,(point.y-worldPosition.getY())*16,x);default->new Vec3(x,(point.y-worldPosition.getY())*16,z);};
        return getBlockState().getValue(StationBlock.MIRRORED)?new Vec3(16-local.x,local.y,local.z):local;
    }
    private AABB scanBounds(){
        var right=StationBlock.across(getBlockState());var a=worldPosition;var b=a.relative(right,1);
        return new AABB(Math.min(a.getX(),b.getX()),a.getY(),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX())+1,a.getY()+2.5,Math.max(a.getZ(),b.getZ())+1).inflate(1.4,0,1.4);
    }
    /** Admission is transient: tickets and passes never control this RP gate. */
    public void sense(Collection<? extends Player> players){
        if(level==null||level.isClientSide||!master()||legacyLayout||!((StationBlock)getBlockState().getBlock()).isGate())return;
        var before=new HashSet<>(admissions);Set<UUID> present=new HashSet<>();boolean approach=false,occupied=false;
        for(var player:players){
            if(player.isSpectator()||!player.isAlive())continue;
            var p=local(player.position());double radius=player.getBbWidth()*8;
            if(p.y>32||p.y+player.getBbHeight()*16<0||p.x+radius<=16||p.x-radius>=32)continue;
            if(p.z>=-20&&p.z<=24){present.add(player.getUUID());}
            if(p.z>=-20&&p.z<=7.4){approach=true;admissions.add(player.getUUID());}
            if(p.z+radius>=0&&p.z-radius<=24)occupied=true;
            // Once the whole player has passed the gate plane, reverse crossing is blocked.
            if(p.z-radius>8.6)admissions.remove(player.getUUID());
        }
        admissions.retainAll(present);
        if(approach){
            if(travel==0&&hold==0)level.playSound(null,worldPosition,ModSounds.FARE_CHIME.get(),SoundSource.BLOCKS,.45f,1f);
            hold=CLEAR_DELAY;
        }else if(occupied&&travel>0)hold=CLEAR_DELAY;
        else if(hold>0)hold--;
        if(!before.equals(admissions))changed();
    }
    public boolean issue(Player player,int choice){
        if(level==null||level.isClientSide||!master()||!((StationBlock)getBlockState().getBlock()).isKiosk()||dispense>0||choice<0||choice>1||player.isSpectator())return false;
        var item=new ItemStack(choice==0?ModItems.RP_TICKET.get():ModItems.RP_PASS.get());
        if(!player.getInventory().add(item))player.drop(item,false);
        lastChoice=choice+1;dispense=DISPENSE;changed();
        level.playSound(null,worldPosition,ModSounds.FARE_CHIME.get(),SoundSource.BLOCKS,.3f,1f);
        return true;
    }
    private void setOpen(boolean open){
        var s=getBlockState();var b=(StationBlock)s.getBlock();if(s.getValue(StationBlock.OPEN)==open)return;
        for(int c=0;c<b.width();c++)for(int r=0;r<2;r++){
            var p=b.part(worldPosition,s,c,r);if(b.belongs(level,p,worldPosition))level.setBlock(p,level.getBlockState(p).setValue(StationBlock.OPEN,open),Block.UPDATE_CLIENTS);
        }
    }
    public static void tick(Level l,BlockPos p,BlockState s,StationBlockEntity be){
        if(!be.master()||be.legacyLayout)return;be.previousTravel=be.travel;
        if(((StationBlock)s.getBlock()).isGate()){
            if(!l.isClientSide)be.sense(l.getEntitiesOfClass(Player.class,be.scanBounds(),player->!player.isSpectator()));
            int old=be.travel;be.travel=Math.clamp(be.travel+(be.hold>0?1:-1),0,TRAVEL);
            if(!l.isClientSide&&old!=be.travel){be.setOpen(be.travel==TRAVEL);be.changed();}
            else if(!l.isClientSide&&be.hold>0&&l.getGameTime()%5==0)be.changed();
            if(l.isClientSide&&be.hold>0)be.hold--;
        }else if(be.dispense>0){be.dispense--;if(!l.isClientSide){be.setChanged();if(be.dispense%5==0)be.changed();}}
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){
        super.saveAdditional(t,r);t.putInt("stationLayout",legacyLayout?1:2);t.putInt("paint",paint);t.putInt("accent",accent);t.putInt("travel",travel);t.putInt("hold",hold);t.putInt("dispense",dispense);t.putInt("choice",lastChoice);
    }
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){
        super.loadAdditional(t,r);paint=t.contains("paint")?Math.clamp(t.getInt("paint"),0,15):DyeColor.GRAY.getId();accent=t.contains("accent")?Math.clamp(t.getInt("accent"),0,15):DyeColor.BLUE.getId();
        legacyLayout=((StationBlock)getBlockState().getBlock()).isGate()&&t.getInt("stationLayout")<2;
        travel=Math.clamp(t.getInt("travel"),0,TRAVEL);previousTravel=travel;hold=Math.clamp(t.getInt("hold"),0,CLEAR_DELAY);dispense=Math.clamp(t.getInt("dispense"),0,DISPENSE);lastChoice=Math.clamp(t.getInt("choice"),0,2);
        admissions.clear();if(t.contains("admissions",Tag.TAG_LIST))for(var entry:t.getList("admissions",Tag.TAG_COMPOUND)){var a=(CompoundTag)entry;if(a.hasUUID("id"))admissions.add(a.getUUID("id"));}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){
        var tag=saveWithoutMetadata(r);var list=new ListTag();for(var id:admissions){var entry=new CompoundTag();entry.putUUID("id",id);list.add(entry);}tag.put("admissions",list);return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection c,ClientboundBlockEntityDataPacket p,HolderLookup.Provider r){
        super.onDataPacket(c,p,r);if(level!=null&&level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
    }
}
