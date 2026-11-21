package com.sanctuarygames.createtransit.elevator.client;
import com.sanctuarygames.createtransit.elevator.model.*;
import com.mojang.blaze3d.vertex.PoseStack;import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.Font;import net.minecraft.client.renderer.*;
/** Both finished faces, with symbols outside the housing; no font dependency for arrow glyphs. */
public final class DoorIndicatorRenderer {
 private DoorIndicatorRenderer(){}
 private static void text(PoseStack p,MultiBufferSource b,String value,int color,float scale){var font=Minecraft.getInstance().font;p.mulPose(Axis.YP.rotationDegrees(180));p.scale(scale,-scale,scale);font.drawInBatch(value,-font.width(value)/2f,0,color,false,p.last().pose(),b,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);}
 public static void draw(CabinConfiguration c,PoseStack p,MultiBufferSource b,double z,double y,double depth,String floor,int direction,boolean mirror,boolean flash){draw(c,p,b,z,y,depth,floor,direction,mirror,flash,null);}
 public static void draw(CabinConfiguration c,PoseStack p,MultiBufferSource b,double z,double y,double depth,String floor,int direction,boolean mirror,boolean flash,String landing){
  draw(c,p,b,z,y,depth,floor,direction,direction,mirror,flash,landing,0);
 }
 public static void draw(CabinConfiguration c,PoseStack p,MultiBufferSource b,double z,double y,double depth,
     String floor,int direction,int lantern,boolean mirror,boolean flash,String landing,double scroll){
  draw(c,p,b,z,y,depth,floor,direction,lantern,mirror,flash,landing,scroll,false);
 }
 public static void draw(CabinConfiguration c,PoseStack p,MultiBufferSource b,double z,double y,double depth,
     String floor,int direction,int lantern,boolean mirror,boolean flash,String landing,double scroll,boolean directional){
  draw(c,p,b,z,y,depth,floor,direction,lantern,mirror,flash,landing,scroll,directional,true);
 }
 public static void draw(CabinConfiguration c,PoseStack p,MultiBufferSource b,double z,double y,double depth,
     String floor,int direction,int lantern,boolean mirror,boolean flash,String landing,double scroll,boolean directional,boolean powered){
  if(!c.indicators())return;double l=CabinGeometry.left(c),r=l+CabinGeometry.width(c),center=(l+r)/2;
  double bottom=y+(c.kit3()?2.35:1.89),top=y+(c.kit3()?2.65:1.985);if(c.compact()&&landing==null){bottom=CabinGeometry.compactY(bottom);top=CabinGeometry.compactY(top);}
  double height=top-bottom,yy=bottom+height*.18;
  for(boolean back:landing==null?new boolean[]{false,true}:new boolean[]{false}){
   p.pushPose();p.translate(center,0,back?z+depth+.014:z-.014);if(back)p.mulPose(Axis.YP.rotationDegrees(180));
   // All artwork is in front of its own panel, toward local negative Z.
   var style=c.indicatorStyle();IndicatorArtwork.panel(p,b,-(r-l)/2,yy-height*.18,r-l,height,0,style,powered,powered?accent(c):0x11161b);
   float scale=(float)Math.min(height*.073,(r-l-.5)/Math.max(1,Minecraft.getInstance().font.width(floor)));
   if(powered){p.pushPose();p.translate(0,yy+height*.5,-.016);text(p,b,style==CabinConfiguration.IndicatorStyle.CLASSIC_LED?"§l"+floor:floor,flash?0x607078:accentText(c),scale);p.popPose();
   for(boolean right:new boolean[]{false,true}){boolean up=right^mirror^back;positionArrow(p,b,right?(r-l)/2-.12:-(r-l)/2+.12,bottom+height*.5,height*.65,-.006,up,(up?direction>0:direction<0),directionColor(c,up,directional),style,scroll);}}
   p.popPose();
  }
  for(boolean right:new boolean[]{false,true}){
   var mount=DoorIndicatorLayout.mount(c,z,depth,right);
   drawJamb(c,p,b,mount.x(),y,mount.z(),mount.yaw(),mount.width(),lantern,landing,directional,powered);
  }
 }
 public static void cabinArrows(CabinConfiguration c,PoseStack pose,MultiBufferSource buffers,boolean rear,int direction,boolean directional){
  double start=Math.min(CabinGeometry.entranceFace(c,rear),CabinGeometry.passageEnd(c,rear));
  for(boolean right:new boolean[]{false,true}){
   var mount=DoorIndicatorLayout.mount(c,start,CabinGeometry.PASSAGE_DEPTH,right);
   drawJamb(c,pose,buffers,mount.x(),DoorIndicatorLayout.cabinJambBase(),mount.z(),mount.yaw(),mount.width(),direction,null,directional,true);
  }
 }
 static int accent(CabinConfiguration c){
  return net.minecraft.world.item.DyeColor.byId(c.ledColor()).getTextureDiffuseColor();
 }
 static double scroll(int direction,long time,float partial){
  return (Math.floorMod(time,12)+partial+.001)/12;
 }
 static void positionArrow(PoseStack pose,MultiBufferSource buffers,double x,double y,double size,double z,
     boolean up,boolean active,int accent,CabinConfiguration.IndicatorStyle style,double phase){
  if(!active||phase==0){
   IndicatorArtwork.arrow(pose,buffers,x,y,size*.80,z,up,active?accent:IndicatorArtwork.GRAY,style);
   return;
  }
  double spacing=size*.36;
  for(int i=-2;i<=2;i++){
   double offset=(i+phase)*(up?1:-1)*spacing;
   double fade=Math.clamp((size*.56-Math.abs(offset))/(size*.20),0,1);
   if(fade<=0)continue;
   int color=((int)((accent>>16&255)*fade)<<16)|((int)((accent>>8&255)*fade)<<8)|(int)((accent&255)*fade);
   IndicatorArtwork.arrow(pose,buffers,x,y+offset,size*.46,z,up,color,CabinConfiguration.IndicatorStyle.SLIMLINE);
  }
 }
 static void doorStrips(CabinConfiguration c,PoseStack pose,MultiBufferSource buffers,double y,
     double jambStart,double jambDepth,boolean rear,double progress,boolean opening,long tick){

  boolean blink=Math.floorMod(tick,10)<5;
  int rgb=opening?(progress>=.999||blink?0x32ff69:0):(blink?0xff3030:0);
  if(progress<=0&&!opening)rgb=0;
  double left=CabinGeometry.left(c),right=left+CabinGeometry.width(c);
  double inset=Math.min(.125,jambDepth*.25);
  double stripZ=rear?jambStart+jambDepth-inset:jambStart+inset;
  for(boolean rightSide:new boolean[]{false,true}){
   pose.pushPose();
   pose.translate(rightSide?right-.004:left+.004,y,stripZ);
   pose.mulPose(Axis.YP.rotationDegrees(rightSide?90:-90));
   double height=Math.min(1.81,DoorGeometry.panelTop(c)-.16);
   IndicatorArtwork.rect(pose,buffers,-.024,.065,.048,height+.03,0,0x101114);
   if(rgb!=0)IndicatorArtwork.rect(pose,buffers,-.0135,.08,.027,height,-.008,rgb);
   pose.popPose();
  }
 }
 static int directionColor(CabinConfiguration c,boolean up,boolean directional){
  return directional?(up?0x34e66a:0xff3030):accent(c);
 }
 static int accentText(CabinConfiguration c){
  int rgb=accent(c);
  return net.minecraft.util.FastColor.ARGB32.lerp(.30f,0xff000000|rgb,0xffffffff)&0xffffff;
 }
 private static void drawJamb(CabinConfiguration c,PoseStack p,MultiBufferSource b,double x,double y,
     double z,float yaw,double width,int direction,String landing,boolean directional,boolean powered){
  p.pushPose();
  p.translate(x,0,z);
  if(yaw!=0)p.mulPose(Axis.YP.rotationDegrees(yaw));
  var style=c.sideIndicatorStyle();
  IndicatorArtwork.panel(p,b,-width/2,y+1.24,width,.46,0,style,true,powered?accent(c):0x11161b);
  if(powered)for(boolean up:new boolean[]{true,false}){
   int color=(up?direction>0:direction<0)?(directional?(up?0x34e66a:0xff3030):accent(c)):IndicatorArtwork.GRAY;
   IndicatorArtwork.arrow(p,b,0,y+(up?1.58:1.40),Math.min(.155,width*.78),-.009,up,color,style);
  }
  if(landing!=null){
   IndicatorArtwork.panel(p,b,-width/2,y+1.01,width,.19,0,style,true,powered?accent(c):0x11161b);
   float scale=(float)Math.min(.014,(width-.025)/Math.max(1,Minecraft.getInstance().font.width(landing)));
   p.translate(0,y+1.15,-.009);
   if(powered)text(p,b,landing,accentText(c),scale);
  }
  p.popPose();
 }
}
