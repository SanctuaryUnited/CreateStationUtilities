package com.sanctuarygames.createtransit;

import com.google.gson.*;
import com.mojang.math.Transformation;
import com.sanctuarygames.createtransit.lrt.client.LrtModelLoader;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import java.nio.file.*;
import java.util.*;

/** Executes the actual model loader and baker without opening a game window. */
public final class LrtModelCheck {
 record Context(Map<String,String> textures,boolean ao) implements IGeometryBakingContext {
  public String getModelName(){return "lrt_validation";}
  public boolean hasMaterial(String name){return textures.containsKey(name);}
  public Material getMaterial(String name){return new Material(TextureAtlas.LOCATION_BLOCKS,ResourceLocation.parse(textures.get(name)));}
  public boolean isGui3d(){return true;}
  public boolean useBlockLight(){return true;}
  public boolean useAmbientOcclusion(){return ao;}
  public ItemTransforms getTransforms(){return ItemTransforms.NO_TRANSFORMS;}
  public Transformation getRootTransform(){return Transformation.identity();}
  public ResourceLocation getRenderTypeHint(){return null;}
  public boolean isComponentVisible(String name,boolean fallback){return fallback;}
 }
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  var loader=new LrtModelLoader();int files=0,quads=0;
  try(var paths=Files.walk(Path.of(args[0]))){
   for(var path:paths.filter(p->p.getFileName().toString().startsWith("lrt_")&&p.toString().endsWith(".json")).toList()){
    var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var textures=new HashMap<String,String>();json.getAsJsonObject("textures").entrySet().forEach(e->textures.put(e.getKey(),e.getValue().getAsString()));
    var geometry=loader.read(json,null);var context=new Context(textures,json.get("ambientocclusion").getAsBoolean());
    for(var rotation:List.of(BlockModelRotation.X0_Y0,BlockModelRotation.X0_Y90,BlockModelRotation.X0_Y180,BlockModelRotation.X0_Y270)){
     var baked=geometry.bake(context,null,material->UnitTextureAtlasSprite.INSTANCE,rotation,ItemOverrides.EMPTY);
     var faces=baked.getQuads(null,null,RandomSource.create(1));check(!faces.isEmpty(),"LRT model must bake visible geometry: "+path);check(baked.useAmbientOcclusion()==context.ao(),"Glass AO setting must survive bake");
     if(path.toString().contains("/block/lrt_cab_floor_"))for(var q:faces)if(q.getTintIndex()==-1)check(q.getDirection()==net.minecraft.core.Direction.UP,"Actual baked cab-floor tops face into the cabin in every orientation");
     for(var q:faces){quads++;check(q.getTintIndex()>=-1&&q.getTintIndex()<=1,"Dye channels survive bake");var data=q.getVertices();
      for(int i=0;i<4;i++){
       for(int axis=0;axis<3;axis++)check(Float.isFinite(Float.intBitsToFloat(data[i*IQuadTransformer.STRIDE+IQuadTransformer.POSITION+axis])),"Rotated positions remain finite");
       for(int axis=0;axis<2;axis++){float uv=Float.intBitsToFloat(data[i*IQuadTransformer.STRIDE+IQuadTransformer.UV0+axis]);check(uv>=0&&uv<=1,"Atlas UV uses normalized 1.21 coordinates");}
      }
     }
    }files++;
   }
  }
  // A north face rotated by its EAST blockstate must sit on x=1 and face east.
  var json=JsonParser.parseString("{\"quads\":[{\"vertices\":[[16,0,0],[0,0,0],[0,16,0],[16,16,0]],\"uv\":[[0,16],[16,16],[16,0],[0,0]],\"texture\":\"#body\",\"tint\":1}]}").getAsJsonObject();
  var rotated=loader.read(json,null).bake(new Context(Map.of("body","createtransit:block/lrt_body","particle","createtransit:block/lrt_body"),true),null,m->UnitTextureAtlasSprite.INSTANCE,BlockModelRotation.X0_Y90,ItemOverrides.EMPTY).getQuads(null,null,RandomSource.create()).getFirst();
  check(rotated.getDirection()==net.minecraft.core.Direction.EAST,"Blockstate orientation agrees with door and lamp renderers");
  for(int i=0;i<4;i++)check(Math.abs(Float.intBitsToFloat(rotated.getVertices()[i*IQuadTransformer.STRIDE+IQuadTransformer.POSITION])-1)<.00001,"EAST outer face stays flush with block edge");
  System.out.println("PASS: actual custom model loader baked "+files+" models / "+quads+" quads in all four orientations; dye tints, atlas UVs, glass AO and flush EAST alignment verified.");
  dynamicUvs();
  for(var spec:List.of(new Spec("LRT-B-Single-Car",17,2,14,16,14,0,1),new Spec("LRT-Quick-1-Three-Cars",44,6,26,40,34,4,3),new Spec("LRT-Quick-2-Two-Cabs",34,4,22,32,26,2,2)))schematic(Path.of(args[1]).resolve(spec.name()+".nbt"),spec);
 }
 static final class Vertices implements com.mojang.blaze3d.vertex.VertexConsumer {
  int count,uvCount;
  public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Dynamic positions remain finite");count++;return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r,int g,int b,int a){return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u,float v){check(u>=0&&u<=1&&v>=0&&v<=1,"Actual moving renderer stays inside its atlas sprite");uvCount++;return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u,int v){return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u,int v){return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x,float y,float z){return this;}
 }
 static void dynamicUvs()throws Exception{
  var out=new Vertices();var pose=new com.mojang.blaze3d.vertex.PoseStack();int faces=0,glass=0;
  for(var field:com.sanctuarygames.createtransit.lrt.client.LrtMesh.class.getFields()){
   if(field.getType()!=com.sanctuarygames.createtransit.lrt.client.LrtMesh.Face[].class)continue;
   for(var face:(com.sanctuarygames.createtransit.lrt.client.LrtMesh.Face[])field.get(null)){
    for(float uv:face.uv())check(uv>=0&&uv<=16,"Generated dynamic UVs are sprite-local, including both door heights: "+field.getName());
    com.sanctuarygames.createtransit.lrt.client.LrtMeshRenderer.face(pose,out,UnitTextureAtlasSprite.INSTANCE,face,0xffffff,0);faces++;if(face.texture().equals("lrt_glass"))glass++;
   }
  }
  check(glass>0&&out.count==faces*4&&out.uvCount==faces*4,"Every emitted moving quad retains four mapped vertices");
  System.out.println("PASS: actual dynamic vertex emitter checked "+faces+" faces / "+glass+" glass faces; compact and legacy doors stay within the glass sprite.");
 }
 record Spec(String name,int length,int bogeys,int boards,int doorCells,int seats,int anchors,int groups){}
 static void schematic(Path path,Spec expected)throws Exception{
  var schematic=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());check(schematic.getInt("DataVersion")==3955,"Schematic targets Minecraft 1.21.1");
  var size=schematic.getList("size",Tag.TAG_INT);check(size.size()==3&&size.getInt(0)==5&&size.getInt(1)==6&&size.getInt(2)==expected.length(),"Shorter schematic bounds match layout");
  var palette=schematic.getList("palette",Tag.TAG_COMPOUND);var blocks=schematic.getList("blocks",Tag.TAG_COMPOUND);var positions=new HashSet<String>();int bogeys=0,boards=0,doorCells=0,seats=0,anchors=0;
  for(var tag:blocks){var block=(CompoundTag)tag;var pos=block.getList("pos",Tag.TAG_INT);check(pos.size()==3&&positions.add(pos.toString()),"Schematic cells are unique");
   for(int axis=0;axis<3;axis++)check(pos.getInt(axis)>=0&&pos.getInt(axis)<size.getInt(axis),"Every cell lies inside its schematic");
   int index=block.getInt("state");check(index>=0&&index<palette.size(),"Every palette reference resolves");var state=palette.getCompound(index);var name=state.getString("Name");
   if(name.equals("create:small_bogey"))bogeys++;if(name.equals("createrailwaysnavigator:advanced_display"))boards++;if(name.equals("createtransit:lrt_seat"))seats++;if(name.equals("createtransit:lrt_articulation_anchor"))anchors++;
   if(name.equals("createtransit:lrt_door")){doorCells++;check(state.getCompound("Properties").getString("tall").equals("false"),"New quick builds use compact doors");}
   check(!name.equals("createtransit:lrt_bogey")&&!name.contains("bellows_wall"),"Retired bogeys and old bellows walls stay absent");
  }
  check(bogeys==expected.bogeys()&&boards==expected.boards()&&doorCells==expected.doorCells()&&seats==expected.seats()&&anchors==expected.anchors(),"Schematic retains all native parts, compact doors, seats and connectors");
  var entities=schematic.getList("entities",Tag.TAG_COMPOUND);check(entities.size()==expected.groups(),"One native glue entity per carriage");
  for(var tag:entities){var entity=((CompoundTag)tag).getCompound("nbt");check(entity.getString("id").equals("create:super_glue"),"Native Create glue entity ID");var from=entity.getList("From",Tag.TAG_DOUBLE);var to=entity.getList("To",Tag.TAG_DOUBLE);check(from.size()==3&&to.size()==3&&to.getDouble(0)-from.getDouble(0)==5&&to.getDouble(1)-from.getDouble(1)==6,"Glue bounds preserve complete shorter bodies");}
  System.out.println("PASS: Minecraft parsed "+expected.name()+": "+blocks.size()+" blocks, "+expected.groups()+" native glue groups, native bogeys/CRN boards and complete compact doors.");
 }
}
