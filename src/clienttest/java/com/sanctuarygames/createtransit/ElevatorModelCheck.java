package com.sanctuarygames.createtransit;
import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sanctuarygames.createtransit.elevator.client.ElevatorRenderer;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;
import java.nio.file.*;
import java.util.*;
public final class ElevatorModelCheck {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class Sprite extends TextureAtlasSprite {Sprite(){super(TextureAtlas.LOCATION_BLOCKS,new SpriteContents(ResourceLocation.parse("createtransit:block/elevator_metal"),new FrameSize(32,32),new NativeImage(32,32,false),ResourceMetadata.EMPTY),512,512,0,0);}}
 public static void main(String[] args)throws Exception {int models=0,quads=0;var sprite=new Sprite();try(var files=Files.list(Path.of(args[0]).resolve("models/block"))){for(var p:files.filter(f->f.getFileName().toString().startsWith("elevator_")).toList()){var json=JsonParser.parseString(Files.readString(p)).getAsJsonObject();for(var e:json.getAsJsonObject("textures").entrySet())check(e.getValue().getAsString().startsWith("createtransit:block/elevator_"),"Every texture belongs to elevator kit");if(json.getAsJsonArray("elements").asList().stream().anyMatch(e->e.getAsJsonObject().getAsJsonObject("faces").entrySet().stream().anyMatch(f->f.getValue().getAsJsonObject().get("texture").getAsString().equals("#glass"))))check(!json.get("ambientocclusion").getAsBoolean()&&json.get("render_type").getAsString().equals("minecraft:translucent"),"Glass remains translucent with no black AO faces");var model=BlockModel.fromString(Files.readString(p));for(var rot:List.of(BlockModelRotation.X0_Y0,BlockModelRotation.X0_Y90,BlockModelRotation.X0_Y180,BlockModelRotation.X0_Y270))for(var e:model.getElements())for(var face:e.faces.entrySet()){var q=BlockModel.bakeFace(e,face.getValue(),sprite,face.getKey(),rot);quads++;check(q.getTintIndex()>=-1&&q.getTintIndex()<=1,"Only frame and accent tint channels");var v=q.getVertices();for(int i=0;i<4;i++){for(int a=0;a<3;a++)check(Float.isFinite(Float.intBitsToFloat(v[i*IQuadTransformer.STRIDE+IQuadTransformer.POSITION+a])),"Finite baked vertex");for(int a=0;a<2;a++){float uv=Float.intBitsToFloat(v[i*IQuadTransformer.STRIDE+IQuadTransformer.UV0+a]);check(uv>=0&&uv<=.06251f,"Native baked face stays within real 32px sprite");}}}models++;}}
 var out=new LrtModelCheck.Vertices();var pose=new PoseStack();int boxes=0;for(int angle:new int[]{0,90,180,270})for(boolean glass:new boolean[]{true,false})for(double progress:new double[]{0,.25,.5,1}){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(angle));ElevatorRenderer.emitBox(pose,out,UnitTextureAtlasSprite.INSTANCE,1.7-progress*6.72,0,2.15,7.9-progress*6.72,30,2.95,0xffffff,0,glass);pose.popPose();boxes++;}check(out.count==boxes*24&&out.uvCount==boxes*24,"Moving leaves retain 24 valid mapped vertices per box");
 Set<String> recipes=new HashSet<>();int count=0;try(var files=Files.list(Path.of(args[1]).resolve("recipe"))){for(var p:files.filter(f->f.getFileName().toString().startsWith("elevator_")).toList()){var json=JsonParser.parseString(Files.readString(p)).getAsJsonObject();json.remove("result");check(recipes.add(json.toString()),"Each elevator part has a distinct recipe");count++;}}check(count==24,"24 independent craftable parts");System.out.println("PASS: elevator kit: "+models+" models / "+quads+" native baked quads; "+boxes+" moving boxes / "+out.count+" emitted vertices; all four orientations, dedicated textures, dye channels, glass AO, normalized UV and 24 distinct recipes.");sprite.contents().close();
 }
}
