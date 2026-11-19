package com.sanctuarygames.createtransit.elevator.client;
import java.util.*;import com.sanctuarygames.createtransit.elevator.model.CabinGeometry.*;
/** Clip real polygons at block-sized UV boundaries; curved floor fans retain their true shape. */
public final class TiledCabinMesh {
 public record Vertex(Point p,double u,double v){}
 public record Tile(Face source,List<Vertex> vertices,double nx,double ny,double nz,int referenceVertices){}
 private static final Map<List<Face>,List<Tile>> CACHE=new LinkedHashMap<>(64,.75f,true){protected boolean removeEldestEntry(Map.Entry<List<Face>,List<Tile>> e){return size()>64;}};
 public static List<Tile> mesh(List<Face> faces){return CACHE.computeIfAbsent(faces,TiledCabinMesh::build);}
 private static List<Tile> build(List<Face> faces){
  var result=new ArrayList<Tile>();
  for(var face:faces)result.addAll(buildFace(face));
  return List.copyOf(result);
 }
 static List<Tile> buildFace(Face face){
  var result=new ArrayList<Tile>();
  var a=face.a();var b=face.b();var c=face.c();
  double ux=b.x()-a.x(),uy=b.y()-a.y(),uz=b.z()-a.z(),vx=c.x()-a.x(),vy=c.y()-a.y(),vz=c.z()-a.z();
  double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
  boolean horizontal=Math.abs(ny)>Math.max(Math.abs(nx),Math.abs(nz)),side=Math.abs(nx)>Math.abs(nz);
  double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
  if(length<1e-8)return List.of();
  if(!horizontal&&!side&&(face.material()==Material.COPY_PRIMARY||face.material()==Material.DOOR_GLASS)){
   return List.of(new Tile(face,List.of(
       new Vertex(a,face.u0(),face.v1()),new Vertex(b,face.u0(),face.v0()),
       new Vertex(c,face.u1(),face.v0()),new Vertex(face.d(),face.u1(),face.v1())),
       nx/length,ny/length,nz/length,4));
  }
  var polygon=new ArrayList<Vertex>();for(var p:List.of(face.a(),face.b(),face.c(),face.d()))polygon.add(new Vertex(p,horizontal?p.x():side?p.z():p.x(),horizontal?p.z():-p.y()));
  double minU=polygon.stream().mapToDouble(Vertex::u).min().orElse(0),maxU=polygon.stream().mapToDouble(Vertex::u).max().orElse(0),minV=polygon.stream().mapToDouble(Vertex::v).min().orElse(0),maxV=polygon.stream().mapToDouble(Vertex::v).max().orElse(0);
  for(int x=(int)Math.floor(minU+1e-9);x<Math.ceil(maxU-1e-9);x++)for(int y=(int)Math.floor(minV+1e-9);y<Math.ceil(maxV-1e-9);y++){
   List<Vertex> p=clip(polygon,true,x,true);p=clip(p,true,x+1,false);p=clip(p,false,y,true);p=clip(p,false,y+1,false);if(p.size()<3)continue;
   var triangles=new ArrayList<Vertex>();if(p.size()==4){for(var v:p)triangles.add(new Vertex(v.p(),Math.clamp(v.u()-x,0,1),Math.clamp(v.v()-y,0,1)));}else for(int i=1;i<p.size()-1;i++)for(var v:List.of(p.get(0),p.get(i),p.get(i+1),p.get(i+1)))triangles.add(new Vertex(v.p(),Math.clamp(v.u()-x,0,1),Math.clamp(v.v()-y,0,1)));result.add(new Tile(face,List.copyOf(triangles),nx/length,ny/length,nz/length,(p.size()-2)*4));
  }
 return List.copyOf(result);}
 private static List<Vertex> clip(List<Vertex> polygon,boolean u,double bound,boolean greater){var out=new ArrayList<Vertex>();if(polygon.isEmpty())return out;Vertex previous=polygon.getLast();boolean before=inside(previous,u,bound,greater);for(var current:polygon){boolean after=inside(current,u,bound,greater);if(before!=after){double av=u?previous.u():previous.v(),bv=u?current.u():current.v(),t=(bound-av)/(bv-av);out.add(lerp(previous,current,t));}if(after)out.add(current);previous=current;before=after;}return out;}
 private static boolean inside(Vertex p,boolean u,double edge,boolean greater){return greater?(u?p.u():p.v())>=edge:(u?p.u():p.v())<=edge;}
 private static Vertex lerp(Vertex a,Vertex b,double t){return new Vertex(new Point(a.p().x()+(b.p().x()-a.p().x())*t,a.p().y()+(b.p().y()-a.p().y())*t,a.p().z()+(b.p().z()-a.p().z())*t),a.u()+(b.u()-a.u())*t,a.v()+(b.v()-a.v())*t);}
}
