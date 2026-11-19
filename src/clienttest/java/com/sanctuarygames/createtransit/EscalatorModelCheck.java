package com.sanctuarygames.createtransit;
import com.google.gson.*;
import com.sanctuarygames.createtransit.lrt.client.LrtModelLoader;
import com.sanctuarygames.createtransit.escalator.EscalatorGeometry;
import com.sanctuarygames.createtransit.escalator.EscalatorVisuals;
import com.sanctuarygames.createtransit.escalator.EscalatorBlock;
import com.sanctuarygames.createtransit.escalator.client.EscalatorRenderer;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import java.nio.file.*;
import java.util.*;
public final class EscalatorModelCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  var loader=new LrtModelLoader();int models=0,quads=0;
  try(var paths=Files.list(Path.of(args[0]))){for(var p:paths.filter(p->p.getFileName().toString().startsWith("escalator_")).toList()){
   var json=JsonParser.parseString(Files.readString(p)).getAsJsonObject();var textures=new HashMap<String,String>();json.getAsJsonObject("textures").entrySet().forEach(e->{String v=e.getValue().getAsString();check(v.startsWith("createtransit:block/escalator_"),"Dedicated kit texture");textures.put(e.getKey(),v);});
   if(p.toString().contains("_pane"))check(!json.get("ambientocclusion").getAsBoolean()&&json.get("render_type").getAsString().equals("minecraft:translucent"),"Glass remains translucent without dark AO");
   var geo=loader.read(json,null);
   for(var face:geo.faces()){
    var v=face.vertices();var normal=new org.joml.Vector3f(v[1]).sub(new org.joml.Vector3f(v[0])).cross(new org.joml.Vector3f(v[2]).sub(new org.joml.Vector3f(v[0])));
    check(normal.lengthSquared()>1e-12,"Every static face has a usable normal: "+p);
    if(p.getFileName().toString().equals("escalator_incline_base.json"))for(var point:v)check(point[1]+point[2]>=11d/16-1e-6&&point[1]+point[2]<=17.85/16+1e-6,"Enclosed underside follows diagonal slope");
   }
   for(var rot:List.of(BlockModelRotation.X0_Y0,BlockModelRotation.X0_Y90,BlockModelRotation.X0_Y180,BlockModelRotation.X0_Y270)){
    var baked=geo.bake(new LrtModelCheck.Context(textures,json.get("ambientocclusion").getAsBoolean()),null,m->UnitTextureAtlasSprite.INSTANCE,rot,ItemOverrides.EMPTY);
    var faces=baked.getQuads(null,null,RandomSource.create());check(!faces.isEmpty(),"Nonempty mesh");int faceIndex=0;for(var q:faces){quads++;var source=geo.faces().get(faceIndex++);if(p.getFileName().toString().startsWith("escalator_surface_"))check(!q.isShade()&&!q.hasAmbientOcclusion()&&q.getTintIndex()==-1,"Shared unit surfaces receive per-instance lighting, with no extra diffuse shading or dye");var v=q.getVertices();for(int i=0;i<4;i++){if(source.texture().equals("#led"))check(v[i*IQuadTransformer.STRIDE+IQuadTransformer.UV2]==net.minecraft.client.renderer.LightTexture.FULL_BRIGHT&&!q.isShade()&&!q.hasAmbientOcclusion(),"Lit LED trim bakes as native fullbright");if(source.texture().equals("#trim_off"))check(v[i*IQuadTransformer.STRIDE+IQuadTransformer.UV2]==0&&q.isShade()&&q.hasAmbientOcclusion()&&q.getTintIndex()==1,"Unlit trim keeps LED dye but uses normal world light, shading and AO");for(int a=0;a<3;a++)check(Float.isFinite(Float.intBitsToFloat(v[i*IQuadTransformer.STRIDE+IQuadTransformer.POSITION+a])),"Finite native vertex");for(int a=0;a<2;a++){float uv=Float.intBitsToFloat(v[i*IQuadTransformer.STRIDE+IQuadTransformer.UV0+a]);check(uv>=0&&uv<=1,"Sprite-local UV");}}}
   }models++;
  }}
  for(boolean slope:new boolean[]{true,false})for(int tick=0;tick<200;tick++){double phase=EscalatorGeometry.wrap(tick*EscalatorGeometry.SPEED);var steps=EscalatorGeometry.treads(slope,phase);double end=0;for(var t:steps){check(Math.abs(t.from()-end)<1e-8,"Treads cover the whole block without gaps");check(t.to()>t.from()&&t.height()>=.125&&t.height()<=1.125,"Positive physical tread");check(Math.abs(t.height()-EscalatorGeometry.height(slope,(t.from()+t.to())/2,phase))<1e-8,"Rendered tops equal physical collision");end=t.to();}check(Math.abs(end-1)<1e-8,"Landing edge coverage");}
  int frames=0;var emitted=new LrtModelCheck.Vertices();
  try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(EscalatorModelCheck.class.getResourceAsStream("/escalator-visual-contract.json")))){
   for(var raw:JsonParser.parseReader(reader).getAsJsonArray()){
    var frame=raw.getAsJsonObject();
    var mesh=new com.sanctuarygames.createtransit.escalator.client.EscalatorRenderMesh();
    mesh.update(EscalatorBlock.Kind.valueOf(frame.get("kind").getAsString().toUpperCase(Locale.ROOT)),frame.get("left").getAsBoolean(),frame.get("right").getAsBoolean(),frame.get("phase").getAsDouble(),false,false,frame.get("flat_join").getAsBoolean(),frame.get("slope_join").getAsBoolean());
    var expected=frame.getAsJsonArray("quads");check(mesh.faceCount()==expected.size(),"Reduced preview and native faces agree");
    var emitter=new com.sanctuarygames.createtransit.escalator.client.EscalatorRenderMesh.Emitter();
    var sprites=new net.minecraft.client.renderer.texture.TextureAtlasSprite[6];java.util.Arrays.fill(sprites,UnitTextureAtlasSprite.INSTANCE);
    var nativeFrame=new EscalatorPerformanceCheck.Recording();emitter.emit(new com.mojang.blaze3d.vertex.PoseStack().last(),nativeFrame,mesh,sprites,0);
    verifyDeck(EscalatorBlock.Kind.valueOf(frame.get("kind").getAsString().toUpperCase(Locale.ROOT)),frame.get("flat_join").getAsBoolean(),frame.get("slope_join").getAsBoolean(),mesh,nativeFrame);
    for(int face=0;face<mesh.faceCount();face++){
     var q=expected.get(face).getAsJsonObject();check(q.get("texture").getAsString().equals("#"+com.sanctuarygames.createtransit.escalator.client.EscalatorRenderMesh.MATERIALS[mesh.material(face)]),"Moving material agrees with preview");
     for(int i=0;i<4;i++)for(int axis=0;axis<3;axis++)check(Math.abs(nativeFrame.vertices.get(face*4+i)[axis]*16-q.getAsJsonArray("vertices").get(i).getAsJsonArray().get(axis).getAsDouble())<2e-6,"Exact reduced preview/native vertex agreement");
    }
    for(int angle:new int[]{0,90,180,270}){
     var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));emitter.emit(pose.last(),emitted,mesh,sprites,0);
    }
    frames++;
   }
  }
  check(frames==320&&emitted.count>0&&emitted.count==emitted.uvCount,"All moving configurations emit valid mapped vertices");
  var cache=new com.sanctuarygames.createtransit.escalator.client.EscalatorRenderMesh.Cache();
  var middle=cache.get(EscalatorBlock.Kind.FLAT,true,true,.04,false,true,true,true);
  var entrance=cache.get(EscalatorBlock.Kind.FLAT,true,true,.04,false,true,false,true);
  var exit=cache.get(EscalatorBlock.Kind.FLAT,true,true,.04,false,true,true,false);
  var single=cache.get(EscalatorBlock.Kind.FLAT,true,true,.04,false,true,false,false);
  check(middle!=entrance&&entrance!=exit&&exit!=single,"Stopped-phase mesh cache separates all four exposed-end selections");
  check(middle==cache.get(EscalatorBlock.Kind.FLAT,true,true,.04,false,true,true,true),"Returning to an earlier join reuses the same bounded mesh buffer");
  int[] signals=new int[4];int at=0;for(var m:List.of(middle,entrance,exit,single)){for(int i=0;i<m.faceCount();i++)if(m.material(i)>=4)signals[at]++;at++;}
  check(Arrays.equals(signals,new int[]{0,2,2,4}),"Direction panels only exist at real flat ends, including both ends of a lone section");
  for(var k:List.of(EscalatorBlock.Kind.LOWER_LANDING,EscalatorBlock.Kind.UPPER_LANDING)){
   var through=cache.get(k,true,true,.04,false,true,true,true);for(int i=0;i<through.faceCount();i++)check(through.material(i)<4,"Internal walkway-to-slope landing has no floating end-only direction panel");
  }
  System.out.println("PASS: All four flat end modes, stopped-phase cache reuse and clean internal-landing motion surfaces.");
  System.out.println("PASS: Native moving deck and fixed end plates cover each flat section without gaps or overlapping stripes.");
  System.out.println("PASS: "+frames+" moving configurations match preview geometry / "+emitted.count+" native vertices in four orientations.");
  for(var name:List.of("Escalator-QuickBuild-2-Wide-Glass","Escalator-QuickBuild-2-Wide-Solid","Travelator-QuickBuild-3-Wide")){
   var nbt=net.minecraft.nbt.NbtIo.readCompressed(Path.of(args[1]).resolve(name+".nbt"),net.minecraft.nbt.NbtAccounter.unlimitedHeap());check(nbt.getInt("DataVersion")==3955,"1.21.1 schematic");int expected=name.startsWith("Travelator")?30:12;int count=0;var palette=nbt.getList("palette",10);
   for(var raw:nbt.getList("blocks",10)){var block=(net.minecraft.nbt.CompoundTag)raw;String id=palette.getCompound(block.getInt("state")).getString("Name");if(id.startsWith("createtransit:")){check(id.startsWith("createtransit:escalator_"),"QuickBuild uses only its own kit");check(block.getCompound("nbt").getString("id").equals("createtransit:escalator_section"),"Native block entity ID");count++;}}
   check(count==expected,"Complete widened QuickBuild: "+name);System.out.println("PASS: Minecraft parsed "+name+": "+count+" dedicated belt sections.");
  }
  System.out.println("PASS: "+models+" escalator models / "+quads+" native baked quads; four orientations, independent materials, glass layers, normalized UV; 400 moving tread configurations match collision.");
 }
 static void verifyDeck(EscalatorBlock.Kind kind,boolean back,boolean front,com.sanctuarygames.createtransit.escalator.client.EscalatorRenderMesh mesh,EscalatorPerformanceCheck.Recording frame){
  if(kind==EscalatorBlock.Kind.INCLINE)return;
  boolean frontCap=kind==EscalatorBlock.Kind.FLAT?!front:kind==EscalatorBlock.Kind.UPPER_LANDING&&!back;
  boolean backCap=kind==EscalatorBlock.Kind.FLAT?!back:kind==EscalatorBlock.Kind.LOWER_LANDING&&!back;
  double start=frontCap?.25:0,end=backCap?.75:1;
  var spans=new ArrayList<double[]>();
  if(frontCap)spans.add(new double[]{0,.25});
  if(backCap)spans.add(new double[]{.75,1});
  for(int face=0;face<mesh.faceCount();face++){
   if(mesh.material(face)>2)continue;
   double z=Double.POSITIVE_INFINITY,Z=Double.NEGATIVE_INFINITY;
   for(int v=0;v<4;v++){double value=frame.vertices.get(face*4+v)[2];z=Math.min(z,value);Z=Math.max(Z,value);}
   check(z>=start-1e-7&&Z<=end+1e-7,"Moving tops, risers and stripes stay outside fixed end plates");
   if(mesh.material(face)==0&&frame.normals.get(face*4)[1]>.99)spans.add(new double[]{z,Z});
  }
  spans.sort(Comparator.comparingDouble(span->span[0]));double covered=0;
  for(var span:spans){check(Math.abs(span[0]-covered)<1e-7,"Native flat deck has no gap or overlapping top");covered=span[1];}
  check(Math.abs(covered-1)<1e-7,"Native flat deck reaches both block edges");
 }
}
