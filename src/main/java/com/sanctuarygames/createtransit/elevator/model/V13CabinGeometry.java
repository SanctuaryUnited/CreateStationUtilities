package com.sanctuarygames.createtransit.elevator.model;
import java.util.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinGeometry.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
/** Compact cabin shell and collision ported from CSU 0.2.0-dev13. */
final class V13CabinGeometry {
 static final double FRONT_PANEL=.055, REAR_PANEL_INSET=.085;
 private V13CabinGeometry(){}
 private static final Map<CabinConfiguration,List<Face>> CACHE=Collections.synchronizedMap(new LinkedHashMap<>(64,.75f,true){protected boolean removeEldestEntry(Map.Entry<CabinConfiguration,List<Face>> e){return size()>128;}});
 private static Material rail(CabinConfiguration c){return c.handrail()==Handrail.DARK?Material.DARK:c.handrail()==Handrail.ACCENT?Material.ACCENT:Material.METAL;}
 private static Material wall(CabinConfiguration c,boolean glass){return glass?Material.GLASS:c.variant()==Variant.WOOD?Material.WOOD:Material.FRAME;}
 public static List<Face> body(CabinConfiguration c){return CACHE.computeIfAbsent(c,V13CabinGeometry::build);}
 private static List<Face> build(CabinConfiguration c){
  var q=new ArrayList<Face>();double n=c.size(),t=.0625,L=left(c),R=L+width(c);
  if(c.variant()==Variant.CYLINDRICAL){
   box(q,Material.FLOOR,0,0,0,n,.125,n/2);box(q,Material.FRAME,0,3.125,0,n,3.25,n/2);
   for(var strip:voxelRear(c)){box(q,Material.FLOOR,strip.x0(),0,strip.z0(),strip.x1(),.125,strip.z1());box(q,Material.FRAME,strip.x0(),3.125,strip.z0(),strip.x1(),3.25,strip.z1());}
   for(var wall:voxelWalls(c)){box(q,wall(c,c.curvedGlass()),wall.x0(),.125,wall.z0(),wall.x1(),3.125,wall.z1());for(double h:new double[]{.2,3.02})box(q,Material.ACCENT,wall.x0(),h,wall.z0(),wall.x1(),h+.055,wall.z1());}
  }else{
   box(q,Material.FLOOR,0,0,0,n,.125,n);box(q,Material.FRAME,0,3.125,0,n,3.25,n);
   if(!c.rearEnabled())box(q,wall(c,c.rearGlass()),0,.125,n-t,n,3.125,n);
  }
  double depth=c.variant()==Variant.CYLINDRICAL?n/2:n;
  box(q,wall(c,c.leftGlazed()),0,.125,0,t,3.125,depth);box(q,wall(c,c.rightGlazed()),n-t,.125,0,n,3.125,depth);
  for(double x:new double[]{0,n-t})for(double z:new double[]{0,depth-t})box(q,Material.METAL,x,.125,z,x+t,3.125,z+t);
  // Glass walls get top and bottom rails (the corner posts above frame their ends), so they read as glazing.
  if(c.variant()!=Variant.CYLINDRICAL){
   for(double x:new double[]{0,n-t})if(x==0?c.leftGlazed():c.rightGlazed()){
    for(double y:new double[]{.125,3.035})box(q,Material.METAL,x==0?0:x-.004,y,0,x==0?t+.004:n,y+.09,depth);
   }
   if(!c.rearEnabled()&&c.rearGlass()){
    for(double y:new double[]{.125,3.035})box(q,Material.METAL,0,y,n-t-.004,n,y+.09,n);
   }
  }
  // Interior rails and floor/ceiling LED trim are visible from inside the cabin.
  for(double x:new double[]{t+.01,n-t-.055}){
   if(c.handrail()!=Handrail.NONE)box(q,rail(c),x,1.12,.18,x+.045,1.18,depth-.12);
   for(double h:new double[]{.19,3.035})box(q,Material.ACCENT,x,h,.12,x+.025,h+.035,depth-.12);
  }
  if(c.variant()!=Variant.CYLINDRICAL&&!c.rearEnabled()){
   if(c.handrail()!=Handrail.NONE)box(q,rail(c),.17,1.12,n-.17,n-.17,1.18,n-.12);
   for(double h:new double[]{.19,3.035})box(q,Material.ACCENT,.1,h,n-t-.035,n-.1,h+.035,n-t-.01);
  }
  frame(q,c,0);if(c.rearEnabled())frame(q,c,n-.5);
  box(q,Material.DARK,R+.025,1.2,.075,Math.min(n-.08,R+.16),1.65,.10);
  for(int i=0;i<3;i++)box(q,Material.LIGHT,R+.04,1.28+i*.1,.105,Math.min(n-.09,R+.13),1.31+i*.1,.11);
  int lightsStart=q.size();
  switch(c.lighting()){
   case PANEL, GRID -> box(q,Material.LIGHT,n*.24,3.105,depth*.25,n*.76,3.12,depth*.75);
   case STRIPS, LINEAR -> {for(double x:new double[]{n*.22,n*.76})box(q,Material.LIGHT,x,3.105,.22,x+.05,3.12,Math.max(.3,depth-.22));}
   case BULBS, RECESSED -> {for(double x:new double[]{n*.28,n*.72})for(double z:new double[]{depth*.3,depth*.7})box(q,Material.LIGHT,x-.055,3.09,z-.055,x+.055,3.12,z+.055);}
   case RING -> {double r=Math.min(n,depth)*.3;for(int i=0;i<32;i++){double a=i*Math.PI/16,b=(i+1)*Math.PI/16;quad(q,Material.LIGHT,n/2+(r-.035)*Math.cos(a),3.105,depth/2+(r-.035)*Math.sin(a),n/2+(r+.035)*Math.cos(a),3.105,depth/2+(r+.035)*Math.sin(a),n/2+(r+.035)*Math.cos(b),3.105,depth/2+(r+.035)*Math.sin(b),n/2+(r-.035)*Math.cos(b),3.105,depth/2+(r-.035)*Math.sin(b));}}
  }
  // Keep every ceiling fixture clear of the front/rear pocket and pressed against the ceiling.
  for(int i=lightsStart;i<q.size();i++){var f=q.get(i);q.set(i,new Face(lightPoint(f.a(),c),lightPoint(f.b(),c),lightPoint(f.c(),c),lightPoint(f.d(),c),f.material(),f.u0(),f.v0(),f.u1(),f.v1()));}
  return compactFaces(q);
 }
 public static Point lightPoint(Point p,CabinConfiguration c){double min=.58,max=c.rearEnabled()?c.size()-.58:c.size()-.12;return new Point(p.x(),Math.min(3.124,p.y()+.004),Math.clamp(p.z(),min,max));}
 public static double compactY(double y){return y<=2.125?y:2.125+(y-2.125)/3;}
 public static Point compactPoint(Point p){return new Point(p.x(),compactY(p.y()),p.z());}
 public static List<Face> compactFaces(List<Face> faces){return faces.stream().map(f->new Face(compactPoint(f.a()),compactPoint(f.b()),compactPoint(f.c()),compactPoint(f.d()),f.material(),f.u0(),f.v0(),f.u1(),f.v1())).toList();}
 private static void frame(List<Face> faces,CabinConfiguration c,double z){
  V13Doors.frame(faces,c,z,.125,.5);
 }
 public static List<Face> doors(CabinConfiguration c,double front,double rear){var q=new ArrayList<Face>();V13Doors.panels(q,c,FRONT_PANEL,.125,front,c.front());if(c.rearEnabled())V13Doors.panels(q,c,c.size()-REAR_PANEL_INSET,.125,rear,c.rear());return c.compact()?compactFaces(q):q;}
 public static List<Box> voxelRear(CabinConfiguration c){var out=new ArrayList<Box>();double n=c.size(),step=.125,r=n/2;for(double z=r;z<n-.00001;z+=step){double half=Math.floor(Math.sqrt(Math.max(0,r*r-Math.pow(z+step/2-r,2)))/step)*step;half=Math.max(step,half);out.add(new Box(r-half,0,z,r+half,3.25,Math.min(n,z+step)));}return out;}
 public static List<Box> voxelWalls(CabinConfiguration c){var out=new ArrayList<Box>();var rows=voxelRear(c);double t=.0625;if(rows.getFirst().x0()>0){out.add(new Box(0,.125,c.size()/2-t,rows.getFirst().x0(),3.125,c.size()/2));out.add(new Box(rows.getFirst().x1(),.125,c.size()/2-t,c.size(),3.125,c.size()/2));}for(int i=0;i<rows.size();i++){var a=rows.get(i);out.add(new Box(a.x0(),.125,a.z0(),a.x0()+t,3.125,a.z1()));out.add(new Box(a.x1()-t,.125,a.z0(),a.x1(),3.125,a.z1()));var next=i+1<rows.size()?rows.get(i+1):null;double left=next==null?c.size()/2:next.x0(),right=next==null?c.size()/2:next.x1();if(left>a.x0())out.add(new Box(a.x0(),.125,a.z1()-t,left,3.125,a.z1()));if(right<a.x1())out.add(new Box(right,.125,a.z1()-t,a.x1(),3.125,a.z1()));}return out;}
 public static List<Box> collision(CabinConfiguration c,double front,double rear){
  var boxes=new ArrayList<Box>();double n=c.size(),t=.0625,depth=c.variant()==Variant.CYLINDRICAL?n/2:n,l=left(c),r=l+width(c);
  boxes.add(new Box(0,0,0,n,.125,depth));boxes.add(new Box(0,3.125,0,n,3.25,depth));
  boxes.add(new Box(0,.125,0,t,3.125,depth));boxes.add(new Box(n-t,.125,0,n,3.125,depth));
  if(c.variant()==Variant.CYLINDRICAL){boxes.addAll(voxelWalls(c));for(var strip:voxelRear(c)){boxes.add(new Box(strip.x0(),0,strip.z0(),strip.x1(),.125,strip.z1()));boxes.add(new Box(strip.x0(),3.125,strip.z0(),strip.x1(),3.25,strip.z1()));}}
  else if(!c.rearEnabled())boxes.add(new Box(0,.125,n-t,n,3.125,n));
  for(double z:c.rearEnabled()?new double[]{0,n-.5}:new double[]{0}){boxes.add(new Box(0,.125,z,l,3.125,z+.5));boxes.add(new Box(r,.125,z,n,3.125,z+.5));boxes.add(new Box(l,2.125,z,r,3.125,z+.5));for(var panel:V13Doors.panelBoxes(c,z==0?FRONT_PANEL:n-REAR_PANEL_INSET,.125,z==0?front:rear))boxes.add(panel);}
  return boxes.stream().map(b->new Box(b.x0(),compactY(b.y0()),b.z0(),b.x1(),compactY(b.y1()),b.z1())).toList();
 }
 private static final class V13Doors {
  private static int rows(CabinConfiguration c){return 3;}
  private static double frameStart(CabinConfiguration c){return 0;}
  private static double frameEnd(CabinConfiguration c){return c.size();}
  private static void frame(List<Face> q,CabinConfiguration c,double z,double y,double depth){
   indicatorHousing(q,c,z,y,depth);
   waveFrame(q,c,z,y,depth);
  }
  private static void panels(List<Face> q,CabinConfiguration c,double z,double y,double progress,Door door){
   wavePanels(q,c,z,y,DoorGeometry.eased(progress),door);
  }
  private static List<Box> panelBoxes(CabinConfiguration c,double z,double y,double progress){
   return panelBoxesAtTravel(c,z,y,DoorGeometry.eased(progress));
  }
  private static List<Box> panelBoxesAtTravel(CabinConfiguration c,double z,double y,double travel){
   var boxes=new ArrayList<Box>();
   double leaf=width(c)/2;
   for(int index=0;index<2;index++){
    double x=left(c)+index*leaf+(index==0?-1:1)*travel*leaf;
    double min=Math.max(0,x),max=Math.min(c.size(),x+leaf-.008);
    if(max>min)boxes.add(new Box(min,y+.005,z,max,y+panelTop(c),z+.025));
   }
   return boxes;
  }
 private static void header(List<Face> q,CabinConfiguration c,Material m,double z,double y,double depth){
  double l=left(c),r=l+width(c),bottom=c.kit3()?2.35:1.89,top=c.kit3()?2.65:1.985,start=c.kit3()?2:1.875;
  double border=switch(c.indicatorStyle()){case SLIMLINE->.006;case CLASSIC_LED->.025;case RECESSED_GLASS->.014;default->.010;};
  box(q,Material.METAL,l,y+bottom-border,z+.001,r,y+bottom,z+Math.max(.015,depth-.045));
  box(q,Material.METAL,l,y+top,z+.001,r,y+Math.min(rows(c),top+border),z+Math.max(.015,depth-.045));
  // Full-width black insert at the frame surface, never hidden behind a solid bridge.
  box(q,m,l,y+start,z+.008,r,y+bottom,z+depth-.06);
  box(q,m,l,y+top,z+.008,r,y+rows(c),z+depth-.06);
  box(q,Material.DOOR_DISPLAY,l,y+bottom,z+.008,r,y+top,z+Math.max(.012,depth-.055));
  if(c.indicatorStyle()==IndicatorStyle.RECESSED_GLASS)box(q,Material.GLASS,l,y+bottom,z+Math.max(.014,depth-.052),r,y+top,z+Math.max(.016,depth-.0505));

 }
 public static void indicatorHousing(List<Face> q,CabinConfiguration c,double z,double y,double depth){if(!c.indicators()||depth<.15)return;double l=left(c),r=l+width(c);for(boolean right:new boolean[]{false,true}){double x=right?r:l-.018;box(q,Material.DOOR_DISPLAY,x,y+1.24,z+.08,x+.018,y+1.70,z+depth-.08);if(c.indicatorStyle()==IndicatorStyle.CLASSIC_LED)box(q,Material.METAL,x-.003,y+1.22,z+.06,x+.021,y+1.24,z+depth-.06);}}
 public static double panelTop(CabinConfiguration c){return c.kit3()?1.995:1.87;}
 private static void waveFrame(List<Face> q,CabinConfiguration c,double z,double y,double depth){
  double l=left(c),r=l+width(c),n=c.size(),t=Math.max(.025,depth-.06);
  box(q,Material.DOOR_FRAME,frameStart(c),y,z+.008,l,y+rows(c),z+t);
  box(q,Material.DOOR_FRAME,r,y,z+.008,frameEnd(c),y+rows(c),z+t);
  header(q,c,Material.DOOR_FRAME,z,y,depth);
  for(double face:new double[]{z+.001,z+t+.001}){
   box(q,Material.DOOR_GASKET,l-.07,y+.025,face,l,y+1.875,face+.005);
   box(q,Material.DOOR_GASKET,r,y+.025,face,r+.07,y+1.875,face+.005);
   box(q,Material.DOOR_GASKET,l,y+1.84,face,r,y+1.875,face+.005);

   for(double x:new double[]{l-.052,r+.018})
    box(q,Material.DOOR_LED,x,y+.085,face-.0005,x+.032,y+1.82,face+.008);
   if(c.doorStyle()==DoorStyle.WOOD_TRIM){
    for(double[] span:new double[][]{{frameStart(c)+.04,l-.10},{r+.10,frameEnd(c)-.04}}){
     box(q,Material.DOOR_GASKET,span[0],y+.04,face,span[1],y+1.84,face+.006);
     box(q,Material.DOOR_WOOD,span[0]+.012,y+.055,face-.0002,span[1]-.012,y+1.825,face+.008);
    }
    for(double[] span:new double[][]{{frameStart(c)+.04,l-.04},{r+.04,frameEnd(c)-.04}})
     box(q,Material.DOOR_WOOD,span[0],y+1.903,face-.0002,span[1],y+1.97,face+.008);
   }
   if(c.doorStyle()==DoorStyle.INDUSTRIAL){
    for(double x:new double[]{l-.115,r+.005})
     box(q,Material.DOOR_SAFETY,x,y+.08,face-.0002,x+.11,y+.40,face+.009);
    for(double x:new double[]{l-.14,r+.075})for(double h:new double[]{.035,1.92})
     box(q,Material.DOOR_TRIM,x,y+h,face-.0003,x+.04,y+h+.035,face+.01);
   }
  }
 }
 private static void wavePanels(List<Face> q,CabinConfiguration c,double z,double y,double p,Door door){
  var style=c.doorStyle();
  boolean full=door==Door.GLASS;
  boolean slit=style==DoorStyle.SLIM_WINDOW||style==DoorStyle.INDUSTRIAL;
  int i=0;for(var panel:panelBoxesAtTravel(c,z,y,p)){
   if(style==DoorStyle.CLASSIC_BRUSHED_STEEL||style==DoorStyle.OG_STEEL||style==DoorStyle.CLASSIC_WOOD){
    DoorGeometry.customLeaf(q,c,panel,door);continue;
   }
   double x=panel.x0(),X=panel.x1(),w=X-x+.008;double originalZ=z;z=panel.z0();int start=q.size();
   if(full||slit){
    double inset=slit?w*.34:Math.min(.09,w/5),a=x+inset,b=X-inset;
    double bottom=y+(style==DoorStyle.INDUSTRIAL?.85:slit?.45:.16),top=y+(style==DoorStyle.INDUSTRIAL?1.58:slit?1.64:1.81);
    box(q,Material.COPY_PRIMARY,x,y+.005,z,a,y+panelTop(c),z+.025);
    box(q,Material.COPY_PRIMARY,b,y+.005,z,X,y+panelTop(c),z+.025);
    box(q,Material.COPY_PRIMARY,a,y+.005,z,b,bottom,z+.025);
    box(q,Material.COPY_PRIMARY,a,top,z,b,y+panelTop(c),z+.025);
    // Four glass boxes total for divided glass: two per moving leaf, separated by ONE bar.
    if(style==DoorStyle.DIVIDED_GLASS){
     double middle=y+.935;
     box(q,Material.DOOR_GLASS,a,bottom,z+.008,b,middle-.018,z+.018);
     box(q,Material.DOOR_GLASS,a,middle+.018,z+.008,b,top,z+.018);
     box(q,Material.DOOR_TRIM,a,middle-.018,z-.003,b,middle+.018,z+.028);
    }else box(q,Material.DOOR_GLASS,a,bottom,z+.008,b,top,z+.018);
    if(slit){
     box(q,Material.DOOR_TRIM,a-.018,bottom-.018,z-.003,a,top+.018,z+.028);
     box(q,Material.DOOR_TRIM,b,bottom-.018,z-.003,b+.018,top+.018,z+.028);
     box(q,Material.DOOR_TRIM,a,bottom-.018,z-.003,b,bottom,z+.028);
     box(q,Material.DOOR_TRIM,a,top,z-.003,b,top+.018,z+.028);
    }
    for(double face:new double[]{z-.002,z+.026}){
     double trim=Math.min(.018,(b-a)/6);
     box(q,Material.DOOR_GASKET,a,bottom,face,b, bottom+.018,face+.002);
     box(q,Material.DOOR_GASKET,a,top-.018,face,b,top,face+.002);
     box(q,Material.DOOR_GASKET,a,bottom,face,a+trim,top,face+.002);
     box(q,Material.DOOR_GASKET,b-trim,bottom,face,b,top,face+.002);
    }
   }else box(q,Material.COPY_PRIMARY,x,y+.005,z,X,y+panelTop(c),z+.025);
   for(double face:new double[]{z-.003,z+.027}){
    for(double edge:new double[]{x,X-.02})box(q,Material.DOOR_TRIM,edge,y+.005,face,edge+.02,y+panelTop(c),face+.003);
    if(style==DoorStyle.WOOD_TRIM){
     double a=x+w*.24,b=x+w*.76;
     box(q,Material.DOOR_GASKET,a-.018,y+.20,face,b+.018,y+1.67,face+.004);
     box(q,Material.DOOR_TRIM,a-.012,y+.206,face-.0003,b+.012,y+1.664,face+.006);
     box(q,Material.DOOR_WOOD,a,y+.22,face-.0006,b,y+1.65,face+.008);
    }
    if(style==DoorStyle.INDUSTRIAL){
     double edge=i==0?X-.028:x+.02;
     box(q,Material.DOOR_YELLOW,edge,y+.005,face-.0002,edge+.008,y+panelTop(c),face+.004);
    }
   }
   // Crop each metal infill from the same complete-leaf tile. Cutouts no longer stretch it.
   for(int j=start;j<q.size();j++){
    var f=q.get(j);if(f.material()!=Material.COPY_PRIMARY||Math.abs(f.a().z()-f.c().z())>1e-8)continue;
    q.set(j,new Face(f.a(),f.b(),f.c(),f.d(),f.material(),(f.a().x()-x)/(X-x),1-(f.b().y()-y-.005)/(panelTop(c)-.005),(f.c().x()-x)/(X-x),1-(f.a().y()-y-.005)/(panelTop(c)-.005)));
   }
   z=originalZ;i++;
  }
 }
 }
}
