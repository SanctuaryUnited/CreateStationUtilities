package com.sanctuarygames.createtransit.radio;
import net.minecraft.core.*;import net.minecraft.nbt.*;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.entity.player.Player;import net.minecraft.world.level.block.entity.BlockEntity;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.SimpleMenuProvider;import net.minecraft.network.chat.Component;
public final class StationSpeakerEntity extends BlockEntity implements com.sanctuarygames.createtransit.block.CabinColors {
 public String group="";public int primary=0,secondary=15;
 @Override public int paint(){return primary;}
 @Override public int accent(){return secondary;}
 @Override public void setColors(int paint,int accent){primary=Math.clamp(paint,0,15);secondary=Math.clamp(accent,0,15);setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
 public RadioSettings effective(){return level instanceof net.minecraft.server.level.ServerLevel l?RadioStations.get(l).effective(group,radio):radio;}
 public final RadioSettings radio=new RadioSettings();
 public StationSpeakerEntity(BlockPos p,BlockState s){super(RadioRegistry.SPEAKER_ENTITY.get(),p,s);}
 @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)RadioNetwork.add(this);}
 @Override public void setRemoved(){if(level!=null&&!level.isClientSide)RadioNetwork.remove(this);super.setRemoved();}
 public CompoundTag snapshot(){var t=new CompoundTag();t.put("Radio",radio.save());t.putString("Group",group);t.putInt("Primary",primary);t.putInt("Secondary",secondary);if(level instanceof net.minecraft.server.level.ServerLevel l){var data=RadioStations.get(l);t.put("RadioStations",data.snapshot());if(!group.isBlank())t.put("GroupRadio",data.group(group,radio).save());}return t;}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider p){return saveWithoutMetadata(p);}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 public void open(Player p){if(p instanceof ServerPlayer player){var t=snapshot();player.openMenu(new SimpleMenuProvider((id,inv,who)->new SpeakerMenu(id,inv,this),Component.literal("Station Speaker")),buf->{buf.writeBlockPos(worldPosition);buf.writeNbt(t);});}}
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider p){super.saveAdditional(t,p);t.putString("Group",group);t.putInt("Primary",primary);t.putInt("Secondary",secondary);t.put("Radio",radio.save());}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider p){super.loadAdditional(t,p);group=t.getString("Group");if(group.length()>32||group.chars().anyMatch(Character::isISOControl))group="";primary=Math.clamp(t.getInt("Primary"),0,15);secondary=t.contains("Secondary")?Math.clamp(t.getInt("Secondary"),0,15):15;radio.load(t.getCompound("Radio"));}
}
