package com.sanctuarygames.createtransit.block;

import com.sanctuarygames.createtransit.registry.ModEntities;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class GondolaDoorBlockEntity extends SlidingDoorBlockEntity implements CabinColors {
    private int paint=DyeColor.WHITE.getId(),accent=DyeColor.WHITE.getId();
    private int travel,previous,closingTicks,openingTicks;
    public GondolaDoorBlockEntity(BlockPos p,BlockState s){super(ModEntities.GONDOLA_DOOR.get(),p,s);travel=previous=isOpen(s)?(s.getBlock() instanceof GondolaWideDoorBlock?WideDoorTiming.RANGE:10):0;}
    public int paint(){return paint;} public int accent(){return accent;}
    public void setColors(int p,int a){paint=Math.clamp(p,0,15);accent=Math.clamp(a,0,15);setChanged();if(level!=null){sendData();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);for(int row=1;row<(getBlockState().getBlock() instanceof GondolaWideDoorBlock?GondolaWideDoorBlock.rows(getBlockState()):2);row++){var q=worldPosition.above(row);level.sendBlockUpdated(q,level.getBlockState(q),level.getBlockState(q),Block.UPDATE_ALL);}}}
    private boolean wide(){return getBlockState().getBlock() instanceof GondolaWideDoorBlock;}
    public void advance(boolean open){previous=travel;if(wide()){travel=WideDoorTiming.advance(travel,open);closingTicks=!open&&previous>0?closingTicks+1:0;openingTicks=open&&previous<WideDoorTiming.RANGE?openingTicks+1:0;}else travel=Math.clamp(travel+(open?1:-1),0,10);}
    public float progress(float partial){float p=(previous+(travel-previous)*Math.clamp(partial,0,1))/(wide()?WideDoorTiming.RANGE:10f);return wide()?WideDoorTiming.position(p):p;}
    public int wideLedColor(float partial){return WideDoorTiming.ledColor(isOpen(getBlockState()),progress(partial),isOpen(getBlockState())?openingTicks:closingTicks,DyeColor.byId(accent).getTextureDiffuseColor());}
    public int centerLedColor(float partial){return WideDoorTiming.centerColor(isOpen(getBlockState()),progress(partial),isOpen(getBlockState())?openingTicks:closingTicks);}
    @Override public void tick(){if(getBlockState().getBlock() instanceof GondolaWideDoorBlock&&getBlockState().getValue(GondolaWideDoorBlock.COLUMN)!=0)return;super.tick();advance(isOpen(getBlockState()));}
    @Override protected void showBlockModel(){if(!wide())super.showBlockModel();}
    @Override protected void write(CompoundTag t,HolderLookup.Provider r,boolean client){super.write(t,r,client);t.putInt("paint",paint);t.putInt("accent",accent);}
    @Override protected void read(CompoundTag t,HolderLookup.Provider r,boolean client){super.read(t,r,client);paint=t.contains("paint")?Math.clamp(t.getInt("paint"),0,15):DyeColor.GRAY.getId();accent=t.contains("accent")?Math.clamp(t.getInt("accent"),0,15):DyeColor.BLUE.getId();if(client&&level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);}
}
