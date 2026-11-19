import com.sanctuarygames.createtransit.elevator.model.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
import java.nio.file.*;import java.util.*;
/** Export from production geometry, never from a separately authored preview model. */
public final class ExportCabinAssets {
 static String quote(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
 static void faces(StringBuilder s,List<CabinGeometry.Face> faces){s.append('[');boolean first=true;for(var f:faces){if(!first)s.append(',');first=false;s.append('[').append(f.material().ordinal());for(var p:List.of(f.a(),f.b(),f.c(),f.d()))s.append(',').append(p.x()).append(',').append(p.y()).append(',').append(p.z());s.append(']');}s.append(']');}
 static void write(Path dir,String key,CabinConfiguration c,boolean showcase)throws Exception{
  var s=new StringBuilder("{\"key\":").append(quote(key)).append(",\"settings\":{");boolean first=true;var p=c.save();for(var k:new TreeSet<>(p.stringPropertyNames())){if(!first)s.append(',');first=false;s.append(quote(k)).append(':').append(quote(p.getProperty(k)));}s.append("},\"materials\":[");for(int i=0;i<CabinGeometry.Material.values().length;i++){if(i>0)s.append(',');s.append(quote(CabinGeometry.Material.values()[i].name()));}s.append("],\"body\":");faces(s,CabinGeometry.body(c));s.append(",\"doors\":");faces(s,CabinGeometry.doors(c,showcase?1:0,showcase?1:0));s.append('}');Files.writeString(dir.resolve(key+".json"),s);
 }
 static CabinConfiguration config(int size,Variant v,Door front,boolean rear,Door back,boolean left,boolean right,boolean curved){return new CabinConfiguration(size,v,0,3,front,rear,back,left,right,curved,"minecraft:spruce_planks","minecraft:smooth_stone",Lighting.PANEL,Chime.OTIS);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[0]);Files.createDirectories(root.resolve("models"));Files.createDirectories(root.resolve("catalog"));Files.createDirectories(root.resolve("finishes"));int count=0;
  for(int size=2;size<=5;size++)for(var v:Variant.values()){
   write(root.resolve("models"),size+"x"+size+"_"+v.name().toLowerCase(Locale.ROOT),config(size,v,Door.GLASS,false,Door.SOLID,false,false,v==Variant.CYLINDRICAL),true);
   for(boolean rear:List.of(false,true)){if(rear&&v==Variant.CYLINDRICAL)continue;for(var front:Door.values())for(var back:Door.values()){
    if(!rear&&back==Door.GLASS)continue;
    for(int windows=0;windows<(v==Variant.REAR_PANORAMIC?4:1);windows++)for(int curved=0;curved<(v==Variant.CYLINDRICAL?2:1);curved++){
     String key=String.format(Locale.ROOT,"%03d_%dx%d_%s_F%s_R%s_W%d_C%d",++count,size,size,v,front,rear?back:"NONE",windows,curved);
     write(root.resolve("catalog"),key,config(size,v,front,rear,back,(windows&1)!=0,(windows&2)!=0,curved==1),false);
    }
   }}
  }
  for(var light:Lighting.values()){var p=config(3,Variant.SOLID,Door.GLASS,false,Door.SOLID,false,false,false).save();p.setProperty("lighting",light.name());write(root.resolve("finishes"),"LIGHT_"+light,CabinConfiguration.load(p),true);}
  for(var rail:Handrail.values()){var p=config(3,Variant.SCENIC,Door.GLASS,false,Door.SOLID,false,false,false).save();p.setProperty("handrail",rail.name());write(root.resolve("finishes"),"RAIL_"+rail,CabinConfiguration.load(p),true);}
  for(String wood:List.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","bamboo","crimson","warped")){var p=config(3,Variant.WOOD,Door.GLASS,false,Door.SOLID,false,false,false).save();p.setProperty("wood","minecraft:"+wood+"_planks");write(root.resolve("finishes"),"WOOD_"+wood,CabinConfiguration.load(p),true);}
  if(count!=184)throw new AssertionError("Expected 184 distinct structural/door/glazing combinations, got "+count);
  System.out.println("Exported 20 primary cabin meshes, 184 distinct door/glazing combinations, and 19 lighting/rail/wood finish examples from production geometry.");
 }
}
