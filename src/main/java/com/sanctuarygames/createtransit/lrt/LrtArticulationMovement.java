package com.sanctuarygames.createtransit.lrt;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.renderer.MultiBufferSource;
public final class LrtArticulationMovement implements MovementBehaviour {
 @Override public boolean disableBlockEntityRendering(){return true;}
 @Override public void renderInContraption(MovementContext c,VirtualRenderWorld world,ContraptionMatrices matrices,MultiBufferSource buffers){
  if(!(c.contraption.entity instanceof CarriageContraptionEntity e))return;
  float partial=net.createmod.catnip.animation.AnimationTickHolder.getPartialTicks();
  var pair=LrtArticulationLink.find(e,c.localPos,c.state.getValue(LrtBlock.FACING),partial,c.state.getValue(LrtBlock.SHORT));
  if(pair!=null){var be=c.contraption.getBlockEntityClientSide(c.localPos);int paint=be instanceof LrtBlockEntity part?part.paint():0,accent=be instanceof LrtBlockEntity part?part.accent():0;com.sanctuarygames.createtransit.lrt.client.LrtRender.bridge(pair,matrices.getModelViewProjection(),buffers,v->e.toLocalVector(v,partial),v->net.minecraft.client.renderer.LevelRenderer.getLightColor(c.world,net.minecraft.core.BlockPos.containing(v)),paint,accent);}
 }
}
