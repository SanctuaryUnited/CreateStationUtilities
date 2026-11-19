package com.sanctuarygames.createtransit.elevator.model;
import java.util.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
/** Exact model coordinates, in blocks. No entity scale is used to create clearance. */
public final class CabinGeometry {
 public static final double INSET=1d/16, HEIGHT=2.8, LEGACY_HEIGHT=2.5, FLOOR_TOP=.125;
 public enum Material { WALL, CEILING, BODY_ACCENT, FOAM, HANDRAIL, FRAME, ACCENT, METAL, GLASS, WOOD, FLOOR, LIGHT, DARK, COPY_PRIMARY, COPY_SECONDARY, BASE_TRIM, RAIL_MATCH, RAIL_DARK,
  DOOR_FRAME, DOOR_TRIM, DOOR_GASKET, DOOR_GLASS, DOOR_WOOD, DOOR_SAFETY, DOOR_YELLOW, DOOR_LED, DOOR_DISPLAY }
 public record Point(double x,double y,double z){}
 public record Face(Point a,Point b,Point c,Point d,Material material,double u0,double v0,double u1,double v1){public Face(Point a,Point b,Point c,Point d,Material material){this(a,b,c,d,material,0,0,1,1);}}
 public record Box(double x0,double y0,double z0,double x1,double y1,double z1){public Box{if(x1<=x0||y1<=y0||z1<=z0)throw new IllegalArgumentException("Empty collider");}}
 private record Part(Material material,Box bounds,boolean solid){}
 private static final Map<CabinConfiguration,List<Part>> PARTS=Collections.synchronizedMap(new LinkedHashMap<>(32,.75f,true){protected boolean removeEldestEntry(Map.Entry<CabinConfiguration,List<Part>> e){return size()>96;}});
 private static final Map<CabinConfiguration,List<Face>> CACHE=Collections.synchronizedMap(new LinkedHashMap<>(32,.75f,true){protected boolean removeEldestEntry(Map.Entry<CabinConfiguration,List<Face>> e){return size()>96;}});
 public static double height(CabinConfiguration c){return c.legacyGeometry()?LEGACY_HEIGHT:HEIGHT;}
 public static double ceilingBottom(CabinConfiguration c){return height(c)-.125;}
 public static final double PASSAGE_DEPTH=.5, PANEL_DEPTH=.025, SEAM_CLEARANCE=1d/512;
 public static double entranceFace(CabinConfiguration c,boolean rear){
  return rear?c.size()-(c.legacyGeometry()?INSET:0):(c.legacyGeometry()?INSET:0);
 }
 public static double passageEnd(CabinConfiguration c,boolean rear){
  return entranceFace(c,rear)+(rear?-PASSAGE_DEPTH:PASSAGE_DEPTH);
 }
 public static Box doorSensor(CabinConfiguration c,boolean rear){
  double plane=panelStart(c,rear)+PANEL_DEPTH/2;
  return new Box(left(c),FLOOR_TOP,plane-.125,left(c)+width(c),FLOOR_TOP+2,plane+.125);
 }
 public static double panelStart(CabinConfiguration c,boolean rear){
  if(c.v13Geometry())return rear?c.size()-V13CabinGeometry.REAR_PANEL_INSET:V13CabinGeometry.FRONT_PANEL;
  if(c.legacyGeometry())return rear?c.size()-INSET-.275:INSET+.25;
  return rear?c.size()-PANEL_DEPTH-SEAM_CLEARANCE:SEAM_CLEARANCE;
 }
 public static double width(CabinConfiguration c){return c.size()==2?1:2;}
 public static double left(CabinConfiguration c){return (c.size()-width(c))/2;}
 static void quad(List<Face> out,Material m,double...v){out.add(new Face(new Point(v[0],v[1],v[2]),new Point(v[3],v[4],v[5]),new Point(v[6],v[7],v[8]),new Point(v[9],v[10],v[11]),m));}
 public static void box(List<Face> q,Material m,double x,double y,double z,double X,double Y,double Z){
  if(X<=x||Y<=y||Z<=z)return;
  quad(q,m,x,y,z,x,Y,z,X,Y,z,X,y,z);quad(q,m,X,y,Z,X,Y,Z,x,Y,Z,x,y,Z);
  quad(q,m,x,y,Z,x,Y,Z,x,Y,z,x,y,z);quad(q,m,X,y,z,X,Y,z,X,Y,Z,X,y,Z);
  quad(q,m,x,Y,z,x,Y,Z,X,Y,Z,X,Y,z);quad(q,m,x,y,Z,x,y,z,X,y,z,X,y,Z);
 }
 private static void part(List<Part> p,Material m,boolean solid,double x,double y,double z,double X,double Y,double Z){if(X>x&&Y>y&&Z>z)p.add(new Part(m,new Box(x,y,z,X,Y,Z),solid));}
 private static Material rail(CabinConfiguration c){return c.handrail()==Handrail.DARK?Material.RAIL_DARK:c.handrail()==Handrail.MATCH?Material.RAIL_MATCH:Material.HANDRAIL;}
 private static void wall(List<Part> p,CabinConfiguration c,boolean glass,double x,double z,double X,double Z){
  part(p,glass?Material.GLASS:Material.WALL,true,x,FLOOR_TOP,z,X,ceilingBottom(c),Z);
  // Narrow rails are inset from glazing, with discrete metal mounting brackets.
  double dx=X-x,dz=Z-z;
  if(dx>dz){double zz=z-.10;part(p,rail(c),false,x,1.08,zz,X,1.16,zz+.0625);for(double xx:new double[]{x+.08,X-.12})part(p,Material.METAL,false,xx,1.04,zz+.03,xx+.035,1.10,z+.01);part(p,Material.BASE_TRIM,false,x,.125,z-.008,X,.1875,z);}
 }
 private static List<Part> parts(CabinConfiguration c){return PARTS.computeIfAbsent(c,CabinGeometry::buildParts);}
 private static List<Part> buildParts(CabinConfiguration c){var p=new ArrayList<Part>();double a=INSET,n=c.size(),b=n-a,t=INSET,back=c.rearEnabled()?passageEnd(c,true):b-t,front=passageEnd(c,false);
  double nose=entranceFace(c,false),tail=c.rearEnabled()?entranceFace(c,true):b;
  double roofFront=c.legacyGeometry()?nose:front;
  double roofRear=c.legacyGeometry()?b:c.rearEnabled()?back:b;
  if(c.variant()==Variant.CYLINDRICAL){
   part(p,Material.FLOOR,true,a,0,nose,b,FLOOR_TOP,n/2);part(p,Material.CEILING,true,a,ceilingBottom(c),roofFront,b,height(c),n/2);
   for(var s:voxelRear(c)){part(p,Material.FLOOR,true,s.x0(),0,s.z0(),s.x1(),FLOOR_TOP,s.z1());part(p,Material.CEILING,true,s.x0(),ceilingBottom(c),s.z0(),s.x1(),height(c),s.z1());}
   for(var w:voxelWalls(c)){
    part(p,c.curvedGlass()?Material.GLASS:Material.WALL,true,w.x0(),FLOOR_TOP,w.z0(),w.x1(),ceilingBottom(c),w.z1());
    double x=w.x0(),X=w.x1(),z=w.z0(),Z=w.z1();
    if(X-x>Z-z){double zz=z-.10;part(p,rail(c),false,x,1.08,zz,X,1.16,zz+.0625);part(p,Material.METAL,false,(x+X)/2-.018,1.04,zz+.035,(x+X)/2+.018,1.10,z+.01);}
    else {double xx=x<n/2?X+.02:x-.0825;part(p,rail(c),false,xx,1.08,z,xx+.0625,1.16,Z);part(p,Material.METAL,false,Math.min(x,xx),1.04,(z+Z)/2-.018,Math.max(X,xx+.0625),1.10,(z+Z)/2+.018);}
   }
   back=n/2;
  }else{
   part(p,Material.FLOOR,true,a,0,nose,b,FLOOR_TOP,tail);part(p,Material.CEILING,true,a,ceilingBottom(c),roofFront,b,height(c),roofRear);
   if(!c.rearEnabled())wall(p,c,c.rearGlass(),a+t,b-t,b-t,b);
  }
  part(p,c.leftGlazed()?Material.GLASS:Material.WALL,true,a,FLOOR_TOP,front,a+t,ceilingBottom(c),back);
  part(p,c.rightGlazed()?Material.GLASS:Material.WALL,true,b-t,FLOOR_TOP,front,b,ceilingBottom(c),back);
  // Side handrails meet the rear rail and stop behind the doorway pocket.
  for(boolean right:new boolean[]{false,true}){double x=right?b-t-.10:a+t+.0375;
   part(p,rail(c),false,x,1.08,front+(c.legacyGeometry()?0:PANEL_DEPTH),x+.0625,1.16,c.variant()==Variant.CYLINDRICAL?back:back-.10);
   for(double z:new double[]{front+.12,back-.14})part(p,Material.METAL,false,right?x+.04:a+t,1.04,z,right?b-t:x+.025,1.10,z+.035);
   double trim=right?b-t-.008:a+t;part(p,Material.BASE_TRIM,false,trim,.125,front,trim+.008,.1875,back);
   if(!c.legacyGeometry()&&c.variant()!=Variant.CYLINDRICAL){
    double strip=right?b-t-.02:a+t;
    for(double y:new double[]{.19,ceilingBottom(c)-.075})
     part(p,Material.ACCENT,false,strip,y,front+.04,strip+.02,y+.035,c.rearEnabled()?back-.04:back-.02);
   }
  }
  if(!c.legacyGeometry()&&!c.rearEnabled()&&c.variant()!=Variant.CYLINDRICAL)
   for(double y:new double[]{.19,ceilingBottom(c)-.075})
    part(p,Material.ACCENT,false,a+t,y,back-.02,b-t,y+.035,back);
  // Frame boxes and colliders have identical extents, including their full half-block depth.
  double l=left(c),r=l+width(c);
  for(boolean rear:c.rearEnabled()?new boolean[]{false,true}:new boolean[]{false}){double z=rear?passageEnd(c,true):nose;
   part(p,Material.DOOR_FRAME,true,a,FLOOR_TOP,z,l,height(c),z+.5);part(p,Material.DOOR_FRAME,true,r,FLOOR_TOP,z,b,height(c),z+.5);
   part(p,Material.DOOR_FRAME,true,l,2.125,z,r,height(c),z+.5);
   for(double x:new double[]{l-.04,r+.02}){
    double ledZ=c.legacyGeometry()?z+.01:rear?z-.01:z+PASSAGE_DEPTH;
    part(p,Material.DOOR_LED,false,x,.25,ledZ,x+.02,1.95,ledZ+.01);
   }
  }
  ceiling(p,c,front+.12,c.variant()==Variant.CYLINDRICAL?n/2+.32:back-.12);
  return List.copyOf(p);
 }
 private static void light(List<Part> p,CabinConfiguration c,double x,double z,double X,double Z){part(p,Material.LIGHT,false,x,ceilingBottom(c)-.015625,z,X,ceilingBottom(c),Z);}
 private static void ceiling(List<Part> p,CabinConfiguration c,double front,double back){double n=c.size(),cx=n/2,cz=(front+back)/2,half=Math.min((back-front)*.42,n*.26),x0=cx-half,x1=cx+half,z0=cz-half,z1=cz+half,w=.0625;
  switch(c.lighting()){
   case PANEL->light(p,c,x0,z0,x1,z1);
   case RECESSED,BULBS->{for(double x:new double[]{x0,x1-.22})for(double z:new double[]{z0,z1-.22})light(p,c,x,z,x+.22,z+.22);}
   case STRIPS,RING->{light(p,c,x0,z0,x1,z0+w);light(p,c,x0,z1-w,x1,z1);light(p,c,x0,z0+w,x0+w,z1-w);light(p,c,x1-w,z0+w,x1,z1-w);}
   case LINEAR->{for(double x:new double[]{cx-half,cx-w/2,cx+half-w})light(p,c,x,z0,x+w,z1);}
   case GRID->{light(p,c,cx-.13,cz-.13,cx+.13,cz+.13);for(double x:new double[]{x0,x1-.12})for(double z:new double[]{z0,z1-.12})light(p,c,x,z,x+.12,z+.12);}
  }
 }
 public static List<Face> body(CabinConfiguration c){if(c.v13Geometry())return V13CabinGeometry.body(c);return CACHE.computeIfAbsent(c,k->{var q=new ArrayList<Face>();for(var p:parts(c)){var b=p.bounds();box(q,p.material(),b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1());}if(!c.legacyGeometry()&&c.variant()==Variant.CYLINDRICAL)addCurvedLedFaces(q,c);
   return List.copyOf(q);});}
 private static void addCurvedLedFaces(List<Face> faces,CabinConfiguration c){
  var path=new ArrayList<Point>();
  var rows=voxelRear(c);
  double front=passageEnd(c,false)+.04,side=2*INSET+.012;
  double join=c.size()/2-INSET-.013;
  double inner=rows.getFirst().x0()+INSET+.012;
  path.add(new Point(side,0,front));
  path.add(new Point(side,0,join));
  path.add(new Point(inner,0,join));
  for(var row:rows){
   path.add(new Point(row.x0()+INSET+.011,0,row.z0()-INSET-.012));
   path.add(new Point(row.x0()+INSET+.011,0,row.z1()-INSET-.012));
  }
  for(var row:voxelRear(c).reversed()){
   path.add(new Point(row.x1()-INSET-.011,0,row.z1()-INSET-.012));
   path.add(new Point(row.x1()-INSET-.011,0,row.z0()-INSET-.012));
  }
  path.add(new Point(c.size()-inner,0,join));
  path.add(new Point(c.size()-side,0,join));
  path.add(new Point(c.size()-side,0,front));
  for(int i=0;i<path.size()-1;i++){
   var a=path.get(i);var b=path.get(i+1);
   if(Math.abs(a.x()-b.x())+Math.abs(a.z()-b.z())<1e-8)continue;
   for(double y:new double[]{.19,ceilingBottom(c)-.075})
    quad(faces,Material.ACCENT,a.x(),y,a.z(),a.x(),y+.035,a.z(),b.x(),y+.035,b.z(),b.x(),y,b.z());
  }
 }
 public static List<Face> doors(CabinConfiguration c,double front,double rear){if(c.v13Geometry())return V13CabinGeometry.doors(c,front,rear);var q=new ArrayList<Face>();DoorGeometry.cabinPanels(q,c,panelStart(c,false),FLOOR_TOP,front,c.front());if(c.rearEnabled())DoorGeometry.cabinPanels(q,c,panelStart(c,true),FLOOR_TOP,rear,c.rear());return List.copyOf(q);}
 public static List<Box> collision(CabinConfiguration c,double front,double rear){if(c.v13Geometry())return V13CabinGeometry.collision(c,front,rear);var q=new ArrayList<Box>();for(var p:parts(c))if(p.solid())q.add(p.bounds());q.addAll(DoorGeometry.cabinPanelBoxes(c,panelStart(c,false),FLOOR_TOP,front));if(c.rearEnabled())q.addAll(DoorGeometry.cabinPanelBoxes(c,panelStart(c,true),FLOOR_TOP,rear));return List.copyOf(q);}
 public static List<Face> landing(CabinConfiguration c,double progress){var q=new ArrayList<Face>();double depth=DoorGeometry.depth(c);DoorGeometry.frame(q,c,1-depth,0,depth);DoorGeometry.panels(q,c,DoorGeometry.LANDING_PANEL_START,0,progress,c.front());return List.copyOf(q);}
 public static List<Box> landingCollision(CabinConfiguration c,double progress){double depth=DoorGeometry.depth(c);return DoorGeometry.collision(c,1-depth,0,depth,progress);}
 public static List<Box> voxelRear(CabinConfiguration c){if(c.v13Geometry())return V13CabinGeometry.voxelRear(c);var q=new ArrayList<Box>();double n=c.size(),r=n/2-INSET,step=c.legacyGeometry()?.25:.125;for(double z=n/2;z<n-INSET-1e-9;z+=step){double Z=Math.min(n-INSET,z+step),half=Math.max(.125,Math.floor(Math.sqrt(Math.max(0,r*r-Math.pow(Z-n/2,2)))*8)/8);q.add(new Box(n/2-half,0,z,n/2+half,height(c),Z));}return List.copyOf(q);}
 public static List<Box> voxelWalls(CabinConfiguration c){var q=new ArrayList<Box>();var rows=voxelRear(c);double t=INSET,n=c.size();var first=rows.getFirst();if(first.x0()>INSET){q.add(new Box(INSET,FLOOR_TOP,n/2-t,first.x0(),ceilingBottom(c),n/2));q.add(new Box(first.x1(),FLOOR_TOP,n/2-t,n-INSET,ceilingBottom(c),n/2));}
  for(int i=0;i<rows.size();i++){var a=rows.get(i);q.add(new Box(a.x0(),FLOOR_TOP,a.z0(),a.x0()+t,ceilingBottom(c),a.z1()));q.add(new Box(a.x1()-t,FLOOR_TOP,a.z0(),a.x1(),ceilingBottom(c),a.z1()));double left=i+1<rows.size()?rows.get(i+1).x0():n/2,right=i+1<rows.size()?rows.get(i+1).x1():n/2;if(left>a.x0())q.add(new Box(a.x0(),FLOOR_TOP,a.z1()-t,left,ceilingBottom(c),a.z1()));if(right<a.x1())q.add(new Box(right,FLOOR_TOP,a.z1()-t,a.x1(),ceilingBottom(c),a.z1()));}return List.copyOf(q);}
 /** Legacy indicator layout adapter only; body geometry is never vertically scaled. */
 public static double compactY(double y){return y<=2.125?y:2.125+(y-2.125)/3;}
 public static Point compactPoint(Point p){return new Point(p.x(),compactY(p.y()),p.z());}
 public static List<Face> compactFaces(List<Face> faces){return faces.stream().map(f->new Face(compactPoint(f.a()),compactPoint(f.b()),compactPoint(f.c()),compactPoint(f.d()),f.material(),f.u0(),f.v0(),f.u1(),f.v1())).toList();}
 public static Point lightPoint(Point p,CabinConfiguration c){return p;}
}
