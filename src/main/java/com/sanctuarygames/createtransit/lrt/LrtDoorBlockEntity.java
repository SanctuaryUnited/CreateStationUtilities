package com.sanctuarygames.createtransit.lrt;
import com.sanctuarygames.createtransit.block.*;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
public final class LrtDoorBlockEntity extends SlidingDoorBlockEntity implements CabinColors {
 public static final float RETRACT_PIXELS=3.2f;
 private int paint,accent,travel,previous,closingTicks,openingTicks;
 public LrtDoorBlockEntity(BlockPos p,BlockState s){super(LrtRegistry.DOOR_ENTITY.get(),p,s);travel=previous=isOpen(s)?WideDoorTiming.RANGE:0;}
 public int paint(){return paint;}public int accent(){return accent;}
 public void setColors(int p,int a){paint=Math.clamp(p,0,15);accent=Math.clamp(a,0,15);setChanged();if(level!=null){sendData();for(int row=0;row<LrtDoorBlock.rows(getBlockState());row++){var q=worldPosition.above(row);level.sendBlockUpdated(q,level.getBlockState(q),level.getBlockState(q),Block.UPDATE_ALL);}}}
 public void advance(boolean open){previous=travel;travel=WideDoorTiming.advance(travel,open);closingTicks=!open&&previous>0?closingTicks+1:0;openingTicks=open&&previous<WideDoorTiming.RANGE?openingTicks+1:0;}
 public float progress(float partial){return WideDoorTiming.position((previous+(travel-previous)*Math.clamp(partial,0,1))/WideDoorTiming.RANGE);}
 public int signal(float partial){return WideDoorTiming.ledColor(isOpen(getBlockState()),progress(partial),isOpen(getBlockState())?openingTicks:closingTicks,DyeColor.byId(accent).getTextureDiffuseColor());}
 public int centerSignal(float partial){return WideDoorTiming.centerColor(isOpen(getBlockState()),progress(partial),isOpen(getBlockState())?openingTicks:closingTicks);}
 @Override public void tick(){if(getBlockState().getValue(LrtDoorBlock.COLUMN)!=0)return;super.tick();advance(isOpen(getBlockState()));}
 @Override protected void showBlockModel(){}
 @Override protected void write(CompoundTag t,HolderLookup.Provider r,boolean client){super.write(t,r,client);t.putInt("paint",paint);t.putInt("accent",accent);}
 @Override protected void read(CompoundTag t,HolderLookup.Provider r,boolean client){super.read(t,r,client);paint=t.getInt("paint");accent=t.getInt("accent");if(client&&level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);}
}
