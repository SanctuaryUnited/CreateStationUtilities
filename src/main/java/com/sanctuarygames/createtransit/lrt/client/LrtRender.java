package com.sanctuarygames.createtransit.lrt.client;
import com.sanctuarygames.createtransit.block.WideDoorTiming;
import com.sanctuarygames.createtransit.lrt.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.*;
public final class LrtRender {
 public static final class Parts implements BlockEntityRenderer<LrtBlockEntity>{
  public Parts(BlockEntityRendererProvider.Context c){}
  @Override public void render(LrtBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
   if(!(be.getBlockState().getBlock()instanceof LrtBlock b))return;
   var s=be.getBlockState();
   if(b.kind()==LrtBlock.Kind.ARTICULATION_ANCHOR){var pair=LrtArticulationLink.findPlaced(be.getLevel(),be.getBlockPos(),s);if(pair!=null)bridge(pair,pose,buffers,v->v.subtract(Vec3.atLowerCornerOf(be.getBlockPos())),p->LevelRenderer.getLightColor(be.getLevel(),net.minecraft.core.BlockPos.containing(p)),be.paint(),be.accent());return;}
   if(b.kind()==LrtBlock.Kind.PANTOGRAPH){pantograph(be,partial,pose,buffers,light);return;}
   var lamps=switch(b.kind()){
    case ROOF->LrtMesh.CEILING_LIGHTS;
    case LED_VERTICAL->LrtMesh.LED_VERTICAL;
    case LED_HORIZONTAL->s.getValue(LrtBlock.HALF)==Half.TOP?LrtMesh.LED_HORIZONTAL_TOP:LrtMesh.LED_HORIZONTAL_BOTTOM;
    case CAB_LOWER->new LrtMesh.Face[0];
    default->new LrtMesh.Face[0];};
   if(b.kind()==LrtBlock.Kind.CAB_LOWER){pose.pushPose();cabLights(pose,buffers,s,true);pose.popPose();return;}
   if(lamps.length==0)return;
   pose.pushPose();orient(pose,s.getValue(LrtBlock.FACING));
   for(var e:lamps)face(pose,buffers.getBuffer(RenderType.solid()),e,e.tint()==1?DyeColor.byId(be.accent()).getTextureDiffuseColor():0xffffff,LightTexture.FULL_BRIGHT);
   pose.popPose();
  }
  @Override public AABB getRenderBoundingBox(LrtBlockEntity be){return new AABB(be.getBlockPos()).inflate(7);}
  @Override public boolean shouldRenderOffScreen(LrtBlockEntity be){return be.getBlockState().is(LrtRegistry.ARTICULATION_ANCHOR.get());}
 }
 public static void cabLights(PoseStack pose,MultiBufferSource buffers,net.minecraft.world.level.block.state.BlockState s,boolean leading){
  orient(pose,s.getValue(LrtBlock.FACING));int color=leading?0xf6f8ff:0xff2520;
  for(var e:LrtMesh.cabLights(s.getValue(LrtBlock.LEFT),s.getValue(LrtBlock.RIGHT)))face(pose,buffers.getBuffer(RenderType.solid()),e,color,LightTexture.FULL_BRIGHT);
 }
 public static final class Doors implements BlockEntityRenderer<LrtDoorBlockEntity>{
  public Doors(BlockEntityRendererProvider.Context c){}
  @Override public void render(LrtDoorBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
   var s=be.getBlockState();if(s.getValue(LrtDoorBlock.COLUMN)!=0)return;
   int paint=DyeColor.byId(be.paint()).getTextureDiffuseColor(),accent=DyeColor.byId(be.accent()).getTextureDiffuseColor(),signal=be.signal(partial);
   boolean tall=s.getValue(LrtDoorBlock.TALL);
   pose.pushPose();orient(pose,s.getValue(DoorBlock.FACING));var out=buffers.getBuffer(RenderType.solid());
   for(var e:tall?LrtMesh.TALL_LIGHTS:LrtMesh.LIGHTS)face(pose,out,e,signal,LightTexture.FULL_BRIGHT);
   for(var e:tall?LrtMesh.TALL_STATUS:LrtMesh.STATUS)face(pose,out,e,signal,LightTexture.FULL_BRIGHT);
   float p=be.progress(partial),slide=WideDoorTiming.slide(p),depth=WideDoorTiming.retract(p)*LrtDoorBlockEntity.RETRACT_PIXELS/16f;
   for(int side=0;side<2;side++){
    pose.pushPose();pose.translate(side==0?-slide:slide,0,-depth);
    for(boolean glass:new boolean[]{false,true}){
     out=buffers.getBuffer(glass?RenderType.translucent():RenderType.solid());
     for(var e:side==0?(tall?LrtMesh.TALL_LEFT:LrtMesh.LEFT):(tall?LrtMesh.TALL_RIGHT:LrtMesh.RIGHT))if(e.texture().equals("lrt_glass")==glass)face(pose,out,e,e.tint()==0?paint:e.tint()==1?accent:0xffffff,light);
    }
    for(var e:side==0?(tall?LrtMesh.TALL_CENTER_LEFT:LrtMesh.CENTER_LEFT):(tall?LrtMesh.TALL_CENTER_RIGHT:LrtMesh.CENTER_RIGHT))face(pose,buffers.getBuffer(RenderType.solid()),e,be.centerSignal(partial),be.centerSignal(partial)==0?light:LightTexture.FULL_BRIGHT);
    pose.popPose();
   }pose.popPose();
  }
  @Override public AABB getRenderBoundingBox(LrtDoorBlockEntity be){return new AABB(be.getBlockPos()).inflate(4);}
  @Override public boolean shouldRenderOffScreen(LrtDoorBlockEntity be){return true;}
 }
 static void pantograph(LrtBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light){
  float p=be.pantographProgress(partial);double angle=Math.toDegrees(LrtPantographMotion.angle(p));int paint=DyeColor.byId(be.paint()).getTextureDiffuseColor(),accent=DyeColor.byId(be.accent()).getTextureDiffuseColor();
  pose.pushPose();orient(pose,be.getBlockState().getValue(LrtBlock.FACING));
  for(int x:new int[]{3,12}){
   pose.pushPose();pose.translate(x/16.,-8.5/16.,2/16.);pose.mulPose(Axis.XP.rotationDegrees((float)(90-angle)));draw(pose,buffers,LrtMesh.PANTO_ARM,paint,accent,light);pose.popPose();
   pose.pushPose();pose.translate(x/16.,LrtPantographMotion.elbowY(p)/16.,LrtPantographMotion.elbowZ(p)/16.);pose.mulPose(Axis.XP.rotationDegrees((float)(angle-90)));draw(pose,buffers,LrtMesh.PANTO_ARM,paint,accent,light);pose.popPose();
  }
  pose.pushPose();pose.translate(0,LrtPantographMotion.headY(p)/16.,0);draw(pose,buffers,LrtMesh.PANTO_HEAD,paint,accent,light);pose.popPose();pose.popPose();
 }
 static void draw(PoseStack pose,MultiBufferSource buffers,LrtMesh.Face[] faces,int paint,int accent,int light){var out=buffers.getBuffer(RenderType.solid());for(var f:faces)face(pose,out,f,f.tint()==0?paint:f.tint()==1?accent:0xffffff,light);}
 public static void bridge(LrtArticulationLink.Pair pair,PoseStack pose,MultiBufferSource buffers,java.util.function.UnaryOperator<Vec3> local,java.util.function.ToIntFunction<Vec3> illumination,int paint,int accent){
  for(var q:LrtBellowsGeometry.build(pair)){var v=new Vec3[]{local.apply(q.a()),local.apply(q.b()),local.apply(q.c()),local.apply(q.d())};var midpoint=q.a().add(q.c()).scale(.5);int light=illumination.applyAsInt(midpoint);int rgb=q.tint()==0?DyeColor.byId(paint).getTextureDiffuseColor():q.tint()==1?DyeColor.byId(accent).getTextureDiffuseColor():0xffffff;int color=shade(rgb,q.shade());for(boolean reverse:new boolean[]{false,true}){
   var points=new double[12];var uv=new float[]{0,16,16,16,16,0,0,0};for(int i=0;i<4;i++){var p=v[reverse?3-i:i];points[3*i]=p.x*16;points[3*i+1]=p.y*16;points[3*i+2]=p.z*16;}
   face(pose,buffers.getBuffer(RenderType.solid()),new LrtMesh.Face(points,uv,q.texture(),-1),color,light);
  }}
 }
 private static int shade(int rgb,float factor){return ((int)(((rgb>>16)&255)*factor)<<16)|((int)(((rgb>>8)&255)*factor)<<8)|(int)((rgb&255)*factor);}
 static void orient(PoseStack p,Direction f){p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(switch(f){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));p.translate(-.5,0,-.5);}
 static void face(PoseStack pose,VertexConsumer out,LrtMesh.Face e,int color,int light){
  var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(LrtRegistry.id("block/"+e.texture()));
  LrtMeshRenderer.face(pose,out,sprite,e,color,light);
 }
}
