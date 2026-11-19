package com.sanctuarygames.createtransit.mixin;
import com.sanctuarygames.createtransit.lrt.LrtGangwayCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;
@Mixin(Entity.class)
public abstract class LrtPlacedGangwayCollisionMixin {
 @ModifyVariable(method="collideBoundingBox",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private static List<VoxelShape> transit$placedShell(List<VoxelShape> original,Entity entity,Vec3 motion,AABB bounds,Level level,List<VoxelShape> supplied){return LrtGangwayCollision.placed(level,entity,bounds.expandTowards(motion),original);}
}
