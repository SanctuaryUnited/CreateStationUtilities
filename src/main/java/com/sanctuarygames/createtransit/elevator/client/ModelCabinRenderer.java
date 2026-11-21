package com.sanctuarygames.createtransit.elevator.client;
import com.sanctuarygames.createtransit.elevator.model.*;import com.mojang.blaze3d.vertex.*;import com.mojang.math.Axis;
import net.minecraft.client.*;import net.minecraft.client.renderer.*;import net.minecraft.client.renderer.entity.*;import net.minecraft.client.renderer.texture.*;import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.item.DyeColor;import net.neoforged.neoforge.client.model.data.ModelData;import java.util.*;
public final class ModelCabinRenderer extends EntityRenderer<ModelCabinEntity> {
 public ModelCabinRenderer(EntityRendererProvider.Context context){super(context);}
 @Override public ResourceLocation getTextureLocation(ModelCabinEntity e){return TextureAtlas.LOCATION_BLOCKS;}
 @Override public void render(ModelCabinEntity cabin,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
  var config=cabin.configuration();
  pose.pushPose();
  pose.mulPose(Axis.YP.rotationDegrees(-cabin.facing().get2DDataValue()*90));
  pose.translate(cabin.offsetX(),0,cabin.offsetZ());
  pose.scale((float)cabin.widthScale(),1,(float)cabin.depthScale());
  if(config.mainLights())light=LightTexture.pack(config.brightness(),LightTexture.sky(light));
  draw(config,CabinRenderGeometry.body(config),pose,buffers,light);
  draw(config,CabinRenderGeometry.doors(config,cabin.front(partial),cabin.rear(partial)),pose,buffers,light);
  entranceDisplay(cabin,pose,buffers,false,partial);
  if(config.rearEnabled())entranceDisplay(cabin,pose,buffers,true,partial);
  for(boolean right:new boolean[]{false,true})drawQueue(cabin,pose,buffers,right);
  pose.popPose();
  super.render(cabin,yaw,partial,pose,buffers,light);
 }
 private static void drawQueue(ModelCabinEntity cabin,PoseStack pose,MultiBufferSource buffers,boolean right){
  var style=cabin.queueDisplay(right);
  if(style==ModelCabinEntity.QueueDisplay.OFF)return;
  var c=cabin.configuration();
  boolean vertical=style==ModelCabinEntity.QueueDisplay.VERTICAL;
  var mount=CabinQueueLayout.mount(c,right,vertical);
  double y=vertical?1.76:CabinGeometry.ceilingBottom(c)-.25;
  double width=mount.width(),height=vertical?.66:.24;
  int accent=DoorIndicatorRenderer.accent(c);
  pose.pushPose();
  pose.translate(mount.x(),y,mount.z());
  pose.mulPose(Axis.YP.rotationDegrees(right?90:-90));
  IndicatorArtwork.insetPanel(pose,buffers,-width/2,-height/2,width,height,.009,0,accent,0x101820);
  CabinSelectorRenderer.text(pose,buffers,"UPCOMING STOPS",0,height/2-.048,width-.04,.053,0xe3e9ee);
  int[] stops=cabin.upcomingStops();
  int count=Math.min(stops.length,vertical?5:4);
  if(count==0)CabinSelectorRenderer.text(pose,buffers,"—",0,-.035,width-.04,.10,0xe3e9ee);
  int visible=stops.length>count?count-1:count;
  for(int i=0;i<count;i++){
   String label=i==visible?"+"+(stops.length-visible):cabin.selectorLabel(stops[i]);
   if(i==0&&vertical)label+=" · NEXT";
   double xx=vertical?0:width/2-(i+.5)*width/count;
   double yy=vertical?height/2-.15-i*.096:-.046;
   CabinSelectorRenderer.text(pose,buffers,label,xx,yy,vertical?width-.038:width/count-.019,.079,i==0?DoorIndicatorRenderer.accentText(c):0xe3e9ee);
  }
  pose.popPose();
 }
 private static void entranceDisplay(ModelCabinEntity cabin,PoseStack pose,MultiBufferSource buffers,boolean rear,float partial){
  var c=cabin.configuration();
  int direction=cabin.jambDirection();
  DoorIndicatorRenderer.doorStrips(c,pose,buffers,CabinGeometry.FLOOR_TOP,
      Math.min(CabinGeometry.entranceFace(c,rear),CabinGeometry.passageEnd(c,rear)),
      CabinGeometry.PASSAGE_DEPTH,rear,cabin.doorTravel(rear)/40.0,
      cabin.doorOpening(rear),cabin.level().getGameTime());
  double scroll=cabin.animatedArrows()&&cabin.arrowsMoving()?DoorIndicatorRenderer.scroll(direction,cabin.level().getGameTime(),partial):0;
  if(c.legacyGeometry()){
   double z=c.v13Geometry()?(rear?c.size()-.5:0):rear?c.size()-CabinGeometry.INSET-.48:CabinGeometry.INSET+.02;
   DoorIndicatorRenderer.draw(c,pose,buffers,z,.125,.46,cabin.floorLabel(),direction,direction,cabin.mirrorIndicators(),false,null,scroll,cabin.classicDirectionColors());
   return;
  }
  CabinSelectorRenderer.draw(cabin,pose,buffers,rear);
  drawModernEntrance(c,pose,buffers,rear,cabin.floorLabel(),direction,cabin.mirrorIndicators(),scroll,cabin.classicDirectionColors());
 }
 static void drawModernEntrance(CabinConfiguration c,PoseStack pose,MultiBufferSource buffers,boolean rear,String floor,int direction,boolean mirror){
  drawModernEntrance(c,pose,buffers,rear,floor,direction,mirror,0,false);
 }
 private static void drawModernEntrance(CabinConfiguration c,PoseStack pose,MultiBufferSource buffers,boolean rear,String floor,int direction,boolean mirror,double scroll,boolean directional){
  if(!c.indicators())return;
  DoorIndicatorRenderer.cabinArrows(c,pose,buffers,rear,direction,directional);
  double z=CabinGeometry.passageEnd(c,rear);
  pose.pushPose();
  pose.translate(c.size()/2.0,0,z+(rear?-.015:.015));
  if(!rear)pose.mulPose(Axis.YP.rotationDegrees(180));
  var style=c.indicatorStyle();
  IndicatorArtwork.panel(pose,buffers,-.70,2.22,1.40,.39,0,style,true,DoorIndicatorRenderer.accent(c));
  CabinSelectorRenderer.text(pose,buffers,floor,0,2.415,.94,.22,DoorIndicatorRenderer.accentText(c));
  for(boolean right:new boolean[]{false,true}){
   boolean up=right^mirror;
   DoorIndicatorRenderer.positionArrow(pose,buffers,right?.56:-.56,2.415,.24,-.008,up,up?direction>0:direction<0,DoorIndicatorRenderer.directionColor(c,up,directional),style,scroll);
  }
  pose.popPose();
 }
 public static String doorMaterial(CabinConfiguration c,boolean secondary){if(c.doorStyle()==CabinConfiguration.DoorStyle.COPYCAT)return secondary?c.materialSecondary():c.materialPrimary();return switch(c.doorStyle()){
 case WOOD_TRIM->secondary?c.wood():"minecraft:iron_block";case EXECUTIVE->secondary?"minecraft:dark_oak_planks":"minecraft:black_concrete";case ART_DECO->secondary?"minecraft:gold_block":"minecraft:green_concrete";case RETRO->secondary?"minecraft:gold_block":"minecraft:smooth_sandstone";case HERITAGE->secondary?"minecraft:spruce_planks":"minecraft:cut_copper";case INDUSTRIAL,DARK_MODERN->secondary?"minecraft:polished_deepslate":"minecraft:gray_concrete";case CLINICAL->secondary?"minecraft:light_blue_concrete":"minecraft:white_concrete";case TRANSIT->secondary?"minecraft:blue_concrete":"minecraft:iron_block";case COASTAL->secondary?"minecraft:cyan_concrete":"minecraft:smooth_sandstone";case HIGH_TECH->secondary?"minecraft:light_gray_concrete":"minecraft:white_concrete";default->secondary?"minecraft:polished_andesite":"minecraft:iron_block";};}
 public static void draw(CabinConfiguration c,List<CabinGeometry.Face> faces,PoseStack pose,MultiBufferSource buffers,int light){
  draw(c,faces,pose,buffers,light,1);
 }
 public static void draw(CabinConfiguration c,List<CabinGeometry.Face> faces,PoseStack pose,MultiBufferSource buffers,int light,float alpha){
  var sprites=sprites(c);
  emit(c,faces,pose,buffers,light,sprites,alpha);
 }
 private record SpriteKey(CabinConfiguration configuration,String quality){}
 private static TextureAtlasSprite atlasGeneration;
 private static final Map<CabinGeometry.Face,List<TiledCabinMesh.Tile>> FLOOR_TILES=new LinkedHashMap<>(128,.75f,true){
  @Override protected boolean removeEldestEntry(Map.Entry<CabinGeometry.Face,List<TiledCabinMesh.Tile>> entry){return size()>512;}
 };
 private static final Map<SpriteKey,Map<CabinGeometry.Material,TextureAtlasSprite>> SPRITES=new LinkedHashMap<>(32,.75f,true){protected boolean removeEldestEntry(Map.Entry<SpriteKey,Map<CabinGeometry.Material,TextureAtlasSprite>> e){return size()>64;}};
 private static Map<CabinGeometry.Material,TextureAtlasSprite> sprites(CabinConfiguration c){
  var atlas=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);var generation=atlas.apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));if(generation!=atlasGeneration){SPRITES.clear();atlasGeneration=generation;}
  var key=new SpriteKey(c,com.sanctuarygames.createtransit.client.StationClientSettings.get().quality());var previous=SPRITES.get(key);if(previous!=null)return previous;
var sprites=new EnumMap<CabinGeometry.Material,TextureAtlasSprite>(CabinGeometry.Material.class);
  for(var m:CabinGeometry.Material.values()){
   String name=DoorStyleTextures.sprite(c.skin(),m,com.sanctuarygames.createtransit.client.StationClientSettings.get().quality());if(c.doorStyle()==CabinConfiguration.DoorStyle.CLASSIC_BRUSHED_STEEL&&m==CabinGeometry.Material.COPY_PRIMARY)name="createtransit:block/elevator3/classic_brushed_leaf";
   if(m==CabinGeometry.Material.FLOOR&&!c.floor().equals("createtransit:default_cabin_floor"))name=null;if(c.skin()==CabinConfiguration.Skin.WOOD_TRIM&&(m==CabinGeometry.Material.CEILING||m==CabinGeometry.Material.COPY_PRIMARY))name=null;if(name==null)name=switch(m){case FRAME,METAL->"createtransit:block/elevator_metal";case ACCENT,LIGHT->"minecraft:block/white_concrete";case GLASS->"minecraft:block/glass";case DARK->"minecraft:block/gray_concrete";default->null;};
   TextureAtlasSprite sprite;if(name!=null)sprite=atlas.apply(ResourceLocation.parse(name));else{var state=BuiltInRegistries.BLOCK.get(ResourceLocation.parse((m==CabinGeometry.Material.WOOD||m==CabinGeometry.Material.DOOR_WOOD||c.skin()==CabinConfiguration.Skin.WOOD_TRIM&&(m==CabinGeometry.Material.CEILING||m==CabinGeometry.Material.COPY_PRIMARY))?c.wood():m==CabinGeometry.Material.COPY_PRIMARY?doorMaterial(c,false):m==CabinGeometry.Material.COPY_SECONDARY?doorMaterial(c,true):c.floor())).defaultBlockState();var model=Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
   sprite=model.getParticleIcon(ModelData.EMPTY);
   if(m==CabinGeometry.Material.FLOOR){
    var random=net.minecraft.util.RandomSource.create(42);
    var faces=model.getQuads(state,net.minecraft.core.Direction.UP,random,ModelData.EMPTY,null);
    if(faces.isEmpty()){
     random.setSeed(42);
     faces=model.getQuads(state,null,random,ModelData.EMPTY,null).stream()
         .filter(face->face.getDirection()==net.minecraft.core.Direction.UP).toList();
    }
    if(!faces.isEmpty())sprite=faces.getFirst().getSprite();
   }}sprites.put(m,sprite);
  }
  SPRITES.put(key,sprites);return sprites;
 }
 /** Same vertex path as draw(), with sprites supplied by the atlas or native headless checks. */
 public static void emit(CabinConfiguration c,List<CabinGeometry.Face> faces,PoseStack pose,MultiBufferSource buffers,int light,Map<CabinGeometry.Material,TextureAtlasSprite> sprites){
  emit(c,faces,pose,buffers,light,sprites,1);
 }
 /** V13 face emission. Each authored face keeps its own UV rectangle. */
 private static void emit(CabinConfiguration c,List<CabinGeometry.Face> faces,PoseStack pose,MultiBufferSource buffers,int light,Map<CabinGeometry.Material,TextureAtlasSprite> sprites,float alpha){
  for(boolean transparent:new boolean[]{false,true}){
   // Glass goes in the translucent block sheet, which Minecraft draws after every entity and block entity's
   // solid parts. entityTranslucent is flushed as soon as another renderer asks for a different type, so glass
   // could reach the depth buffer before the cabin behind it and hide the cabin, showing the shaft instead.
   var out=buffers.getBuffer(alpha<1?RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS):transparent?net.minecraft.client.renderer.Sheets.translucentCullBlockSheet():RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
   emitFaces(c,faces,pose,out,light,sprites,alpha,transparent);
  }
 }
 static void emitFaces(CabinConfiguration c,List<CabinGeometry.Face> faces,PoseStack pose,VertexConsumer out,
     int light,Map<CabinGeometry.Material,TextureAtlasSprite> sprites,float alpha,boolean transparent){
   for(var face:faces){var m=face.material();boolean glass=m==CabinGeometry.Material.GLASS||m==CabinGeometry.Material.DOOR_GLASS;if(glass!=transparent)continue;var a=face.a();var b=face.b();var cVertex=face.c();double ux=b.x()-a.x(),uy=b.y()-a.y(),uz=b.z()-a.z(),vx=cVertex.x()-a.x(),vy=cVertex.y()-a.y(),vz=cVertex.z()-a.z();double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,length=Math.sqrt(nx*nx+ny*ny+nz*nz);if(length<1e-5)continue;nx/=length;ny/=length;nz/=length;
    int color=m==CabinGeometry.Material.LIGHT?DyeColor.byId(c.ceilingColor()).getTextureDiffuseColor():(m==CabinGeometry.Material.ACCENT||m==CabinGeometry.Material.DOOR_LED)?brighterStrip(DyeColor.byId(c.ledColor()).getTextureDiffuseColor()):0xffffff;
    if(c.primary()!=0&&(m==CabinGeometry.Material.WALL||m==CabinGeometry.Material.FRAME||m==CabinGeometry.Material.DOOR_FRAME||m==CabinGeometry.Material.COPY_PRIMARY||m==CabinGeometry.Material.RAIL_MATCH))color=DyeColor.byId(c.primary()).getTextureDiffuseColor();
    if(c.secondary()!=11&&m==CabinGeometry.Material.BASE_TRIM)color=DyeColor.byId(c.secondary()).getTextureDiffuseColor();
    boolean emit=m==CabinGeometry.Material.LIGHT||m==CabinGeometry.Material.ACCENT||m==CabinGeometry.Material.DOOR_LED;float shade=emit?1:.72f+.28f*(float)Math.max(0,ny);var sprite=sprites.get(m);
    if(Math.abs(ny)>.999&&(m==CabinGeometry.Material.FLOOR||m==CabinGeometry.Material.CEILING)){
     for(var tile:FLOOR_TILES.computeIfAbsent(face,TiledCabinMesh::buildFace))
      for(var v:tile.vertices())vertex(out,pose,v.p(),sprite,(float)v.u(),(float)v.v(),color,shade,alpha,light,nx,ny,nz);
     continue;
    }
    for(int i=0;i<4;i++){
     var v=switch(i){case 0->face.a();case 1->face.b();case 2->face.c();default->face.d();};
     vertex(out,pose,v,sprite,textureU(face,i),textureV(face,i),color,shade,
         alpha*(m==CabinGeometry.Material.DOOR_GLASS?.75f:transparent?.42f:1),
         emit?LightTexture.FULL_BRIGHT:light,nx,ny,nz);
    }
   }
 }
 private static void vertex(VertexConsumer out,PoseStack pose,CabinGeometry.Point v,TextureAtlasSprite sprite,
     float u,float textureV,int color,float shade,float alpha,int light,double nx,double ny,double nz){
  out.addVertex(pose.last(),(float)v.x(),(float)v.y(),(float)v.z())
      .setColor(((color>>16)&255)/255f*shade,((color>>8)&255)/255f*shade,(color&255)/255f*shade,alpha)
      .setUv(sprite.getU(u),sprite.getV(textureV)).setOverlay(OverlayTexture.NO_OVERLAY)
      .setLight(light).setNormal(pose.last(),(float)nx,(float)ny,(float)nz);
 }
 private static int brighterStrip(int rgb){
  int red=(rgb>>16)&255,green=(rgb>>8)&255,blue=rgb&255;
  double gain=Math.min(1.65,255.0/Math.max(1,Math.max(red,Math.max(green,blue))));
  return (int)Math.round(red*gain)<<16|(int)Math.round(green*gain)<<8|(int)Math.round(blue*gain);
 }
 public static float textureU(CabinGeometry.Face f,int i){float u=(float)(i==0||i==1?f.u0():f.u1()),v=(float)(i==0||i==3?f.v1():f.v0());return verticalWood(f)?v:u;}
 public static float textureV(CabinGeometry.Face f,int i){float u=(float)(i==0||i==1?f.u0():f.u1()),v=(float)(i==0||i==3?f.v1():f.v0());return verticalWood(f)?1-u:v;}
 private static boolean verticalWood(CabinGeometry.Face f){return f.material()==CabinGeometry.Material.DOOR_WOOD&&Math.abs(f.b().y()-f.a().y())>Math.abs(f.c().x()-f.a().x());}
}
