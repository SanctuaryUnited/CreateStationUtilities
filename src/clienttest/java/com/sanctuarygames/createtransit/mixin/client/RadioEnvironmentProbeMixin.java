package com.sanctuarygames.createtransit.mixin.client;
import com.sanctuarygames.createtransit.radio.client.RadioReentryValidation;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Test-only capture of the actual environment applied to each native source. */
@Pseudo @Mixin(targets="com.sonicether.soundphysics.SoundPhysics",remap=false)
public abstract class RadioEnvironmentProbeMixin {
 @Inject(method="setEnvironment",at=@At("TAIL"),remap=false)
 private static void record(int source,float s0,float s1,float s2,float s3,float c0,float c1,float c2,float c3,float cutoff,float gain,CallbackInfo ci){RadioReentryValidation.environment(source,gain,cutoff,s0+s1+s2+s3);}
}
