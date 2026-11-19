package com.sanctuarygames.createtransit.lrt;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;

public final class LrtHeadlightMovement implements MovementBehaviour {
 /** Forward in Create's assembly orientation is the leading white cab. */
 public static boolean leading(Direction cab,Direction assembly,boolean reversed){return (cab==assembly)!=reversed;}
 @Override public boolean disableBlockEntityRendering(){return true;}
 @Override public void renderInContraption(MovementContext c,VirtualRenderWorld world,ContraptionMatrices matrices,MultiBufferSource buffers){
  if(!(c.contraption.entity instanceof CarriageContraptionEntity entity)||!(entity.getContraption() instanceof CarriageContraption car)||entity.getCarriage()==null)return;
  var train=entity.getCarriage().train;
  boolean backwards=Math.abs(train.speed)>0.0001?train.speed<0:train.currentlyBackwards;
  var p=matrices.getModelViewProjection();p.pushPose();p.translate(c.localPos.getX(),c.localPos.getY(),c.localPos.getZ());
  LrtRenderProxy.render(p,buffers,c.state,leading(c.state.getValue(LrtBlock.FACING),car.getAssemblyDirection(),backwards));p.popPose();
 }
 private static final class LrtRenderProxy {
  static void render(com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,net.minecraft.world.level.block.state.BlockState state,boolean white){com.sanctuarygames.createtransit.lrt.client.LrtRender.cabLights(pose,buffers,state,white);}
 }
}
