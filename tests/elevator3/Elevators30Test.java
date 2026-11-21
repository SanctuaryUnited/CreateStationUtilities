import com.sanctuarygames.createtransit.elevator.model.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinGeometry.*;
import java.util.*;
public class Elevators30Test {
 static int checks;
 static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static void eq(double a,double b,String message){check(Math.abs(a-b)<1e-9,message+" ("+a+" vs "+b+")");}
 static CabinConfiguration set(CabinConfiguration c,String k,String v){var p=c.save();p.setProperty(k,v);if(k.equals("lighting"))p.remove("ceilingColor");if(k.equals("front"))p.setProperty("doorStyle",v);return load(p);}
 static boolean intersects(Box a,Box b){return a.x0()<b.x1()-1e-9&&a.x1()>b.x0()+1e-9&&a.y0()<b.y1()-1e-9&&a.y1()>b.y0()+1e-9&&a.z0()<b.z1()-1e-9&&a.z1()>b.z0()+1e-9;}
 static void bounds(List<Face> faces,int size,boolean landing){for(var f:faces)for(var p:List.of(f.a(),f.b(),f.c(),f.d())){check(Double.isFinite(p.x()+p.y()+p.z()),"Finite vertex");check(p.x()>=(landing?0:INSET)-1e-9&&p.x()<=size-(landing?0:INSET)+1e-9,"Vertex width bounds "+f.material()+" "+p);check(p.z()>=(landing?.5:INSET)-1e-9&&p.z()<=(landing?1:size-INSET)+1e-9,"Vertex depth bounds "+f.material()+" "+p);check(p.y()>=0&&p.y()<=(landing?3:2.5)+1e-9,"Height bounds");}}
 public static void main(String[] args)throws Exception{
  int configurations=0;
  for(int size:new int[]{2,3,4,5})for(var model:CABIN_MODELS)for(var skin:APPROVED_SKINS)for(var front:Door.values())for(var rear:Door.values())for(boolean back:new boolean[]{false,true}){
   if(back&&model==Variant.CYLINDRICAL)continue;
   var c=set(set(defaults(size,model),"legacyGeometry","true"),"frameType","STANDARD");c=set(c,"skin",skin.name());c=set(c,"front",front.name());c=set(c,"rear",rear.name());c=set(c,"rearEnabled",""+back);
   check(load(c.save()).equals(c),"Settings round trip");check(c.rear()==rear,"Independent rear material");
   bounds(body(c),size,false);
   var points=body(c).stream().flatMap(f->List.of(f.a(),f.b(),f.c(),f.d()).stream()).toList();
   eq(points.stream().mapToDouble(Point::x).min().orElseThrow(),INSET,"Actual mesh minimum X");eq(points.stream().mapToDouble(Point::x).max().orElseThrow(),size-INSET,"Actual mesh maximum X");eq(points.stream().mapToDouble(Point::z).min().orElseThrow(),INSET,"Actual mesh minimum Z");eq(points.stream().mapToDouble(Point::z).max().orElseThrow(),size-INSET,"Actual mesh maximum Z");eq(points.stream().mapToDouble(Point::y).max().orElseThrow(),LEGACY_HEIGHT,"Actual mesh height");
   double frameLeft=left(c);check(collision(c,0,0).stream().anyMatch(b->Math.abs(b.x0()-INSET)<1e-9&&Math.abs(b.x1()-frameLeft)<1e-9&&Math.abs(b.z1()-b.z0()-.5)<1e-9),"Actual cabin jamb has half-block depth");for(double progress:new double[]{0,.25,.5,.75,1}){bounds(doors(c,progress,progress),size,false);bounds(landing(c,progress),size,true);}
   for(var box:collision(c,1,1)){check(box.x0()>=INSET&&box.x1()<=size-INSET&&box.z0()>=INSET&&box.z1()<=size-INSET&&box.y0()>=0&&box.y1()<=LEGACY_HEIGHT,"Collision stays inside body");}
   // At full opening a passenger occupying the exact clear aperture intersects no jamb/door.
   var corridor=new Box(left(c),FLOOR_TOP,INSET,left(c)+width(c),2.125,INSET+.5);
   for(var box:collision(c,1,1))check(!intersects(corridor,box),"Open cabin passage clear");
   var corridorLanding=new Box(left(c),0,.5,left(c)+width(c),DoorGeometry.panelTop(c),1);
   for(var box:landingCollision(c,1))check(!intersects(corridorLanding,box),"Open landing passage clear");
   check(!collision(c,0,0).equals(collision(c,1,1)),"Door collisions animate");
   if(size==3){eq((size-2*INSET)*16,46,"46px exterior");eq(LEGACY_HEIGHT*16,40,"40px height");eq(width(c)*16,32,"32px aperture");eq((left(c)-INSET)*16,7,"7px cabin jamb");eq(left(c)*16,8,"8px landing jamb");eq(DoorGeometry.depth(c)*16,8,"8px landing depth");}
   check(body(c).stream().anyMatch(f->f.material()==Material.RAIL_MATCH),"Mandatory handrails default");
   check(!back||!c.rearGlass(),"Rear glass replaced by frame");configurations++;
  }
  var c=defaults(3,Variant.SOLID);
  var lightMeshes=new HashSet<List<Face>>();for(var style:CEILING_LAYOUTS){var lit=set(c,"lighting",style.name());lightMeshes.add(body(lit).stream().filter(f->f.material()==Material.LIGHT).toList());}check(lightMeshes.size()==5,"Five geometrically distinct ceiling layouts");
  for(var style:CEILING_LAYOUTS){c=set(c,"lighting",style.name());for(int dye=0;dye<16;dye++){c=set(c,"ceilingColor",""+dye);eq(c.ceilingColor(),dye,"Selected ceiling color");check(load(c.save()).equals(c),"Color serialized");}c=set(c,"ceilingColor",""+Arrays.asList(CEILING_LAYOUTS).indexOf(style));}
  for(var style:CEILING_LAYOUTS){c=set(c,"lighting",style.name());eq(c.ceilingColor(),Arrays.asList(CEILING_LAYOUTS).indexOf(style),"Per-style color restored");}
  for(int brightness=0;brightness<16;brightness++){c=set(c,"brightness",""+brightness);eq(load(c.save()).brightness(),brightness,"Brightness persistence");}
  for(var finish:HANDRAIL_FINISHES){c=set(c,"handrail",finish.name());check(load(c.save()).handrail()==finish,"Handrail persistence");}
  for(String retired:new String[]{"WOOD_TRIM","LIGHT_TRANSIT","unknown"})check(set(c,"skin",retired).skin()==Skin.MODERN_DARK,"Retired skin fallback");
  check(set(c,"handrail","NONE").handrail()==Handrail.MATCH,"Cannot disable rails");check(set(c,"mainLights","false").mainLights(),"Ceiling always enabled");
  c=set(c,"ledColor","14");eq(c.ledColor(),14,"Door LED separate");eq(c.ceilingColor(),4,"Ceiling color unchanged");
  check(!defaults(3,Variant.SOLID).legacyGeometry(),"New 3×3 cabin uses V17");
  check(defaults(3,Variant.SOLID).skin()==Skin.BRUSHED_STEEL&&defaults(3,Variant.SOLID).brightness()==6,"New finish and brightness");
  eq(DoorGeometry.LANDING_PANEL_START+PANEL_DEPTH,1-SEAM_CLEARANCE,"Landing closes on its own side of the seam");
  for(int size=2;size<=5;size++)for(boolean legacy:new boolean[]{false,true}){
   var sc=set(set(defaults(size,Variant.SOLID),"legacyGeometry",""+legacy),"v13Geometry",""+legacy);
   double half=CabinSelectorLayout.width(sc)/2,mid=CabinSelectorLayout.center(sc,false);
   check(mid-half>=CabinSelectorLayout.wallFace(sc)+1e-9,"Destination board clears the side wall at size "+size);
   check(mid+half<=CabinSelectorLayout.ledEdge(sc)-1e-9,"Destination board clears the door LED at size "+size);
   check(half*2>=4/16.0,"Destination board stays readable at size "+size);
   if(size>=4)eq(half*2,CabinSelectorLayout.WIDTH,"Wide cabins keep the full board");
   eq(CabinSelectorLayout.center(sc,true),size-mid,"Rear board mirrors the front");
  }
  eq(DoorIndicatorLayout.cabinJambBase(),FLOOR_TOP,"Cabin jamb arrows sit at the same height as landing arrows");
  for(int size=2;size<=5;size++){var ic=defaults(size,Variant.SOLID);for(boolean right:new boolean[]{false,true}){
   var cabinMount=DoorIndicatorLayout.mount(ic,0,PASSAGE_DEPTH,right);var landingMount=DoorIndicatorLayout.mount(ic,1-DoorGeometry.depth(ic),DoorGeometry.depth(ic),right);
   eq(cabinMount.x(),landingMount.x(),"Cabin and landing arrows share a jamb line");eq(cabinMount.width(),landingMount.width(),"Cabin and landing arrows are the same size");}}
  check(CabinSelectorLayout.pages(6)==1&&CabinSelectorLayout.pages(7)==2,"Six stops per selector page");
  check(CabinSelectorLayout.arrowAt(5,62)==1&&CabinSelectorLayout.arrowAt(27,62)==-1,"Footer arrows advance and rewind");
  var old=defaults(3,Variant.SOLID).save();
  old.setProperty("schema","3");old.remove("legacyGeometry");
  check(load(old).legacyGeometry(),"Old saves retain V16 geometry until upgraded");
  for(var variant:CABIN_MODELS)for(boolean rear:new boolean[]{false,true}){
   if(rear&&variant==Variant.CYLINDRICAL)continue;
   var modern=set(defaults(3,variant),"rearEnabled",Boolean.toString(rear));
   check(!modern.legacyGeometry()&&load(modern.save()).equals(modern),"V17 profile persists");
   double l=left(modern),r=l+width(modern);
   var open=collision(modern,1,1);
   var closed=collision(modern,0,0);
   var front=new Box(l,FLOOR_TOP,0,r,FLOOR_TOP+2,PASSAGE_DEPTH);
   check(open.stream().noneMatch(box->intersects(box,front)),"2×2 open front passage");
   check(open.stream().anyMatch(box->box.z0()==0&&box.z1()==.5&&box.x0()==INSET&&box.x1()==l),"7 px front jamb");
   check(open.stream().anyMatch(box->box.y1()==FLOOR_TOP&&box.z0()==0),"Front sill reaches grid plane");
   check(closed.stream().anyMatch(box->box.z0()==SEAM_CLEARANCE&&box.x0()==l&&box.y0()==FLOOR_TOP),"Front door follows the seam");
   eq(open.stream().mapToDouble(Box::y1).max().orElseThrow(),2.8,"V17 overall height");
   if(rear){
    var passage=new Box(l,FLOOR_TOP,2.5,r,FLOOR_TOP+2,3);
    check(open.stream().noneMatch(box->intersects(box,passage)),"2×2 open rear passage");
    check(open.stream().anyMatch(box->box.z0()==2.5&&box.z1()==3&&box.x0()==INSET&&box.x1()==l),"Rear frame reaches grid plane");
    check(closed.stream().anyMatch(box->Math.abs(box.z1()-(3-SEAM_CLEARANCE))<1e-9&&box.x0()==l&&box.y0()==FLOOR_TOP),"Rear door follows the seam");
   }else eq(open.stream().mapToDouble(Box::z1).max().orElseThrow(),3-INSET,"Rear shell inset");
   var reverted=set(modern,"legacyGeometry","true");
   eq(panelStart(reverted,false),5/16.0,"Saved V16 profile retains its panel");
   var v13=set(reverted,"v13Geometry","true");
   check(v13.v13Geometry()&&load(v13.save()).equals(v13),"V13 rollback round-trips");
   eq(panelStart(v13,false),.055,"V13 front door plane restored");
   check(!body(v13).equals(body(reverted)),"V13 shell differs from the V16 shell");
   check(!collision(v13,0,0).equals(collision(reverted,0,0)),"V13 colliders are restored");
   if(rear)eq(panelStart(v13,true),2.915,"V13 rear door plane restored");
   check(set(reverted,"legacyGeometry","false").equals(modern),"Reapplying preserves the rest of the configuration");
  }
  for(int index=0;index<CabinSelectorLayout.MAX_TILES;index++)
   check(CabinSelectorLayout.tileAt(CabinSelectorLayout.tileX(index)+6,CabinSelectorLayout.tileY(index)+5,6)==index,"Touchscreen tile bounds");
  check(CabinSelectorLayout.tileAt(16,27,6)==CabinSelectorLayout.NO_TILE,"Touchscreen gap does not select a floor");
  System.out.println("PASS: "+configurations+" cabin configurations, five door animation poses, 80 style/dye combinations, independent ceiling memory, 16 brightness levels, handrail and skin migration; "+checks+" assertions.");
 }
}
