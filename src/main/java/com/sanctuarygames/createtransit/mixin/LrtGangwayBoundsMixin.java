package com.sanctuarygames.createtransit.mixin;
import com.sanctuarygames.createtransit.lrt.LrtGangwayCollision;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=ContraptionCollider.class,remap=false)
public abstract class LrtGangwayBoundsMixin {
 @ModifyVariable(method="collideEntities",at=@At("STORE"),ordinal=0)
 private static AABB transit$includeGangway(AABB bounds,AbstractContraptionEntity entity){return LrtGangwayCollision.movingBounds(entity,bounds);}
}
