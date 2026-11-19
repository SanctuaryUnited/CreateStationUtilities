package com.sanctuarygames.createtransit.elevator.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.sanctuarygames.createtransit.elevator.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
public final class ElevatorRenderer implements BlockEntityRenderer<ElevatorPartEntity> {
 public ElevatorRenderer(BlockEntityRendererProvider.Context c){}
 @Override public void render(ElevatorPartEntity be,float partial,PoseStack p,MultiBufferSource out,int light,int overlay){var s=be.getBlockState();if(!(s.getBlock() instanceof ElevatorPartBlock b))return;int paint=DyeColor.byId(be.paint()).getTextureDiffuseColor(),accent=DyeColor.byId(be.accent()).getTextureDiffuseColor();p.pushPose();p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(switch(s.getValue(ElevatorPartBlock.FACING)){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));p.translate(-.5,0,-.5);
  if(b instanceof ElevatorDoorBlock door&&s.getValue(ElevatorDoorBlock.ROW)==0){float progress=be.progress(partial);long time=be.getLevel()==null?0:be.getLevel().getGameTime();int signal=be.statusColor(partial,time);for(double z:new double[]{-.05,1.95}){box(p,out,"led",5,30.3,z,11,31.3,z+.1,signal,LightTexture.FULL_BRIGHT,false);for(double x:new double[]{.25,15.25})box(p,out,"led",x,.5,z,x+.5,29.2,z+.1,accent,LightTexture.FULL_BRIGHT,false);}
   for(int side=0;side<2;side++){p.pushPose();p.translate((side==0?-1:1)*progress*.42,0,0);double x=side==0?1.7:8.1,xx=side==0?7.9:14.3;
    if(door.solid())box(p,out,"metal",x,0,2.15,xx,30,2.95,paint,light,false);
    else {box(p,out,"metal",x,0,2.15,xx,9,2.95,paint,light,false);box(p,out,"metal",x,26,2.15,xx,30,2.95,paint,light,false);box(p,out,"metal",x,9,2.15,x+.65,26,2.95,paint,light,false);box(p,out,"metal",xx-.65,9,2.15,xx,26,2.95,paint,light,false);box(p,out,"glass",x+.65,9,2.35,xx-.65,26,2.75,0xffffff,light,true);}
    box(p,out,"led",x,4,3,xx,5.2,3.05,accent,LightTexture.FULL_BRIGHT,false);p.popPose();}
  }else if(b.kind==ElevatorPartBlock.Kind.CALL){
   TouchCallPanelRenderer.draw(be,p,out,light);
  }else if(b instanceof ElevatorSolidBlock){
   for(double y:new double[]{1,14.75})if(!s.getValue(y==1?ElevatorSolidBlock.DOWN:ElevatorSolidBlock.UP)){
    box(p,out,"led",b.kind==ElevatorPartBlock.Kind.CORNER?-.045:0,y,-.045,16,y+.75,-.005,accent,LightTexture.FULL_BRIGHT,false);
    box(p,out,"led",b.kind==ElevatorPartBlock.Kind.CORNER?2.005:0,y,2.005,16,y+.75,2.045,accent,LightTexture.FULL_BRIGHT,false);
    if(b.kind==ElevatorPartBlock.Kind.CORNER){
     box(p,out,"led",-.045,y,-.005,-.005,y+.75,16,accent,LightTexture.FULL_BRIGHT,false);
     box(p,out,"led",2.005,y,2.045,2.045,y+.75,16,accent,LightTexture.FULL_BRIGHT,false);
    }
   }
  }else if(b.kind==ElevatorPartBlock.Kind.LED)box(p,out,"led",7,0,-.05,9,16,.05,accent,LightTexture.FULL_BRIGHT,false);
  else if(ElevatorCeilingGeometry.isCeiling(b.kind))for(var lamp:ElevatorCeilingGeometry.lamps(s))box(p,out,"led",lamp.x(),lamp.y(),lamp.z(),lamp.X(),lamp.Y(),lamp.Z(),b.kind==ElevatorPartBlock.Kind.CEILING_LIGHT&&be.accent()==DyeColor.WHITE.getId()?0xffefd0:accent,LightTexture.FULL_BRIGHT,false);
  p.popPose();
 }
 private static void box(PoseStack p,MultiBufferSource out,String tex,double x,double y,double z,double X,double Y,double Z,int rgb,int light,boolean glass){var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ElevatorRegistry.id("block/elevator_"+tex));emitBox(p,out.getBuffer(glass?RenderType.translucent():RenderType.solid()),sprite,x,y,z,X,Y,Z,rgb,light,glass);}
 public static void emitBox(PoseStack p,VertexConsumer out,TextureAtlasSprite sprite,double x,double y,double z,double X,double Y,double Z,int rgb,int light,boolean glass){double[][] vertices={{x,y,z,X,y,z,X,Y,z,x,Y,z},{X,y,Z,x,y,Z,x,Y,Z,X,Y,Z},{x,y,Z,x,y,z,x,Y,z,x,Y,Z},{X,y,z,X,y,Z,X,Y,Z,X,Y,z},{x,Y,z,X,Y,z,X,Y,Z,x,Y,Z},{x,y,Z,X,y,Z,X,y,z,x,y,z}};float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};float[] shades={.86f,.86f,.72f,.72f,1,.6f};float[][] uv={{0,1},{1,1},{1,0},{0,0}};
  for(int f=0;f<6;f++){float shade=light==LightTexture.FULL_BRIGHT?1:shades[f];for(int i=0;i<4;i++){out.addVertex(p.last(),(float)vertices[f][3*(3-i)]/16,(float)vertices[f][3*(3-i)+1]/16,(float)vertices[f][3*(3-i)+2]/16).setColor(((rgb>>16)&255)/255f*shade,((rgb>>8)&255)/255f*shade,(rgb&255)/255f*shade,glass?.38f:1f).setUv(sprite.getU(uv[i][0]),sprite.getV(uv[i][1])).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p.last(),normals[f][0],normals[f][1],normals[f][2]);}}
 }
 @Override public AABB getRenderBoundingBox(ElevatorPartEntity be){return new AABB(be.getBlockPos()).inflate(2);}
 @Override public boolean shouldRenderOffScreen(ElevatorPartEntity be){return be.getBlockState().getBlock() instanceof ElevatorDoorBlock;}
}
