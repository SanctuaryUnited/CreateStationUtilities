package com.sanctuarygames.createtransit.elevator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared, allocation-free navy/cyan drawing primitives for all station menus. */
public final class StationUi {
 public static final int INK=0xffe4eff7,MUTED=0xff9bb4c7,CYAN=0xff18dafa,GREEN=0xff67e49b,YELLOW=0xffffd36d,RED=0xffff7684;
 public static int accent(){return com.sanctuarygames.createtransit.client.StationClientSettings.accent();}
 public static int themeSurface(int base,float amount){
  return net.minecraft.util.FastColor.ARGB32.lerp(amount,base,accent());
 }
 private StationUi(){}
 public static void panel(GuiGraphics g,int x,int y,int w,int h,int color,int edge){
  g.fill(x+3,y,x+w-3,y+h,edge);g.fill(x,y+3,x+w,y+h-3,edge);g.fill(x+1,y+1,x+w-1,y+h-1,edge);
  color=themeSurface(color,.13f);
  g.fillGradient(x+2,y+3,x+w-2,y+h-3,color,shade(color));g.fill(x+3,y+2,x+w-3,y+3,color);
 }
 private static int shade(int c){return 0xff000000|((c>>16&255)*3/4<<16)|((c>>8&255)*3/4<<8)|(c&255)*3/4;}
 public static Button button(int x,int y,int w,int h,String label,Button.OnPress action){return new Styled(x,y,w,h,label,action);}
 private static String sliderValue(String label,double value){
  if(label.equals("Bass boost"))return String.format(java.util.Locale.ROOT,"+%.1f dB",value*6);
  if(label.equals("Distance falloff"))return String.format(java.util.Locale.ROOT,"%.2f×",.5+value*1.5);
  if(label.equals("Wall muffling"))return Math.round(value*200)+"%";
  if(label.startsWith("Radius")||label.endsWith("range"))
   return com.sanctuarygames.createtransit.radio.RadioSettings.radiusFromSlider(value)+" blocks";
  return Math.round(value*(label.toLowerCase(java.util.Locale.ROOT).contains("volume")&&!label.equals("Motor volume")?300:100))+"%";
 }
 public static net.minecraft.client.gui.components.AbstractSliderButton slider(int x,int y,int w,String label,double value,java.util.function.DoubleConsumer save){return new net.minecraft.client.gui.components.AbstractSliderButton(x,y,w,20,Component.literal(label),Math.clamp(value,0,1)){
  protected void updateMessage(){setMessage(Component.literal(label+": "+sliderValue(label,this.value)));}protected void applyValue(){}public void onRelease(double x,double y){super.onRelease(x,y);save.accept(this.value);}
  public boolean keyPressed(int key,int scan,int mods){boolean result=super.keyPressed(key,scan,mods);if(result&&(key==262||key==263))save.accept(this.value);return result;}
  public void renderWidget(GuiGraphics g,int mx,int my,float p){panel(g,getX(),getY(),width,height,0xff152e42,isHoveredOrFocused()?accent():0xff52728a);int span=width-8;g.fill(getX()+4,getY()+height-5,getX()+4+(int)(span*this.value),getY()+height-3,accent());g.drawCenteredString(Minecraft.getInstance().font,label+": "+sliderValue(label,this.value),getX()+width/2,getY()+4,INK);}
 };}
 public static void swatch(Button button,int color){if(button instanceof Styled styled)styled.swatch=color;}
 public static Button tab(int x,int y,int w,String label,String icon,boolean selected,Button.OnPress action){return new Styled(x,y,w,20,(selected?"• ":"")+label,action,icon);}
 private static final class Styled extends Button {
  private final net.minecraft.resources.ResourceLocation icon;private int swatch=-1;

  Styled(int x,int y,int w,int h,String label,OnPress action){this(x,y,w,h,label,action,null);}
  Styled(int x,int y,int w,int h,String label,OnPress action,String icon){super(x,y,w,h,Component.literal(label),action,DEFAULT_NARRATION);this.icon=icon==null?null:net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("createtransit","textures/gui/station/"+icon+".png");}
  @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){boolean selected=getMessage().getString().startsWith("•");boolean primary=getMessage().getString().equals("Go")||getMessage().getString().equals("Activate");int edge=!active?0xff283a49:isHoveredOrFocused()||selected||primary?accent():0xff52728a;int bg=!active?0xff17232e:selected||primary?0xff074a67:isHoveredOrFocused()?0xff25465c:0xff213447;
   if(selected||isHoveredOrFocused())g.fill(getX()+5,getY()+height-1,getX()+width-5,getY()+height,accent());panel(g,getX(),getY(),width,height,bg,edge);
   var font=Minecraft.getInstance().font;String label=getMessage().getString();if(swatch>=0){g.fill(getX()+3,getY()+3,getRight()-3,getBottom()-3,0xff000000|swatch);int ink=((swatch>>16&255)*299+(swatch>>8&255)*587+(swatch&255)*114)>150000?0xff101820:0xfff4f8ff;g.drawCenteredString(font,font.plainSubstrByWidth(label,width-8),getX()+width/2,getY()+6,ink);return;}
   if(icon!=null){g.blit(icon,getX()+(label.replace("• ","").isBlank()?width/2-6:4),getY()+4,0,0,12,12,12,12);g.drawString(font,font.plainSubstrByWidth(label.replace("• ",""),width-24),getX()+20,getY()+6,active?INK:MUTED,false);}
   else if(label.equals("✎")){int x=getX()+width/2-4,y=getY()+height/2+2;for(int i=0;i<7;i++)g.fill(x+i,y-i,x+i+2,y-i+2,active?INK:MUTED);g.fill(x-1,y+3,x+2,y+4,INK);}
   else if(width>110&&(label.endsWith(": true")||label.endsWith(": false")||label.endsWith(": On")||label.endsWith(": Off"))){boolean on=label.endsWith(": true")||label.endsWith(": On");int x=getX()+width-30,y=getY()+5;g.fill(x,y,x+23,y+10,on?themeSurface(0xff263746,.4f):0xff344a5b);g.fill(x+(on?14:2),y+1,x+(on?21:9),y+9,on?accent():MUTED);g.drawString(font,font.plainSubstrByWidth(label.substring(0,label.lastIndexOf(':')),width-40),getX()+6,getY()+6,active?INK:MUTED,false);}
   else g.drawCenteredString(font,font.plainSubstrByWidth(label,width-8),getX()+width/2,getY()+(height-8)/2,active?INK:0xff687d90);
  }
 }
}
