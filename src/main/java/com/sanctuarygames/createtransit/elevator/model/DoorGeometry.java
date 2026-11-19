package com.sanctuarygames.createtransit.elevator.model;
import java.util.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinGeometry.*;
/** Landing frame stays in z=[.5,1]. Cabin pockets are constructed inside the inset body. */
public final class DoorGeometry {
 private DoorGeometry(){}
 public static double frameStart(CabinConfiguration c){return 0;}
 public static double frameEnd(CabinConfiguration c){return c.size();}
 public static int rows(CabinConfiguration c){return c.size()>=3?3:2;}
 public static double displayY(CabinConfiguration c){return c.size()>=3?2.60:1.97;}
 public static final double LANDING_PANEL_START=1-PANEL_DEPTH-SEAM_CLEARANCE;
 public static double depth(CabinConfiguration c){
  return switch(c.frameType()){case FLUSH->1;case RECESSED->.75;default->.5;};
 }
 public static double indicatorX(CabinConfiguration c,boolean right){return right?left(c)+width(c)-.2:left(c)+.2;}
 public static double indicatorDepth(double depth){return Math.max(.025,depth-.055);}
 public static double panelTop(CabinConfiguration c){return c.size()>=3?2:1.875;}
 public static boolean telescoping(CabinConfiguration c){return false;}
 public static double eased(double p){p=Math.clamp(p,0,1);return p*p*(3-2*p);}
 private static List<Box> leaves(CabinConfiguration c,double z,double y,double progress,double lo,double hi,boolean cabin){var q=new ArrayList<Box>();double w=width(c)/2,p=eased(progress),top=cabin?2:panelTop(c);
  for(int i=0;i<2;i++){double x=left(c)+i*w+(i==0?-1:1)*p*w,X=x+w;x=Math.max(lo,x);X=Math.min(hi,X);if(X>x)q.add(new Box(x,y,z,X,y+top,z+PANEL_DEPTH));}return List.copyOf(q);}
 public static List<Box> panelBoxes(CabinConfiguration c,double z,double y,double p){return leaves(c,z,y,p,left(c),left(c)+width(c),false);}
 public static List<Box> cabinPanelBoxes(CabinConfiguration c,double z,double y,double p){return c.legacyGeometry()?leaves(c,z,y,p,INSET,c.size()-INSET,true):leaves(c,z,y,p,left(c),left(c)+width(c),true);}
 public static void frame(List<Face> q,CabinConfiguration c,double z,double y,double depth){double l=left(c),r=l+width(c),top=rows(c),passage=panelTop(c);
  box(q,Material.DOOR_FRAME,0,y,z,l,y+top,z+depth);box(q,Material.DOOR_FRAME,r,y,z,c.size(),y+top,z+depth);
  box(q,Material.DOOR_FRAME,l,y+passage,z,r,y+top,z+depth);
  double stripHeight=passage-.08;
  {
   double near=z+.035;
   box(q,Material.DOOR_LED,l-.018,y+.07,near,l+.006,y+stripHeight,near+.024);
   box(q,Material.DOOR_LED,r-.006,y+.07,near,r+.018,y+stripHeight,near+.024);
  }
 }
 public static void indicatorHousing(List<Face> q,CabinConfiguration c,double z,double y,double depth){}
 private static void panels(List<Face> q,CabinConfiguration c,List<Box> leaves,Door door){
  boolean custom=c.doorStyle()==DoorStyle.CLASSIC_BRUSHED_STEEL||c.doorStyle()==DoorStyle.OG_STEEL||c.doorStyle()==DoorStyle.CLASSIC_WOOD;
  for(var b:leaves){
   if(custom){customLeaf(q,c,b,door);continue;}
   double x=b.x0(),X=b.x1(),y=b.y0(),Y=b.y1(),z=b.z0(),Z=b.z1(),t=Math.min(.03125,(X-x)/4);
   if(door==Door.SOLID)box(q,Material.COPY_PRIMARY,x,y,z,X,Y,Z);
   else{
    box(q,Material.DOOR_TRIM,x,y,z,x+t,Y,Z);box(q,Material.DOOR_TRIM,X-t,y,z,X,Y,Z);
    box(q,Material.DOOR_TRIM,x+t,y,z,X-t,y+.04,Z);box(q,Material.DOOR_TRIM,x+t,Y-.04,z,X-t,Y,Z);
    box(q,Material.DOOR_GLASS,x+t,y+.04,z+.008,X-t,Y-.04,Z-.008);
   }
  }
 }
 static void customLeaf(List<Face> q,CabinConfiguration c,Box b,Door door){
  double x=b.x0()+.0006,X=b.x1()-.0006,y=b.y0(),Y=b.y1(),z=b.z0(),Z=b.z1();
  if(X<=x)return;
  if(c.doorStyle()==DoorStyle.CLASSIC_WOOD){
   box(q,Material.DOOR_WOOD,x,y,z+.003,X,Y,Z-.003);
   double margin=(X-x)*.13,edge=Math.min(.025,margin*.55);
   for(double face:new double[]{z,Z-.003}){
    for(double a:new double[]{x+margin,X-margin-edge})box(q,Material.WOOD,a,y+.16,face,a+edge,Y-.16,face+.003);
    for(double h:new double[]{y+.16,Y-.16-edge})box(q,Material.WOOD,x+margin+edge,h,face,X-margin-edge,h+edge,face+.003);
   }
   return;
  }
  if(door==Door.SOLID){box(q,Material.COPY_PRIMARY,x,y,z,X,Y,Z);return;}
  boolean slim=c.glazing()==Glazing.SLIM_GLASS;
  double inset=slim?(X-x)*.34:Math.min(.03125,(X-x)/4);
  double low=y+(slim?.37:.04),high=Y-(slim?.32:.04);
  int start=q.size();
  Material metal=c.doorStyle()==DoorStyle.OG_STEEL&&!slim?Material.DOOR_TRIM:Material.COPY_PRIMARY;
  box(q,metal,x,y,z,x+inset,Y,Z);
  box(q,metal,X-inset,y,z,X,Y,Z);
  box(q,metal,x+inset,y,z,X-inset,low,Z);
  box(q,metal,x+inset,high,z,X-inset,Y,Z);
  for(int i=start;i<q.size();i++){
   var f=q.get(i);
   if(Math.abs(f.a().z()-f.c().z())>1e-8)continue;
   q.set(i,new Face(f.a(),f.b(),f.c(),f.d(),f.material(),
       (f.a().x()-x)/(X-x),1-(f.b().y()-y)/(Y-y),(f.c().x()-x)/(X-x),1-(f.a().y()-y)/(Y-y)));
  }
  box(q,Material.DOOR_GLASS,x+inset,low,z+.008,X-inset,high,Z-.008);
 }
 public static void panels(List<Face> q,CabinConfiguration c,double z,double y,double p,Door door){panels(q,c,panelBoxes(c,z,y,p),door);}
 public static void cabinPanels(List<Face> q,CabinConfiguration c,double z,double y,double p,Door door){panels(q,c,cabinPanelBoxes(c,z,y,p),door);}
 public static List<Box> collision(CabinConfiguration c,double z,double y,double depth,double progress){double l=left(c),r=l+width(c),top=rows(c);var q=new ArrayList<Box>();q.add(new Box(0,y,z,l,y+top,z+depth));q.add(new Box(r,y,z,c.size(),y+top,z+depth));q.add(new Box(l,y+panelTop(c),z,r,y+top,z+depth));q.addAll(panelBoxes(c,LANDING_PANEL_START,y,progress));return List.copyOf(q);}
}
