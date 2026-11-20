package com.sanctuarygames.createtransit.elevator.client;
import java.util.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.client.gui.components.*;import com.sanctuarygames.createtransit.radio.client.RadioClient;
/** Shared radio editor for cabin and wall/ceiling speaker menus. */
public final class RadioControls {
 public interface Host {Button button(String label,int x,int y,int w,Button.OnPress action);EditBox field(int x,int y,int w,String value,int max);void send(int op,String key,String value);void slider(String label,int x,int y,int w,double value,java.util.function.DoubleConsumer save);default void adjusted(String label,int x,int y,int w,double v,boolean distance,java.util.function.DoubleConsumer save){double step=distance?1d/252:label.toLowerCase(java.util.Locale.ROOT).contains("volume")?1d/300:.01;button("−",x,y,22,b->save.accept(Math.max(0,v-step)));slider(label,x+26,y,w-52,v,save);button("+",x+w-22,y,22,b->save.accept(Math.min(1,v+step)));}}
 private RadioControls(){}
 public static void build(Host h,CompoundTag state,CompoundTag radio,int y,int width,boolean speaker){
  int w=(width-34)/2,right=w+20;boolean grouped=speaker&&!state.getString("Group").isBlank();var control=grouped?state.getCompound("GroupRadio"):radio;String prefix=grouped?"group.":"";h.button("Radio: "+(control.getBoolean("Enabled")?"On":"Off"),14,y,width-28,b->h.send(13,prefix+"enabled",Boolean.toString(!control.getBoolean("Enabled"))));
  var raw=state.getCompound("RadioStations").getList("Stations",10);var list=new ArrayList<CompoundTag>();for(int i=0;i<raw.size();i++)list.add(raw.getCompound(i));String selected=control.getString("Station");int index=-1;for(int i=0;i<list.size();i++)if(list.get(i).getString("Id").equals(selected))index=i;final int next=(index+1)%Math.max(1,list.size());
  h.button(list.isEmpty()?"No stations — add one below":"Station: "+(index<0?"None":list.get(index).getString("Name")),14,y+26,width-28,b->{if(!list.isEmpty())h.send(13,prefix+"station",list.get(next).getString("Id"));}).active=!list.isEmpty();
  h.adjusted("Volume",14,y+52,width-28,radio.getDouble("Volume")/3,false,v->h.send(13,"volume",String.format(Locale.ROOT,"%.2f",v*3)));
  String[] modes=speaker?new String[]{"NEARBY","POWERED","UNPOWERED"}:new String[]{"NEARBY","INSIDE"};String mode=control.getString("Mode");h.button("Playback: "+mode,14,y+78,width-28,b->h.send(13,prefix+"mode",modes[(Arrays.asList(modes).indexOf(mode)+1)%modes.length]));
  h.adjusted("Direct range",14,y+104,width-28,com.sanctuarygames.createtransit.radio.RadioSettings.radiusToSlider(radio.getInt("Radius")),true,v->h.send(13,"radius",Integer.toString(com.sanctuarygames.createtransit.radio.RadioSettings.radiusFromSlider(v))));
  h.button("Extended reverb: "+radio.getBoolean("Reverb"),14,y+132,width-28,b->h.send(13,"reverb",Boolean.toString(!radio.getBoolean("Reverb"))));
  h.adjusted("Reverb range",14,y+158,width-28,com.sanctuarygames.createtransit.radio.RadioSettings.radiusToSlider(radio.getInt("ReverbRadius")),true,v->h.send(13,"reverbRadius",Integer.toString(com.sanctuarygames.createtransit.radio.RadioSettings.radiusFromSlider(v))));
  h.adjusted("Reverb level",14,y+184,width-28,radio.getDouble("ReverbLevel"),false,v->h.send(13,"reverbLevel",String.format(Locale.ROOT,"%.2f",v)));
  h.button("Extended reverb requires Sound Physics Remastered",14,y+210,width-28,b->{}).active=false;
  h.adjusted("Bass boost",14,y+236,width-28,com.sanctuarygames.createtransit.radio.RadioSettings.readAcoustic(radio,"BassDb",0,0,6)/6,false,v->h.send(13,"bass",Double.toString(v*6)));
  h.adjusted("Distance falloff",14,y+262,width-28,(com.sanctuarygames.createtransit.radio.RadioSettings.readAcoustic(radio,"Falloff",1,.5,2)-.5)/1.5,false,v->h.send(13,"falloff",Double.toString(.5+v*1.5)));
  h.adjusted("Wall muffling",14,y+288,width-28,com.sanctuarygames.createtransit.radio.RadioSettings.readAcoustic(radio,"Muffling",1,0,2)/2,false,v->h.send(13,"muffling",Double.toString(v*2)));
  h.button("Wall muffling requires Sound Physics Remastered",14,y+314,width-28,b->{}).active=false;
  y+=206;
  if(speaker){var group=h.field(14,y+238,width-28,state.getString("Group"),32);group.setHint(net.minecraft.network.chat.Component.literal("Radio group name"));h.button("Join / create group",14,y+264,w,b->h.send(13,"group",group.getValue()));h.button("Leave group",right,y+264,w,b->h.send(13,"group",""));
   var g=state.getCompound("GroupRadio");h.button("Group power: "+g.getBoolean("Enabled"),14,y+290,w,b->h.send(13,"group.enabled",Boolean.toString(!g.getBoolean("Enabled"))));h.button("Use local station",right,y+290,w,b->h.send(13,"group.station",radio.getString("Station")));h.adjusted("Group volume",14,y+316,width-28,g.getDouble("Volume")/3,false,v->h.send(13,"group.volume",String.format(Locale.ROOT,"%.2f",v*3)));h.button("Group mode: "+g.getString("Mode"),14,y+342,width-28,b->h.send(13,"group.mode",modes[(Arrays.asList(modes).indexOf(g.getString("Mode"))+1)%modes.length]));
   for(int row=0;row<2;row++){final String key=row==0?"primary":"secondary";for(int color=0;color<16;color++){final int id=color;var button=h.button("",14+color*(width-28)/16,y+370+row*24,Math.max(7,(width-28)/16-1),b->h.send(13,key,Integer.toString(id)));StationUi.swatch(button,net.minecraft.world.item.DyeColor.byId(id).getTextureDiffuseColor());button.setTooltip(Tooltip.create(net.minecraft.network.chat.Component.literal((row==0?"Casing: ":"Grille: ")+net.minecraft.world.item.DyeColor.byId(id).getName())));}}}
  var name=h.field(14,y+134,width-28,"",32);name.setHint(net.minecraft.network.chat.Component.literal("Custom station name"));
  var url=h.field(14,y+160,width-28,"",256);url.setHint(net.minecraft.network.chat.Component.literal("https://host.example/live.mp3"));
  h.button("Add station",14,y+186,w,b->h.send(12,name.getValue(),url.getValue()));h.button("Remove selected",right,y+186,w,b->h.send(14,"",selected)).active=!selected.isEmpty();
  h.button("MP3 / Ogg • direct live streams",14,y+212,width-28,b->{}).active=false;
 }
}
