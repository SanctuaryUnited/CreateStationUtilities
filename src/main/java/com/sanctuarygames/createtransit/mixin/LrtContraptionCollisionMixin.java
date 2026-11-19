package com.sanctuarygames.createtransit.mixin;
import com.sanctuarygames.createtransit.lrt.LrtGangwayCollision;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.collision.CollisionList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Contraption.class,remap=false)
public abstract class LrtContraptionCollisionMixin {
 @Unique private final LrtGangwayCollision.NativeCache transit$gangway=new LrtGangwayCollision.NativeCache();
 @Inject(method="getSimplifiedEntityColliders",at=@At("RETURN"),cancellable=true)
 private void transit$physicalGangway(CallbackInfoReturnable<CollisionList> cir){cir.setReturnValue(transit$gangway.apply((Contraption)(Object)this,cir.getReturnValue()));}
 @Inject(method="invalidateColliders",at=@At("HEAD"))
 private void transit$refreshGangway(CallbackInfo ci){transit$gangway.clear();}
}
