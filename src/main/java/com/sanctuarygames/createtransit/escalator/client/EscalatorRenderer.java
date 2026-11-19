package com.sanctuarygames.createtransit.escalator.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.sanctuarygames.createtransit.escalator.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import net.minecraft.core.Direction;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
public final class EscalatorRenderer implements BlockEntityRenderer<EscalatorEntity> {
 private static volatile int resourceGeneration;
 private static final Quaternionf[] ROTATIONS={Axis.YP.rotationDegrees(0),Axis.YP.rotationDegrees(-90),Axis.YP.rotationDegrees(180),Axis.YP.rotationDegrees(90)};
 public static Quaternionf rotation(Direction direction){return ROTATIONS[switch(direction){case EAST->1;case SOUTH->2;case WEST->3;default->0;}];}
 private final EscalatorRenderMesh.Cache meshes=new EscalatorRenderMesh.Cache();
 private final EscalatorRenderMesh.Emitter emitter=new EscalatorRenderMesh.Emitter();
 private TextureAtlasSprite[] sprites;
 private int observedGeneration=-1;
 public EscalatorRenderer(BlockEntityRendererProvider.Context c){}
 public static void invalidateResources(){resourceGeneration++;}
 @Override public void render(EscalatorEntity be,float partial,PoseStack p,MultiBufferSource out,int light,int overlay){
  if(VisualizationManager.supportsVisualization(be.getLevel()))return;
  var s=be.getBlockState();var b=(EscalatorBlock)s.getBlock();
  if(sprites==null||observedGeneration!=resourceGeneration){
   var atlas=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
   sprites=new TextureAtlasSprite[EscalatorRenderMesh.MATERIALS.length];
   for(int i=0;i<sprites.length;i++)sprites[i]=atlas.apply(EscalatorRegistry.id("block/escalator_"+EscalatorRenderMesh.MATERIALS[i]));
   observedGeneration=resourceGeneration;
  }
  p.pushPose();p.translate(.5,0,.5);p.mulPose(rotation(s.getValue(EscalatorBlock.FACING)));p.translate(-.5,0,-.5);
  var mesh=meshes.get(EscalatorBlock.kind(s),!s.getValue(EscalatorBlock.LEFT),!s.getValue(EscalatorBlock.RIGHT),be.phase(partial),s.getValue(EscalatorBlock.REVERSED),s.getValue(EscalatorBlock.STOPPED),s.getValue(EscalatorBlock.FLAT_JOIN),s.getValue(EscalatorBlock.SLOPE_JOIN));
  emitter.emit(p.last(),out.getBuffer(RenderType.solid()),mesh,sprites,light);
  p.popPose();
 }
 public static void emit(PoseStack p,VertexConsumer out,TextureAtlasSprite sprite,EscalatorVisuals.Part part,int light){
  double[][] uv={{0,1},{0,0},{1,0},{1,1}};
  for(var v:EscalatorVisuals.vertices(part)){
   var normal=new Vector3f((float)(v[3]-v[0]),(float)(v[4]-v[1]),(float)(v[5]-v[2])).cross(new Vector3f((float)(v[6]-v[0]),(float)(v[7]-v[1]),(float)(v[8]-v[2])));
   if(normal.lengthSquared()<1e-12)continue;normal.normalize();float shade=light==LightTexture.FULL_BRIGHT?1:.7f+.3f*Math.max(0,normal.y());
   for(int i=0;i<4;i++)out.addVertex(p.last(),(float)v[i*3]/16,(float)v[i*3+1]/16,(float)v[i*3+2]/16)
    .setColor(shade,shade,shade,1).setUv(sprite.getU((float)uv[i][0]),sprite.getV((float)uv[i][1]))
    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p.last(),normal.x(),normal.y(),normal.z());
  }
 }
 @Override public AABB getRenderBoundingBox(EscalatorEntity be){return be.renderBounds();}
}
