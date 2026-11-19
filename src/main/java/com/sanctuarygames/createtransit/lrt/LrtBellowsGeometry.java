package com.sanctuarygames.createtransit.lrt;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Interpolated rectangular pleats attach exactly to both carriage end planes. */
public final class LrtBellowsGeometry {
 public record Quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,String texture,int tint,float shade){}
 public static List<Quad> build(LrtArticulationLink.Pair pair){
  var a=pair.first();var b=pair.second();var br=b.right().dot(a.right())<0?b.right().scale(-1):b.right();
  int count=Math.max(10,Math.min(30,(int)Math.ceil(a.floor().distanceTo(b.floor())*8)));if(count%2!=0)count++;
  List<Quad> out=new ArrayList<>();Vec3[] previous=null;
  for(int n=0;n<=count;n++){
   double t=(double)n/count,w=n%2==0?2.5:2.43,inset=n%2==0?0:.055;
   var center=a.floor().lerp(b.floor(),t);var right=a.right().lerp(br,t);var up=a.up().lerp(b.up(),t);
   Vec3[] ring={center.add(right.scale(-w)).add(up.scale(.006)),center.add(right.scale(w)).add(up.scale(.006)),center.add(right.scale(w)).add(up.scale(pair.height()-inset)),center.add(right.scale(-w)).add(up.scale(pair.height()-inset))};
   if(previous!=null)for(int face=0;face<4;face++)out.add(new Quad(previous[face],previous[(face+1)%4],ring[(face+1)%4],ring[face],face==0?"lrt_floor":face==2?"lrt_ceiling":"lrt_bellows_dye",face==1||face==3?n%6==0?1:0:-1,face==0?.70f:face==2?.9f:n%2==0?.63f:.92f));previous=ring;
  }
  return out;
 }
}
