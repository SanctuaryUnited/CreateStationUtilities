package com.sanctuarygames.createtransit.escalator;
import java.util.*;
/** Moving mesh definition, independently compared with the asset preview. */
public final class EscalatorVisuals {
 public record Part(double x,double y,double z,double X,double Y,double Z,String texture,boolean slope){}
 public static List<Part> parts(EscalatorBlock.Kind kind,boolean left,boolean right,double phase,boolean reverse,boolean stopped){
  boolean slope=kind==EscalatorBlock.Kind.INCLINE;double x=left?3.02:0,X=right?12.98:16;var out=new ArrayList<Part>();
  for(var t:EscalatorGeometry.treads(slope,phase)){
   double z=Math.max(kind==EscalatorBlock.Kind.UPPER_LANDING?4:0,(1-t.to())*16),Z=Math.min(kind==EscalatorBlock.Kind.LOWER_LANDING?12:16,(1-t.from())*16),y=t.height()*16;
   if(Z<=z)continue;
   out.add(new Part(x,y-4.1,z,X,y,Z,"steps",false));
   out.add(new Part(x,y+.008,z,X,y+.025,Math.min(Z,z+.14),"rail",false));
   if(left)out.add(new Part(x,y+.03,z,x+.55,y+.055,Z,"edge",false));
   if(right)out.add(new Part(X-.55,y+.03,z,X,y+.055,Z,"edge",false));
  }
  for(boolean l:new boolean[]{true,false}){
   if(!(l?left:right))continue;double xx=l?.4:13.4;
   for(int i=0;i<4;i++){
    double u=EscalatorGeometry.wrap(phase+i/4d),z=(1-u)*16,Z=Math.min(16,z+.13);
    if(Z<=z)continue;
    if(kind==EscalatorBlock.Kind.LOWER_LANDING&&z>11.5)continue;
    if(kind==EscalatorBlock.Kind.UPPER_LANDING&&z<4.5)continue;
    double y=slope?19.44:19.04;out.add(new Part(xx,y,z,xx+2.2,y+.04,Z,"grip",slope));
   }
  }
  if(kind==EscalatorBlock.Kind.LOWER_LANDING||kind==EscalatorBlock.Kind.UPPER_LANDING){
   boolean entry=(kind==EscalatorBlock.Kind.LOWER_LANDING)!=reverse;
   String tex=entry&&!stopped?"arrow":"stop";double z=kind==EscalatorBlock.Kind.LOWER_LANDING?15.86:.14;
   if(left)out.add(new Part(.45,5.2,z-.012,2.55,9.5,z+.012,tex,false));
   if(right)out.add(new Part(13.45,5.2,z-.012,15.55,9.5,z+.012,tex,false));
  }
  return out;
 }
 public static double[][] vertices(Part p){
  double x=p.x,y=p.y,z=p.z,X=p.X,Y=p.Y,Z=p.Z;
  double[][] vs={{x,y,z,x,Y,z,X,Y,z,X,y,z},{X,y,Z,X,Y,Z,x,Y,Z,x,y,Z},
   {x,y,Z,x,Y,Z,x,Y,z,x,y,z},{X,y,z,X,Y,z,X,Y,Z,X,y,Z},
   {x,Y,z,x,Y,Z,X,Y,Z,X,Y,z},{x,y,Z,x,y,z,X,y,z,X,y,Z}};
  if(p.slope)for(var v:vs)for(int i=0;i<12;i+=3)v[i+1]+=16-v[i+2];
  return vs;
 }
}
