package com.sanctuarygames.createtransit.elevator.client;
import com.sanctuarygames.createtransit.elevator.*;import com.sanctuarygames.createtransit.elevator.model.*;import com.mojang.blaze3d.vertex.PoseStack;import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;import net.minecraft.client.renderer.blockentity.*;import net.minecraft.world.phys.AABB;
public final class ModelLandingRenderer implements BlockEntityRenderer<ModelLandingEntity>{
 public ModelLandingRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(ModelLandingEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){var s=e.getBlockState();if(s.getValue(ModelLandingBlock.COLUMN)!=0||s.getValue(ModelLandingBlock.ROW)!=0)return;var facing=s.getValue(ElevatorPartBlock.FACING);pose.pushPose();switch(facing){case EAST->pose.translate(0,0,1);case NORTH->pose.translate(1,0,1);case WEST->pose.translate(1,0,0);default->{}}pose.mulPose(Axis.YP.rotationDegrees(-facing.get2DDataValue()*90));var c=ModelLandingBlock.configuration(s,e);ModelCabinRenderer.draw(c,CabinRenderGeometry.landingFrame(c),pose,buffers,light);ModelCabinRenderer.draw(c,CabinRenderGeometry.landingPanels(c,e.linearProgress(partial)),pose,buffers,light);double depth=DoorGeometry.depth(c);long time=e.getLevel().getGameTime();
 DoorIndicatorRenderer.draw(c,pose,buffers,1-depth,0,depth,e.direction==0?e.header(time):e.label,
     e.direction,e.serviceDirection,e.mirrorIndicators,false,e.landingLabel,
     e.animatedArrows&&e.direction!=0?DoorIndicatorRenderer.scroll(e.direction,time,partial):0,e.directionalLanterns,e.displayPowered);
 DoorIndicatorRenderer.doorStrips(c,pose,buffers,0,1-depth,depth,true,e.linearProgress(partial),s.getValue(ModelLandingBlock.OPEN),time);
 pose.popPose();}
 @Override public AABB getRenderBoundingBox(ModelLandingEntity e){var s=e.getBlockState();var base=ModelLandingBlock.base(e.getBlockPos(),s);var end=ModelLandingBlock.part(base,s.getValue(ElevatorPartBlock.FACING),s.getValue(ModelLandingBlock.SIZE)-1,s.getValue(ModelLandingBlock.KIT3)?2:1);return new AABB(base).minmax(new AABB(end)).inflate(.04);}
}
