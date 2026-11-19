package com.sanctuarygames.createtransit.lrt.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
/** Shared dynamic vertex emission, with atlas-safe sprite-local UVs. */
public final class LrtMeshRenderer {
 public static void face(PoseStack pose,VertexConsumer out,TextureAtlasSprite sprite,LrtMesh.Face e,int color,int light){
  Vec3[] v=new Vec3[4];for(int i=0;i<4;i++)v[i]=new Vec3(e.vertices()[3*i]/16,e.vertices()[3*i+1]/16,e.vertices()[3*i+2]/16);
  var n=v[1].subtract(v[0]).cross(v[2].subtract(v[0])).normalize();
  for(int i=0;i<4;i++)out.addVertex(pose.last(),(float)v[i].x,(float)v[i].y,(float)v[i].z)
   .setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1)
   .setUv(sprite.getU(Math.clamp(e.uv()[2*i],0,16)/16f),sprite.getV(Math.clamp(e.uv()[2*i+1],0,16)/16f)).setLight(light)
   .setNormal(pose.last(),(float)n.x,(float)n.y,(float)n.z);
 }
 private LrtMeshRenderer(){}
}
