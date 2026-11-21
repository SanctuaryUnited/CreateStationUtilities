package com.sanctuarygames.createtransit.client;
import com.sanctuarygames.createtransit.elevator.client.StationUi;import com.sanctuarygames.createtransit.radio.client.RadioClient;
import net.minecraft.client.gui.*;import net.minecraft.client.gui.screens.Screen;import net.minecraft.client.gui.components.*;import net.minecraft.network.chat.Component;import net.minecraft.world.item.DyeColor;import java.util.*;import java.util.function.*;
public final class StationSettingsScreen extends Screen {
 private final Screen parent;private final Map<AbstractWidget,Integer> widgets=new LinkedHashMap<>();private double target,offset;private long time;private int x,w,bottom;
 public StationSettingsScreen(Screen parent){super(com.sanctuarygames.createtransit.TransitText.uiText("Station Utilities • Client Settings"));this.parent=parent;}
 private Button button(String s,int y,Button.OnPress press){var b=addRenderableWidget(StationUi.button(x,y,w,20,s,press));widgets.put(b,y);bottom=Math.max(bottom,y+20);return b;}
 private void volume(String name,int y,DoubleSupplier get,DoubleConsumer set){button("−",y,b->{set.accept(Math.max(0,get.getAsDouble()-.01));StationClientSettings.get().save();init();}).setWidth(22);var slide=addRenderableWidget(new AbstractSliderButton(x+26,y,w-52,20,com.sanctuarygames.createtransit.TransitText.uiText(""),get.getAsDouble()){
  protected void updateMessage(){setMessage(com.sanctuarygames.createtransit.TransitText.uiText(name+": "+Math.round(value*100)+"%"));}protected void applyValue(){set.accept(value);StationClientSettings.get().save();}
  {updateMessage();}
 });widgets.put(slide,y);var plus=button("+",y,b->{set.accept(Math.min(1,get.getAsDouble()+.01));StationClientSettings.get().save();init();});plus.setX(x+w-22);plus.setWidth(22);}
 @Override protected void init(){clearWidgets();widgets.clear();w=Math.min(390,width-24);x=(width-w)/2;bottom=0;var s=StationClientSettings.get();int y=48;
  button("PBR quality: "+s.pbr,y,b->{s.pbr=StationClientSettings.Pbr.values()[(s.pbr.ordinal()+1)%4];s.save();init();});y+=28;
  volume("Elevator audio",y,()->s.elevatorVolume,v->s.elevatorVolume=v);y+=28;volume("Radio speakers",y,()->s.radioVolume,v->s.radioVolume=v);y+=28;
  button("Streamer Mode: "+s.streamerMode,y,b->{s.streamerMode=!s.streamerMode;s.save();RadioClient.sync();init();});y+=28;
  button("Automatic Radio Sync: "+s.autoRadioSync,y,b->{s.autoRadioSync=!s.autoRadioSync;s.save();init();});y+=28;
  button("Sync nearby radio now",y,b->RadioClient.sync());y+=28;
  var minus=button("−",y,b->{s.uiScale=Math.max(.7,s.uiScale-.05);s.save();minecraft.resizeDisplay();});minus.setWidth(22);
  var scale=addRenderableWidget(new AbstractSliderButton(x+26,y,w-52,20,com.sanctuarygames.createtransit.TransitText.uiText("Menu size: "+Math.round(s.uiScale*100)+"%"),(s.uiScale-.7)/.8){protected void updateMessage(){setMessage(com.sanctuarygames.createtransit.TransitText.uiText("Menu size: "+Math.round((.7+value*.8)*100)+"%"));}protected void applyValue(){}public void onRelease(double xx,double yy){super.onRelease(xx,yy);s.uiScale=.7+value*.8;s.save();minecraft.resizeDisplay();}});widgets.put(scale,y);
  var plus=button("+",y,b->{s.uiScale=Math.min(1.5,s.uiScale+.05);s.save();minecraft.resizeDisplay();});plus.setX(x+w-22);plus.setWidth(22);y+=28;
  button("UI theme: "+DyeColor.byId(s.theme).getName(),y,b->{s.theme=(s.theme+1)%16;s.save();init();});y+=26;
  for(int i=0;i<16;i++){final int dye=i;var strip=addRenderableWidget(StationUi.button(x+i*w/16,y,Math.max(5,w/16-1),18,"",b->{s.theme=dye;s.save();init();}));StationUi.swatch(strip,DyeColor.byId(i).getTextureDiffuseColor());strip.setTooltip(Tooltip.create(com.sanctuarygames.createtransit.TransitText.uiText(DyeColor.byId(i).getName())));widgets.put(strip,y);bottom=Math.max(bottom,y+18);}

  addRenderableWidget(StationUi.button(x,height-27,w,20,"Done",b->onClose()));layout();
 }
 private void layout(){double max=Math.max(0,bottom-(height-38));target=Math.clamp(target,0,max);offset=Math.clamp(offset,0,max);for(var e:widgets.entrySet()){int y=e.getValue()-(int)Math.round(offset);e.getKey().setY(y);e.getKey().visible=y>=45&&y+e.getKey().getHeight()<=height-36;}}
 @Override public boolean mouseScrolled(double x,double y,double h,double v){target=Math.clamp(target-v*28,0,Math.max(0,bottom-(height-38)));return true;}
 @Override public void render(GuiGraphics g,int mx,int my,float partial){long now=System.nanoTime();double dt=time==0?.016:Math.min(.1,(now-time)/1e9);time=now;offset+=(target-offset)*(1-Math.exp(-18*dt));layout();g.fill(0,0,width,height,0xff101e2b);super.render(g,mx,my,partial);g.drawCenteredString(font,font.plainSubstrByWidth("Station Utilities • Client Settings",width-20),width/2,14,StationUi.INK);if(bottom>height-38){int span=height-83,thumb=Math.max(8,span*span/Math.max(1,bottom-45)),yy=45+(int)((span-thumb)*offset/Math.max(1,bottom-height+38));g.fill(width-8,45,width-5,height-38,0xff294355);g.fill(width-8,yy,width-5,yy+thumb,StationUi.accent());}}
 @Override public void onClose(){minecraft.setScreen(parent);}
 @Override public boolean isPauseScreen(){return false;}
}
