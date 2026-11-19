package com.sanctuarygames.createtransit.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
/** Uses the block atlas so labPBR companion maps can be sampled by terrain shaders. */
public final class TransitDoorRenderer implements BlockEntityRenderer<TransitBlockEntity> {
    public TransitDoorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(TransitBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!be.master()||!(be.getBlockState().getBlock() instanceof TransitBarrierBlock b)||b instanceof DisplayMountBlock)return;
        pose.pushPose();pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(switch(be.getBlockState().getValue(TransitBarrierBlock.FACING)){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));
        pose.translate(-.5,0,-.5);
        int frame=DyeColor.byId(be.paint()).getTextureDiffuseColor(),accent=DyeColor.byId(be.accent()).getTextureDiffuseColor();
        double slide=be.progress(partial)*PanelMesh.SLIDE;
        int status=be.getBlockState().getValue(TransitBarrierBlock.STATUS);
        boolean flash=b.isDoor()&&(status==1||status==3||status==4||status==5);
        boolean bright=!flash||be.getLevel()==null||(be.getLevel().getGameTime()/5)%2==0;
        String led=!bright?"led_off":b.isDoor()&&(status==1||status==2)?"led_green":flash?"led_amber":"led_white";
        // Finish the opaque atlas buffer before obtaining the translucent buffer.
        VertexConsumer out=buffers.getBuffer(RenderType.solid());
        int panel=be.getBlockState().getValue(TransitBarrierBlock.PANEL);
        var mesh=PanelMesh.leaf((int)b.pixelsHigh(),panel);
        if(b.isDoor())for(int side=0;side<2;side++){
            double x=(side==0?PanelMesh.JAMB:16)+(side==0?-slide:slide);
            for(var e:mesh)if(!e.texture().equals("glass"))
                cube(pose,out,e.texture(),e.tint()==0?frame:e.tint()==1?accent:0xffffff,light,x+e.x1(),e.y1(),e.z1(),x+e.x2(),e.y2(),e.z2(),e.faces(),x);
        }
        // Accent strips are stationary, dye-tinted and independent of status/flash.
        var lamps=b.isDoor()?(b.pixelsHigh()==32?PanelMesh.COMPACT_DOOR_LIGHTS:b.isFullHeight()?PanelMesh.FULL_DOOR_LIGHTS:PanelMesh.HALF_DOOR_LIGHTS)
            :(b.pixelsHigh()==32?PanelMesh.COMPACT_WALL_LIGHTS:b.isFullHeight()?PanelMesh.FULL_WALL_LIGHTS:PanelMesh.HALF_WALL_LIGHTS);
        var state=be.getBlockState();
        for(var e:lamps){
            double startY=e.y1();
            if(!b.isDoor()&&((e.x1()<2&&state.getValue(TransitBarrierBlock.LEFT))||(e.x1()>14&&state.getValue(TransitBarrierBlock.RIGHT)))){
                if(b.pixelsHigh()==48&&compactNeighbor(be,e.x1()<2))startY=Math.max(startY,28);
                else continue;
            }
            cube(pose,out,e.tint()==1?"led_accent":led,e.tint()==1?accent:0xffffff,LightTexture.FULL_BRIGHT,e.x1(),startY,e.z1(),e.x2(),e.y2(),e.z2(),e.faces(),0);
        }
        if(!b.isDoor()&&b.pixelsHigh()==48)for(boolean left:new boolean[]{true,false}){
            if(state.getValue(left?TransitBarrierBlock.LEFT:TransitBarrierBlock.RIGHT)&&compactNeighbor(be,left))
                for(var e:left?PanelMesh.TALL_TRIM_LEFT:PanelMesh.TALL_TRIM_RIGHT)
                    cube(pose,out,e.texture(),frame,light,e.x1(),e.y1(),e.z1(),e.x2(),e.y2(),e.z2(),e.faces(),0);
        }
        if(b.isDoor()){
            out=buffers.getBuffer(RenderType.translucent());
            for(int side=0;side<2;side++){
                double x=(side==0?PanelMesh.JAMB:16)+(side==0?-slide:slide);
                for(var e:mesh)if(e.texture().equals("glass"))cube(pose,out,e.texture(),0xffffff,light,x+e.x1(),e.y1(),e.z1(),x+e.x2(),e.y2(),e.z2(),e.faces(),x);
            }
        }
        pose.popPose();
    }
    private static boolean compactNeighbor(TransitBlockEntity be,boolean left){
        if(be.getLevel()==null)return false;
        Direction right=be.getBlockState().getValue(TransitBarrierBlock.FACING).getClockWise();
        var other=be.getLevel().getBlockState(be.getBlockPos().relative(left?right.getOpposite():right));
        return other.getBlock() instanceof TransitBarrierBlock next&&next.pixelsHigh()==32;
    }
    private static TextureAtlasSprite sprite(String name){return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"block/"+name));}
    private static void cube(PoseStack pose,VertexConsumer out,String texture,int color,int light,double x1,double y1,double z1,double x2,double y2,double z2){
        cube(pose,out,texture,color,light,x1,y1,z1,x2,y2,z2,63,0);
    }
    private static void cube(PoseStack pose,VertexConsumer out,String texture,int color,int light,double x1,double y1,double z1,double x2,double y2,double z2,int mask,double origin){
        var sp=sprite(texture);boolean glass=texture.equals("glass");double base=Math.floor(y1/16)*16;
        if((mask&1)!=0)face(pose,out,sp,color,light,0,0,-1,new double[][]{{x2,y1,z1},{x1,y1,z1},{x1,y2,z1},{x2,y2,z1}},glass,origin,base);
        if((mask&2)!=0)face(pose,out,sp,color,light,0,0,1,new double[][]{{x1,y1,z2},{x2,y1,z2},{x2,y2,z2},{x1,y2,z2}},glass,origin,base);
        if((mask&4)!=0)face(pose,out,sp,color,light,-1,0,0,new double[][]{{x1,y1,z1},{x1,y1,z2},{x1,y2,z2},{x1,y2,z1}},false,0,0);
        if((mask&8)!=0)face(pose,out,sp,color,light,1,0,0,new double[][]{{x2,y1,z2},{x2,y1,z1},{x2,y2,z1},{x2,y2,z2}},false,0,0);
        if((mask&16)!=0)face(pose,out,sp,color,light,0,1,0,new double[][]{{x1,y2,z2},{x2,y2,z2},{x2,y2,z1},{x1,y2,z1}},false,0,0);
        if((mask&32)!=0)face(pose,out,sp,color,light,0,-1,0,new double[][]{{x1,y1,z1},{x2,y1,z1},{x2,y1,z2},{x1,y1,z2}},false,0,0);
    }
    private static void face(PoseStack pose,VertexConsumer out,TextureAtlasSprite sp,int color,int light,float nx,float ny,float nz,double[][] v,boolean glass,double origin,double base){
        float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f;
        float[][] uv={{1,1},{0,1},{0,0},{1,0}};
        for(int i=0;i<4;i++){
            float u=glass?(float)(nz<0?16-(v[i][0]-origin):v[i][0]-origin)/16:uv[i][0];
            float w=glass?(float)(16-(v[i][1]-base))/16:uv[i][1];
            out.addVertex(pose.last(),(float)v[i][0]/16,(float)v[i][1]/16,(float)v[i][2]/16).setColor(r,g,b,1).setUv(sp.getU(u),sp.getV(w)).setLight(light).setNormal(pose.last(),nx,ny,nz);
        }
    }
    @Override public AABB getRenderBoundingBox(TransitBlockEntity be){return new AABB(be.getBlockPos()).inflate(4);}
    @Override public boolean shouldRenderOffScreen(TransitBlockEntity be){return true;}
}
