import com.sanctuarygames.createtransit.elevator.model.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinGeometry.*;
import java.nio.file.*;import java.util.*;
/** Export the production faces for offline inspection; runtime uses the Java mesh directly. */
public class ExportElevators30 {
 static String point(Point p){return "["+p.x()+","+p.y()+","+p.z()+"]";}
 static String faces(List<Face> faces){var out=new StringJoiner(",","[","]");for(var f:faces)out.add("{\"material\":\""+f.material()+"\",\"vertices\":["+point(f.a())+","+point(f.b())+","+point(f.c())+","+point(f.d())+"]}");return out.toString();}
 public static void main(String[] args)throws Exception{var dest=Path.of(args[0]);Files.createDirectories(dest);int count=0;for(int size:new int[]{2,3})for(var model:CABIN_MODELS)for(var skin:APPROVED_SKINS){var p=defaults(size,model).save();p.setProperty("skin",skin.name());var c=load(p);var json="{\"format\":\"createtransit:inspection_mesh_v1\",\"units\":\"blocks\",\"runtime\":\"CabinGeometry.java\",\"size\":"+size+",\"model\":\""+model+"\",\"skin\":\""+skin+"\",\"body\":"+faces(body(c))+",\"doors_closed\":"+faces(doors(c,0,0))+",\"doors_open\":"+faces(doors(c,1,1))+",\"landing_closed\":"+faces(landing(c,0))+",\"landing_open\":"+faces(landing(c,1))+"}\n";Files.writeString(dest.resolve(size+"x"+size+"_"+model.name().toLowerCase()+"_"+skin.name().toLowerCase()+".json"),json);count++;}System.out.println("Exported "+count+" exact production inspection meshes");}
}
