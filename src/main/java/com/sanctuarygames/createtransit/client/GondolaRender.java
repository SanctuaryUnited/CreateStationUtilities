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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;

public final class GondolaRender {
    public static final class Pieces implements BlockEntityRenderer<GondolaBlockEntity> {
        public Pieces(BlockEntityRendererProvider.Context c){}
        @Override public void render(GondolaBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(!(be.getBlockState().getBlock() instanceof GondolaBlock b)||!b.isLed())return;
            pose.pushPose();orient(pose,be.getBlockState().getValue(GondolaBlock.FACING));
            boolean top=be.getBlockState().getValue(GondolaBlock.HALF)==Half.TOP;
            var lights=switch(b.kind()){
                case LED_PILLAR -> top?GondolaMesh.LED_PILLAR_TOP:GondolaMesh.LED_PILLAR_BOTTOM;
                case LED_VERTICAL -> top?GondolaMesh.LED_VERTICAL_TOP:GondolaMesh.LED_VERTICAL_BOTTOM;
                case LED_VERTICAL_DOUBLE -> top?GondolaMesh.LED_VERTICAL_DOUBLE_TOP:GondolaMesh.LED_VERTICAL_DOUBLE_BOTTOM;
                case LED_VERTICAL_CORNER -> top?GondolaMesh.LED_VERTICAL_CORNER_TOP:GondolaMesh.LED_VERTICAL_CORNER_BOTTOM;
                case LED_HORIZONTAL -> top?GondolaMesh.LED_HORIZONTAL_TOP:GondolaMesh.LED_HORIZONTAL_BOTTOM;
                case LED_HORIZONTAL_DOUBLE -> top?GondolaMesh.LED_HORIZONTAL_DOUBLE_TOP:GondolaMesh.LED_HORIZONTAL_DOUBLE_BOTTOM;
                case LED_HORIZONTAL_CORNER -> top?GondolaMesh.LED_HORIZONTAL_CORNER_TOP:GondolaMesh.LED_HORIZONTAL_CORNER_BOTTOM;
                case LED_VERTICAL_OUTER_CORNER -> top?GondolaMesh.LED_VERTICAL_OUTER_CORNER_TOP:GondolaMesh.LED_VERTICAL_OUTER_CORNER_BOTTOM;
                case LED_HORIZONTAL_OUTER_CORNER -> top?GondolaMesh.LED_HORIZONTAL_OUTER_CORNER_TOP:GondolaMesh.LED_HORIZONTAL_OUTER_CORNER_BOTTOM;
                default -> new GondolaMesh.Cube[0];
            };
            for(var e:lights)cube(pose,buffers.getBuffer(RenderType.solid()),e,DyeColor.byId(be.accent()).getTextureDiffuseColor(),LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
    public static final class Doors implements BlockEntityRenderer<GondolaDoorBlockEntity> {
        public Doors(BlockEntityRendererProvider.Context c){}
        @Override public void render(GondolaDoorBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var s=be.getBlockState();if(s.getBlock() instanceof GondolaWideDoorBlock){wide(be,partial,pose,buffers,light);return;}int paint=DyeColor.byId(be.paint()).getTextureDiffuseColor(),accent=DyeColor.byId(be.accent()).getTextureDiffuseColor();
            pose.pushPose();orient(pose,s.getValue(DoorBlock.FACING));
            var out=buffers.getBuffer(RenderType.solid());
            for(var e:GondolaMesh.DOOR_LIGHTS){if(s.getValue(GondolaDoorBlock.PAIRED)&&((s.getValue(DoorBlock.HINGE)==DoorHingeSide.LEFT&&e.x1()>8)||(s.getValue(DoorBlock.HINGE)==DoorHingeSide.RIGHT&&e.x1()<8)))continue;cube(pose,out,e,accent,LightTexture.FULL_BRIGHT);}
            for(var e:GondolaMesh.STATUS){var status=new GondolaMesh.Cube(e.x1(),e.y1(),e.z1(),e.x2(),e.y2(),e.z2(),s.getValue(DoorBlock.OPEN)?(be.progress(partial)>=1||(int)(be.progress(partial)*10)/3%2==0?"led_green":"led_off"):"led_white",-1,e.faces());cube(pose,out,status,0xffffff,LightTexture.FULL_BRIGHT);}
            float progress=be.progress(partial),slide=Math.max(0,(progress-.2f)/.8f),pop=Math.min(1,progress*5);
            // Pop out before sliding across the adjacent wall; all three surfaces stay separate.
            pose.pushPose();pose.translate((s.getValue(DoorBlock.HINGE)==DoorHingeSide.LEFT?-1:1)*slide*slide,0,-pop*.2);
            for(boolean glass:new boolean[]{false,true}){
                out=buffers.getBuffer(glass?RenderType.translucent():RenderType.solid());
                for(var e:GondolaMesh.LEAF)if(e.texture().equals("gondola_glass")==glass)cube(pose,out,e,e.tint()==0?paint:e.tint()==1?accent:0xffffff,light);
            }
            pose.popPose();pose.popPose();
        }
        private void wide(GondolaDoorBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var s=be.getBlockState();if(s.getValue(GondolaWideDoorBlock.COLUMN)!=0)return;
            boolean tall=s.getValue(GondolaWideDoorBlock.TALL);
            int paint=DyeColor.byId(be.paint()).getTextureDiffuseColor(),accent=DyeColor.byId(be.accent()).getTextureDiffuseColor();
            int signal=be.wideLedColor(partial);
            pose.pushPose();orient(pose,s.getValue(DoorBlock.FACING));
            var out=buffers.getBuffer(RenderType.solid());
            for(var e:tall?GondolaMesh.WIDE_LIGHTS:GondolaMesh.LEGACY_WIDE_LIGHTS)cube(pose,out,e,signal,LightTexture.FULL_BRIGHT);
            for(var e:tall?GondolaMesh.WIDE_STATUS:GondolaMesh.LEGACY_WIDE_STATUS)cube(pose,out,e,signal,LightTexture.FULL_BRIGHT);
            float progress=be.progress(partial),slide=WideDoorTiming.slide(progress),retract=WideDoorTiming.retract(progress);
            // Nearly flush leaves retract three pixels, then slide on the original clear rail.
            for(int side=0;side<2;side++){
                pose.pushPose();pose.translate((side==0?-1:1)*slide,0,retract*WideDoorTiming.RETRACT_PIXELS/16f);
                for(boolean glass:new boolean[]{false,true}){
                    out=buffers.getBuffer(glass?RenderType.translucent():RenderType.solid());
                    for(var e:side==0?(tall?GondolaMesh.WIDE_LEFT:GondolaMesh.LEGACY_WIDE_LEFT):(tall?GondolaMesh.WIDE_RIGHT:GondolaMesh.LEGACY_WIDE_RIGHT))if(e.texture().equals("gondola_glass")==glass)cube(pose,out,e,e.tint()==0?paint:e.tint()==1?accent:0xffffff,light);
                }
                out=buffers.getBuffer(RenderType.solid());
                var center=side==0?(tall?GondolaMesh.WIDE_CENTER_LEFT:GondolaMesh.LEGACY_WIDE_CENTER_LEFT):(tall?GondolaMesh.WIDE_CENTER_RIGHT:GondolaMesh.LEGACY_WIDE_CENTER_RIGHT);
                for(var e:center)cube(pose,out,e,be.centerLedColor(partial),be.centerLedColor(partial)==WideDoorTiming.OFF?light:LightTexture.FULL_BRIGHT);
                pose.popPose();
            }
            pose.popPose();
        }
        @Override public AABB getRenderBoundingBox(GondolaDoorBlockEntity be){return new AABB(be.getBlockPos()).inflate(4);}
        @Override public boolean shouldRenderOffScreen(GondolaDoorBlockEntity be){return true;}
    }
    static void orient(PoseStack p,Direction f){p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(switch(f){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));p.translate(-.5,0,-.5);}
    private static void cube(PoseStack pose,VertexConsumer out,GondolaMesh.Cube e,int color,int light){
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
}
