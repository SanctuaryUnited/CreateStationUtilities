package com.sanctuarygames.createtransit.mixin.client;
import com.mojang.blaze3d.vertex.*;
import com.sanctuarygames.createtransit.cable.client.CableSegments;
import com.simibubi.create.content.trains.track.*;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=TrackRenderer.class,remap=false)
public abstract class CableTrackRendererMixin {
    @Inject(method="renderBezierTurn",at=@At("HEAD"),cancellable=true)
    private static void transit$cable(Level level,BezierConnection connection,PoseStack pose,VertexConsumer out,CallbackInfo ci){
        if(!CableSegments.isCable(connection))return;
        if(connection.isPrimary())for(var segment:CableSegments.bake(connection)){
            int light=LevelRenderer.getLightColor(level,segment.lightPosition().offset(connection.bePositions.getFirst()));
            CachedBuffers.partial(CableSegments.MODEL,Blocks.AIR.defaultBlockState()).mulPose(segment.transform().pose()).mulNormal(segment.transform().normal()).light(light).renderInto(pose,out);
        }
        ci.cancel();
    }
}
