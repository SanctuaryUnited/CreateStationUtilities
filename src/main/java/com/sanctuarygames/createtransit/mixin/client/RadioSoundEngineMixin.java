package com.sanctuarygames.createtransit.mixin.client;

import net.minecraft.client.resources.sounds.TickableSoundInstance;
import com.sanctuarygames.createtransit.radio.client.RadioSoundPhysicsBridge;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import java.util.Map;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Refresh positional transit loops when SPR's global moving-sound option is off. */
@Mixin(SoundEngine.class)
public abstract class RadioSoundEngineMixin {
    @Shadow @Final private Map<SoundInstance,ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow private int tickCount;

    @Inject(method="calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",at=@At("RETURN"),cancellable=true)
    private void stationElevatorVolume(SoundInstance sound,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> ci){if(sound.getLocation().getNamespace().equals("createtransit")&&sound.getLocation().getPath().startsWith("elevator_"))ci.setReturnValue(ci.getReturnValue()*(float)com.sanctuarygames.createtransit.client.StationClientSettings.get().elevatorVolume);}
    @Inject(method="tickNonPaused",at=@At("TAIL"))
    private void transit$refreshTransitSounds(CallbackInfo ci) {
        if(instanceToChannel.isEmpty()||!RadioSoundPhysicsBridge.needsPeriodicRefresh())return;
        for(var entry:instanceToChannel.entrySet()) {
            var sound=entry.getKey();var handle=entry.getValue();
            if(sound instanceof TickableSoundInstance&&RadioSoundPhysicsBridge.supports(sound)&&!handle.isStopped()
                    &&Math.floorMod(tickCount+System.identityHashCode(sound),5)==0)
                handle.execute(channel->RadioSoundPhysicsBridge.refresh(channel,sound));
        }
    }
}
