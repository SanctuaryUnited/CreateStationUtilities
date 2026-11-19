package com.sanctuarygames.createtransit.mixin.client;
import com.simibubi.create.content.trains.track.TrackVisual;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=TrackVisual.class,remap=false)
public interface TrackVisualAccess {
    @Accessor("visualPos") BlockPos transit$visualPos();
}
