package com.sanctuarygames.createtransit.mixin.client;
import com.sanctuarygames.createtransit.radio.client.RadioSoundPhysicsBridge;
import com.mojang.blaze3d.audio.Channel;import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
/** Negative control: only included by the explicit development baseline flag. */
@Mixin(RadioSoundPhysicsBridge.class)
public abstract class RadioRefreshDisabledMixin {
 @Inject(method="refresh",at=@At("HEAD"),cancellable=true,remap=false)
 private static void suppress(Channel channel,SoundInstance sound,CallbackInfo ci){ci.cancel();}
 @Inject(method="needsPeriodicRefresh",at=@At("HEAD"),cancellable=true,remap=false)
 private static void suppressTick(CallbackInfoReturnable<Boolean> ci){ci.setReturnValue(false);}
}
