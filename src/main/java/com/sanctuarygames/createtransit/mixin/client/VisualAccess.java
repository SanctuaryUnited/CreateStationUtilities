package com.sanctuarygames.createtransit.mixin.client;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=AbstractVisual.class,remap=false)
public interface VisualAccess {
    @Invoker("instancerProvider") InstancerProvider transit$provider();
}
