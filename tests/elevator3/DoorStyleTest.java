import java.util.*;import com.sanctuarygames.createtransit.elevator.model.*;import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
public class DoorStyleTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){int cases=0,poses=0;
  check(DOOR_MODELS.length==3&&CEILING_LAYOUTS.length==5,"Exactly three door styles and five current ceiling layouts");
  for(var skin:APPROVED_SKINS)for(var style:DOOR_MODELS)for(var glazing:List.of(Glazing.SOLID,Glazing.FULL_GLASS,Glazing.SLIM_GLASS))for(int size=2;size<=5;size++)for(var mechanism:Mechanism.values())for(boolean two:new boolean[]{false,true}){
   var p=CabinConfiguration.defaults(size,Variant.SOLID).save();p.setProperty("skin",skin.name());p.setProperty("doorStyle",style.name());p.setProperty("glazing",glazing.name());p.setProperty("mechanism",mechanism.name());p.setProperty("twoSpeed",""+two);p.setProperty("compact","true");var c=CabinConfiguration.load(p);
   check(c.skin()==skin&&c.doorStyle()==style&&CabinConfiguration.load(c.save()).equals(c),"Independent skin/model settings survive schema 2 roundtrip");
   for(double progress:new double[]{0,.01,.25,.5,.75,.99,1}){
    var mesh=CabinGeometry.landing(c,progress);for(var f:mesh)for(var v:List.of(f.a(),f.b(),f.c(),f.d()))check(Double.isFinite(v.x())&&v.x()>=-1e-8&&v.x()<=size+1e-8&&v.y()>=0&&v.y()<=3&&v.z()>=1-DoorGeometry.depth(c)&&v.z()<=1,"Landing envelope: "+skin+"/"+style+"/"+size+"/"+mechanism+"/two="+two+" p="+progress+" vertex="+v);
    var boxes=DoorGeometry.panelBoxes(c,.96,0,progress);for(var b:boxes){check(b.z0()>=.96&&b.z1()<1.02,"Panels only travel sideways, with optional fixed parallel lanes");if(progress==1)check(b.x1()<=CabinGeometry.left(c)+1e-8||b.x0()>=CabinGeometry.left(c)+CabinGeometry.width(c)-1e-8,"Fully open leaves clear passage");}
    poses++;
   }
   var leaves=new ArrayList<CabinGeometry.Face>();DoorGeometry.panels(leaves,c,.055,.125,0,c.front());long glass=leaves.stream().filter(f->f.material()==CabinGeometry.Material.DOOR_GLASS).count();check((glass>0)==(style!=DoorStyle.CLASSIC_WOOD&&glazing!=Glazing.SOLID),"Solid has no glass; panoramic/slim have transparent cutouts");cases++;
  }
  int migrated=0;for(var old:DoorStyle.values())for(var glazing:Glazing.values()){
   var p=CabinConfiguration.defaults(3,Variant.SOLID).save();p.setProperty("schema","1");p.remove("skin");p.remove("ledColor");p.remove("mainLights");p.setProperty("doorStyle",old.name());p.setProperty("glazing",glazing.name());p.setProperty("wood","minecraft:cherry_planks");var c=CabinConfiguration.load(p);check((Arrays.asList(DOOR_MODELS).contains(c.doorStyle())||c.doorStyle()==DoorStyle.SOLID||c.doorStyle()==DoorStyle.GLASS),"Every legacy style maps to a retained door");check(c.wood().equals("minecraft:cherry_planks"),"Migration retains selected material");check(CabinConfiguration.load(c.save()).equals(c),"Migration is idempotent");migrated++;
  }
  for(var skin:APPROVED_SKINS)for(var layout:CEILING_LAYOUTS)for(var variant:new Variant[]{Variant.SOLID,Variant.REAR_PANORAMIC,Variant.SCENIC,Variant.CYLINDRICAL})for(int size=2;size<=5;size++){
   var p=CabinConfiguration.defaults(size,variant).save();p.setProperty("skin",skin.name());p.setProperty("lighting",layout.name());p.setProperty("compact","true");var c=CabinConfiguration.load(p);check(CabinGeometry.body(c).stream().anyMatch(f->f.material()==CabinGeometry.Material.LIGHT&&f.a().y()>2.2),"Every size/model/skin supports every ceiling layout");
   p.setProperty("mainLights","false");check(CabinGeometry.body(CabinConfiguration.load(p)).stream().anyMatch(f->f.material()==CabinGeometry.Material.LIGHT&&f.a().y()>2.2),"Primary ceiling lights remain enabled");
  }
  check(IndicatorSignal.direction(1,0,100)==1&&IndicatorSignal.direction(-1,0,100)==-1&&IndicatorSignal.direction(0,1,100)==0,"Motion direction and gray idle");
  check(IndicatorSignal.direction(0,1,2)==1&&IndicatorSignal.direction(0,1,12)==0&&IndicatorSignal.direction(0,-1,2)==-1&&IndicatorSignal.direction(0,-1,8)==0&&IndicatorSignal.direction(0,-1,14)==-1&&IndicatorSignal.direction(0,-1,22)==0,"One up flash, two down flashes, then gray");
  System.out.println("PASS: "+cases+" skin/model/size/mechanism/speed combinations, "+poses+" poses, "+migrated+" migrations; 320 ceiling combinations/on-off, finite geometry, slab envelopes, clear passages and arrival pulses.");
 }
}
