package com.sanctuarygames.createtransit.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sanctuarygames.createtransit.cable.client.CableSegments;
import com.simibubi.create.content.trains.track.*;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.lib.instance.*;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Runs after Steam 'n' Rails' constructor hook; ordinary monorails are untouched. */
@Mixin(targets="com.simibubi.create.content.trains.track.TrackVisual$BezierTrackVisual",priority=500,remap=false)
public abstract class CableTrackVisualMixin {
    @Shadow @Final @Mutable private TransformedInstance[] ties;
    @Shadow @Final @Mutable private TransformedInstance[] left;
    @Shadow @Final @Mutable private TransformedInstance[] right;
    @Inject(method="<init>",at=@At("RETURN"))
    private void transit$cable(TrackVisual owner,BezierConnection connection,CallbackInfo ci){
        if(!CableSegments.isCable(connection))return;
        for(var old:ties)old.delete();for(var old:left)old.delete();for(var old:right)old.delete();
        var segments=CableSegments.bake(connection);
        ties=new TransformedInstance[segments.size()];left=new TransformedInstance[0];right=new TransformedInstance[0];
        ((VisualAccess)owner).transit$provider().instancer(InstanceTypes.TRANSFORMED,SpecialModels.smoothLit(CableSegments.MODEL)).createInstances(ties);
        PoseStack pose=new PoseStack();TransformStack.of(pose).translate(((TrackVisualAccess)owner).transit$visualPos());
        for(int i=0;i<ties.length;i++)ties[i].setTransform(pose).mul(segments.get(i).transform()).setChanged();
    }
}
