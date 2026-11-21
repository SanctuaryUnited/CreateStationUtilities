package com.sanctuarygames.createtransit.mixin;
import com.sanctuarygames.createtransit.elevator.model.CabinCollisions;
import net.minecraft.world.entity.Entity;import net.minecraft.world.level.Level;import net.minecraft.world.phys.*;import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import java.util.List;
/**
 * Adds cabin walls to the shapes an entity collides with. Hooked on collectColliders, which both the normal move
 * (through collideBoundingBox) and the step-up retry in Entity.collide use. Hooking only collideBoundingBox left the
 * step-up retry without cabin walls, so with a block edge in step range a player could walk through a jamb.
 */
@Mixin(Entity.class)
public abstract class ElevatorCabinCollisionMixin {
 @ModifyVariable(method="collectColliders",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private static List<VoxelShape> transit$cabin(List<VoxelShape> original,Entity entity,Level level,List<VoxelShape> supplied,AABB query){return CabinCollisions.add(level,entity,query,original);}
}
