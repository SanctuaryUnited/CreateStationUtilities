package com.sanctuarygames.createtransit.elevator.model;
import java.util.*;
import com.sanctuarygames.createtransit.elevator.model.CabinGeometry.*;
/**
 * Smooth faces for the rear half of the Rounded cabin: a half circle around (cx, cz) from angle 0 (+x) through
 * pi/2 (+z, the back) to pi (-x). Visual only; collision keeps the stepped boxes.
 */
final class RoundedShell {
 private final double cx,cz;
 private final int segments,perTile;
 private final double[] cos,sin;
 /** Splits the curve into tiles about one block long, so wall textures repeat at roughly block scale. */
 RoundedShell(double cx,double cz,double radius){
  this.cx=cx;this.cz=cz;
  int tiles=Math.max(1,(int)Math.round(Math.PI*radius));
  int per=Math.max(2,(int)Math.ceil(32.0/tiles));if(per%2==1)per++; // an even count puts a vertex exactly at the back
  perTile=per;segments=tiles*per;cos=new double[segments+1];sin=new double[segments+1];
  for(int j=0;j<=segments;j++){
   // Exact values at the ends and the back keep the mesh exactly on the cabin's outline.
   if(j==0){cos[j]=1;sin[j]=0;}else if(j==segments){cos[j]=-1;sin[j]=0;}else if(2*j==segments){cos[j]=0;sin[j]=1;}
   else{double a=Math.PI*j/segments;cos[j]=Math.cos(a);sin[j]=Math.sin(a);}
  }
 }
 int segments(){return segments;}
 Point at(double r,int j,double y){return new Point(cx+r*cos[j],y,cz+r*sin[j]);}
 /** Point at an arbitrary angle, for small parts such as brackets. */
 Point at(double r,double angle,double y){return new Point(cx+r*Math.cos(angle),y,cz+r*Math.sin(angle));}
 /** The vertical surface at radius r, facing outward or inward. Wall textures tile along the curve. */
 void surface(List<Face> q,Material m,double r,double y0,double y1,boolean outward){
  for(int j=0;j<segments;j++){
   int i=j%perTile;double u0=(double)i/perTile,u1=(double)(i+1)/perTile;
   if(outward)q.add(new Face(at(r,j,y0),at(r,j,y1),at(r,j+1,y1),at(r,j+1,y0),m,u0,0,u1,1));
   else q.add(new Face(at(r,j+1,y0),at(r,j+1,y1),at(r,j,y1),at(r,j,y0),m,1-u1,0,1-u0,1));
  }
 }
 /** The flat ring between radii r0 and r1 at height y, facing up or down. */
 void ring(List<Face> q,Material m,double r0,double r1,double y,boolean up){
  for(int j=0;j<segments;j++)oriented(q,m,up?1:-1,at(r0,j,y),at(r1,j,y),at(r1,j+1,y),at(r0,j+1,y));
 }
 /** The flat half disc of radius r at height y, as a fan of triangles from the center. */
 void disc(List<Face> q,Material m,double r,double y,boolean up){
  var center=new Point(cx,y,cz);
  // (center, a, b) with a before b along the curve faces down; the triangle repeats its last point as a quad.
  for(int j=0;j<segments;j++){var a=at(r,j,y);var b=at(r,j+1,y);q.add(up?new Face(center,b,a,a,m):new Face(center,a,b,b,m));}
 }
 /** A curved bar: inner and outer surfaces plus top and bottom, from radius r0 to r1 and y0 to y1. */
 void band(List<Face> q,Material m,double r0,double r1,double y0,double y1,boolean top,boolean bottom){
  surface(q,m,r1,y0,y1,true);surface(q,m,r0,y0,y1,false);
  if(top)ring(q,m,r0,r1,y1,true);if(bottom)ring(q,m,r0,r1,y0,false);
 }
 /** A small radial block (a handrail bracket) centered on an angle, as six faces. */
 void bracket(List<Face> q,Material m,double r0,double r1,double angle,double width,double y0,double y1){
  double h=width/2/r1;double a0=angle-h,a1=angle+h;
  Point[] lo={at(r0,a0,y0),at(r1,a0,y0),at(r1,a1,y0),at(r0,a1,y0)},hi={at(r0,a0,y1),at(r1,a0,y1),at(r1,a1,y1),at(r0,a1,y1)};
  oriented(q,m,-1,lo[0],lo[1],lo[2],lo[3]);oriented(q,m,1,hi[0],hi[1],hi[2],hi[3]);
  for(int i=0;i<4;i++){int k=(i+1)%4;sideFace(q,m,lo[i],lo[k],hi[k],hi[i],angle,(r0+r1)/2,(y0+y1)/2);}
 }
 /** Vertical face of a small solid, wound to face away from the solid's middle. */
 private void sideFace(List<Face> q,Material m,Point a,Point b,Point c,Point d,double angle,double r,double y){
  var mid=at(r,angle,y);double fx=(a.x()+c.x())/2-mid.x(),fz=(a.z()+c.z())/2-mid.z();
  var n=normal(a,b,c);if(n[0]*fx+n[2]*fz<0)q.add(new Face(a,d,c,b,m));else q.add(new Face(a,b,c,d,m));
 }
 /** Adds a horizontal face wound so its normal points up (sign 1) or down (sign -1). */
 private static void oriented(List<Face> q,Material m,int sign,Point a,Point b,Point c,Point d){
  if(normal(a,b,c)[1]*sign<0)q.add(new Face(a,d,c,b,m));else q.add(new Face(a,b,c,d,m));
 }
 private static double[] normal(Point a,Point b,Point c){
  double ux=b.x()-a.x(),uy=b.y()-a.y(),uz=b.z()-a.z(),vx=c.x()-a.x(),vy=c.y()-a.y(),vz=c.z()-a.z();
  return new double[]{uy*vz-uz*vy,uz*vx-ux*vz,ux*vy-uy*vx};
 }
}
