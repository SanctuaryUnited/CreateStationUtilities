package com.sanctuarygames.createtransit.elevator.client;
import com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
/** Front-facing pixel geometry: arrow direction does not depend on font coverage or texture orientation. */
public final class IndicatorArtwork {
 private static final ResourceLocation DISPLAY_PIXEL=ResourceLocation.fromNamespaceAndPath("createtransit","textures/gui/elevator_display_pixel.png");
 public static final int GRAY=0x606870,UP=0x80ff00,DOWN=0xff3030;
 public static void rect(PoseStack p,MultiBufferSource b,double x,double y,double w,double h,double z,int color){
  face(p,b.getBuffer(RenderType.entitySolid(DISPLAY_PIXEL)),x,y,w,h,z,color,LightTexture.FULL_BRIGHT,null);
 }
 static void metal(PoseStack p,MultiBufferSource b,double x,double y,double w,double h,double z,int color,int light){
  var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
      .apply(ResourceLocation.fromNamespaceAndPath("createtransit","block/elevator3/call_brushed_metal"));
  face(p,b.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),x,y,w,h,z,color,light,sprite);
 }
 private static void face(PoseStack p,com.mojang.blaze3d.vertex.VertexConsumer out,double x,double y,double w,double h,double z,int color,int light,net.minecraft.client.renderer.texture.TextureAtlasSprite sprite){
  for(int i=0;i<4;i++){float xx=(float)(i<2?x:x+w),yy=(float)(i==0||i==3?y:y+h);out.addVertex(p.last(),xx,yy,(float)z).setColor((color>>16)&255,(color>>8)&255,color&255,255).setUv(sprite==null?.5f:sprite.getU(i<2?0:1),sprite==null?.5f:sprite.getV(i==0||i==3?1:0)).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p.last(),0,0,-1);}
 }
 static void ring(PoseStack p,MultiBufferSource b,double x,double y,double outer,double inner,double z,int color){
  var out=b.getBuffer(RenderType.entitySolid(DISPLAY_PIXEL));
  for(int i=0;i<32;i++){
   double start=i*Math.PI/16,end=(i+1)*Math.PI/16;
   ringVertex(p,out,x,y,z,inner,start,color);
   ringVertex(p,out,x,y,z,inner,end,color);
   ringVertex(p,out,x,y,z,outer,end,color);
   ringVertex(p,out,x,y,z,outer,start,color);
  }
 }
 private static void ringVertex(PoseStack p,com.mojang.blaze3d.vertex.VertexConsumer out,double x,double y,double z,double radius,double angle,int color){
  out.addVertex(p.last(),(float)(x+radius*Math.cos(angle)),(float)(y+radius*Math.sin(angle)),(float)z)
      .setColor(color>>16&255,color>>8&255,color&255,255).setUv(.5f,.5f)
      .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p.last(),0,0,-1);
 }
 public static void insetPanel(PoseStack p,MultiBufferSource b,double x,double y,double w,double h,double edge,double z,int border,int fill){
  rect(p,b,x,y,w,edge,z,border);
  rect(p,b,x,y+h-edge,w,edge,z,border);
  rect(p,b,x,y+edge,edge,h-2*edge,z,border);
  rect(p,b,x+w-edge,y+edge,edge,h-2*edge,z,border);
  rect(p,b,x+edge,y+edge,w-2*edge,h-2*edge,z,fill);
 }
 public static void panel(PoseStack p,MultiBufferSource b,double x,double y,double w,double h,double z,IndicatorStyle style,boolean focus,int color){
  int border=focus?color:switch(style){case BRUSHED_METAL->0xb6c2c9;case CLASSIC_LED->0xa69875;case TRANSIT_SIGN->0xd0e4ee;case RECESSED_GLASS->0x26333c;default->0x627b89;};
  double thickness=style==IndicatorStyle.TRANSIT_SIGN?.025:style==IndicatorStyle.SLIMLINE?.008:.014;
  rect(p,b,x,y,w,thickness,z,border);
  rect(p,b,x,y+h-thickness,w,thickness,z,border);
  rect(p,b,x,y+thickness,thickness,h-2*thickness,z,border);
  rect(p,b,x+w-thickness,y+thickness,thickness,h-2*thickness,z,border);
  rect(p,b,x+thickness,y+thickness,w-2*thickness,h-2*thickness,z,0x080e14);
  if(style==IndicatorStyle.BRUSHED_METAL)for(double xx:new double[]{x+.018,x+w-.026})for(double yy:new double[]{y+.018,y+h-.026})rect(p,b,xx,yy,.008,.008,z-.002,0x38404a);
 }
 public static void arrow(PoseStack p,MultiBufferSource b,double cx,double cy,double size,double z,boolean up,int color,IndicatorStyle style){
  // Rows grow away from the tip. The screen's world Y increases upward.
  double unit=size/7;
  for(int row=0;row<4;row++)for(int col=0;col<7;col++){
   int distance=Math.abs(col-3);boolean edge=style==IndicatorStyle.SLIMLINE;
   if(edge?distance!=row:distance>row)continue;
   double gap=style==IndicatorStyle.PIXEL_MATRIX?.32:style==IndicatorStyle.CLASSIC_LED?.12:0;
   rect(p,b,cx+(col-3.5)*unit,cy+(up?1:-1)*(1.5-row)*unit,unit*(1-gap),unit*(1-gap),z,color);
  }
 }
}
