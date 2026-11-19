package com.sanctuarygames.createtransit.elevator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sanctuarygames.createtransit.elevator.model.CabinSelectorLayout;
import com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

final class CabinSelectorRenderer {
 private CabinSelectorRenderer(){}

 static void draw(ModelCabinEntity cabin,PoseStack pose,MultiBufferSource buffers,boolean rear){
  var c=cabin.configuration();
  double z=CabinSelectorLayout.screenPlane(c,rear);
  pose.pushPose();
  pose.translate(CabinSelectorLayout.center(c,rear),CabinSelectorLayout.BOTTOM,z);
  if(!rear)pose.mulPose(Axis.YP.rotationDegrees(180));
  double w=CabinSelectorLayout.WIDTH/32,h=CabinSelectorLayout.HEIGHT/64;
  // Adjacent opaque regions leave no second surface under the tiles or header.
  IndicatorArtwork.rect(pose,buffers,-16*w,0,32*w,7*h,0,0x243847);
  IndicatorArtwork.rect(pose,buffers,-16*w,41*h,32*w,h,0,DoorIndicatorRenderer.accent(c));
  IndicatorArtwork.rect(pose,buffers,-16*w,42*h,32*w,22*h,0,0x203442);
  var player=Minecraft.getInstance().player;
  var hit=player==null?null:cabin.selectorHit(player);
  int[] queued=cabin.upcomingStops();


  int accent=DoorIndicatorRenderer.accent(c);
  var stops=cabin.selectorStops();
  int target=cabin.destination();
  String destination="--";
  for(int i=0;i<stops.size();i++){
   var stop=stops.getCompound(i);
   if(stop.getInt("Y")==target){destination=stop.getString("Id");break;}
  }
  text(pose,buffers,"DESTINATION",0,58*h,28*w,4*h,DoorIndicatorRenderer.accentText(c));
  text(pose,buffers,destination,0,46*h,28*w,12*h,DoorIndicatorRenderer.accentText(c));
  int pages=CabinSelectorLayout.pages(stops.size());
  int page=cabin.selectorPage(),offset=page*CabinSelectorLayout.MAX_TILES;
  int tileCount=Math.min(stops.size()-offset,CabinSelectorLayout.MAX_TILES);
  for(int row=0;row<3;row++){
   double y=(54-CabinSelectorLayout.tileY(row*2))*h;
   IndicatorArtwork.rect(pose,buffers,-16*w,y,3*w,10*h,0,0x0b141c);
   IndicatorArtwork.rect(pose,buffers,-w,y,2*w,10*h,0,0x0b141c);
   IndicatorArtwork.rect(pose,buffers,13*w,y,3*w,10*h,0,0x0b141c);
   if(row<2)IndicatorArtwork.rect(pose,buffers,-16*w,y-2*h,32*w,2*h,0,0x0b141c);
  }
  for(int i=0;i<CabinSelectorLayout.MAX_TILES;i++){
   double x=(4-CabinSelectorLayout.tileX(i))*w,y=(54-CabinSelectorLayout.tileY(i))*h;
   if(i>=tileCount){
    IndicatorArtwork.rect(pose,buffers,x,y,12*w,10*h,0,0x0b141c);
    continue;
   }
   var stop=stops.getCompound(offset+i);
   boolean selected=false;
   for(int floor:queued)if(floor==stop.getInt("Y")){selected=true;break;}
   boolean hovered=hit!=null&&(hit.z==1)==rear&&CabinSelectorLayout.tileAt(hit.x,hit.y,stops.size()-offset)==i;
   boolean available=stop.getBoolean("Available"),denied=cabin.denied(stop.getInt("Y"));
   int border=denied?0xff3030:!available?0x44484e:selected||hovered?accent:0x34454f;
   int face=denied?0x871d29:!available?0x24262a:hovered?accent:selected?net.minecraft.util.FastColor.ARGB32.lerp(.30f,0xff172029,0xff000000|accent)&0xffffff:0x172029;
   IndicatorArtwork.insetPanel(pose,buffers,x,y,12*w,10*h,w,0,border,face);
   text(pose,buffers,stop.getString("Id"),x+6*w,y+5*h,10*w,5*h,!available&&!denied?0x81868b:hovered&&available&&((accent>>16&255)*3+(accent>>8&255)*6+(accent&255))>1250?0x101820:0xe2e9ec);
  }
  if(pages>1){
   IndicatorArtwork.arrow(pose,buffers,11*w,3*h,5*w,-.006,false,page+1<pages?0xb5efff:0x52606b,c.indicatorStyle());
   IndicatorArtwork.arrow(pose,buffers,-11*w,3*h,5*w,-.006,true,page>0?0xb5efff:0x52606b,c.indicatorStyle());
   text(pose,buffers,(page+1)+" of "+pages,0,3*h,16*w,4*h,0xb5c5cf);
  }
  pose.popPose();
 }

 static void text(PoseStack pose,MultiBufferSource buffers,String value,double x,double y,double width,double height,int color){
  Font font=Minecraft.getInstance().font;
  float scale=(float)Math.min(width/Math.max(1,font.width(value)),height/font.lineHeight);
  pose.pushPose();
  pose.translate(x,y+font.lineHeight*scale/2,-.019);
  pose.mulPose(Axis.YP.rotationDegrees(180));
  pose.scale(scale,-scale,scale);
  font.drawInBatch(value,-font.width(value)/2f,0,0xff000000|color,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);
  pose.popPose();
 }
}
