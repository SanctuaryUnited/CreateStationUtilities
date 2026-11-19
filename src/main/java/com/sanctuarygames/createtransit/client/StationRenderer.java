package com.sanctuarygames.createtransit.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
public final class StationRenderer implements BlockEntityRenderer<StationBlockEntity> {
    public StationRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(StationBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!be.master())return;var block=(StationBlock)be.getBlockState().getBlock();boolean gate=block.isGate();
        int frame=DyeColor.byId(be.paint()).getTextureDiffuseColor(),accent=DyeColor.byId(be.accent()).getTextureDiffuseColor();
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(switch(be.getBlockState().getValue(StationBlock.FACING)){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));pose.translate(-.5,0,-.5);
        var out=buffers.getBuffer(RenderType.solid());
        for(var e:block.isKiosk()?StationMesh.KIOSK_LIGHTS:StationMesh.GATE_LIGHTS)cube(pose,out,e,e.tint()==1?accent:0xffffff,LightTexture.FULL_BRIGHT);
        if(gate){
            drawLeaf(pose,out,be.progress(partial),frame,light,false,be.getBlockState().getValue(StationBlock.MIRRORED));
            out=buffers.getBuffer(RenderType.translucent());
            drawLeaf(pose,out,be.progress(partial),frame,light,true,be.getBlockState().getValue(StationBlock.MIRRORED));
        }else if(block.isKiosk()&&be.dispensing()>0){
            float t=be.ticketProgress(partial);double reach=Math.sin(Math.PI*t)*3.2;
            cube(pose,out,new StationMesh.Cube(5,12.7,4.7-reach,9,13.3,4.8,"ticket_paper",-1,63),0xffffff,light);
        }
        pose.popPose();
    }
    private void drawLeaf(PoseStack pose,VertexConsumer out,float progress,int frame,int light,boolean glass,boolean mirrored){
        pose.pushPose();pose.translate(mirrored?0:1,0,8/16.0);pose.mulPose(Axis.YP.rotationDegrees((mirrored?90:-90)*progress));pose.translate(0,0,-8/16.0);
        for(var e:StationMesh.LEAF)if(e.texture().equals("glass")==glass){
            var leaf=mirrored?new StationMesh.Cube(-e.x2(),e.y1(),e.z1(),-e.x1(),e.y2(),e.z2(),e.texture(),e.tint(),e.faces()):e;
            cube(pose,out,leaf,glass?0xffffff:frame,light);
        }
        pose.popPose();
    }
    private static void cube(PoseStack pose,VertexConsumer out,StationMesh.Cube e,int color,int light){
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"block/"+e.texture()));
        double x=e.x1(),y=e.y1(),z=e.z1(),X=e.x2(),Y=e.y2(),Z=e.z2();int m=e.faces();
        if((m&1)!=0)face(pose,out,sprite,color,light,0,0,-1,new double[][]{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}});
        if((m&2)!=0)face(pose,out,sprite,color,light,0,0,1,new double[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}});
        if((m&4)!=0)face(pose,out,sprite,color,light,-1,0,0,new double[][]{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}});
        if((m&8)!=0)face(pose,out,sprite,color,light,1,0,0,new double[][]{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}});
        if((m&16)!=0)face(pose,out,sprite,color,light,0,1,0,new double[][]{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}});
        if((m&32)!=0)face(pose,out,sprite,color,light,0,-1,0,new double[][]{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}});
    }
    private static void face(PoseStack pose,VertexConsumer out,TextureAtlasSprite s,int c,int light,float nx,float ny,float nz,double[][] v){
        float[][] uv={{0,1},{1,1},{1,0},{0,0}};
        for(int i=0;i<4;i++)out.addVertex(pose.last(),(float)v[i][0]/16,(float)v[i][1]/16,(float)v[i][2]/16).setColor(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,1).setUv(s.getU(uv[i][0]),s.getV(uv[i][1])).setLight(light).setNormal(pose.last(),nx,ny,nz);
    }
    @Override public AABB getRenderBoundingBox(StationBlockEntity be){return new AABB(be.getBlockPos()).inflate(3);}
    @Override public boolean shouldRenderOffScreen(StationBlockEntity be){return true;}
}
