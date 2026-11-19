package com.sanctuarygames.createtransit.mixin.client;
import com.sanctuarygames.createtransit.radio.client.RadioAcoustics;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;import org.spongepowered.asm.mixin.injection.invoke.arg.Args;import com.llamalad7.mixinextras.injector.ModifyExpressionValue;import net.minecraft.resources.ResourceLocation;import net.minecraft.sounds.SoundSource;import net.minecraft.world.phys.Vec3;import org.lwjgl.openal.AL10;
/** Optional, per-source SPR 1.21.1 integration. Does not alter global reverb settings. */
@Pseudo @Mixin(targets="com.sonicether.soundphysics.SoundPhysics",remap=false)
public abstract class RadioSoundPhysicsMixin {
 @Unique private static final ThreadLocal<RadioAcoustics.Profile> transit$profile=new ThreadLocal<>();
 @Unique private static final ThreadLocal<Double> transit$distance=ThreadLocal.withInitial(()->0d);
 @Inject(method="evaluateEnvironment",at=@At("HEAD"),remap=false)
 private static void transit$begin(int source,double x,double y,double z,SoundSource category,ResourceLocation sound,float scale,CallbackInfoReturnable<Vec3> ci){var p=RadioAcoustics.find(sound,x,y,z);transit$profile.set(p);var player=net.minecraft.client.Minecraft.getInstance().player;if(p!=null){transit$distance.set(player==null?0:player.getEyePosition().distanceTo(new Vec3(x,y,z)));AL10.alSourcef(source,AL10.AL_ROLLOFF_FACTOR,0);}}
 @ModifyArgs(method="evaluateEnvironment",at=@At(value="INVOKE",target="Lcom/sonicether/soundphysics/SoundPhysics;setDefaultEnvironment(IF)V"),remap=false)
 private static void transit$fallback(Args args){var p=transit$profile.get();if(p!=null)args.set(1,((Float)args.get(1))*RadioAcoustics.fade(transit$distance.get(),p.direct(),p.falloff()));}
 @ModifyExpressionValue(method="evaluateEnvironment",at=@At(value="INVOKE",target="Lde/maxhenkel/sound_physics_remastered/configbuilder/entry/ConfigEntry;get()Ljava/lang/Object;",ordinal=0),remap=false)
 private static Object transit$processingDistance(Object original){var p=transit$profile.get();return p!=null&&original instanceof Double n?Double.valueOf(Math.max(n,(p.extended()?Math.max(p.direct(),p.reverb()):p.direct())+2d)):original;}
 @ModifyExpressionValue(method="evaluateEnvironment",at=@At(value="INVOKE",target="Lorg/lwjgl/openal/AL10;alGetSourcef(II)F"),remap=false)
 private static float transit$wetDistance(float original){return transit$profile.get()==null?original:Float.MAX_VALUE;}
 @ModifyArgs(method="evaluateEnvironment",at=@At(value="INVOKE",target="Lcom/sonicether/soundphysics/SoundPhysics;setEnvironment(IFFFFFFFFFF)V"),remap=false)
 private static void transit$gains(Args args){var p=transit$profile.get();if(p==null)return;double d=transit$distance.get();float direct=RadioAcoustics.fade(d,p.direct(),p.falloff());
  float wet=p.extended()?(float)p.level()*RadioAcoustics.fade(d,p.reverb(),p.falloff()):direct;
  for(int i=1;i<=4;i++)args.set(i,((Float)args.get(i))*wet);
  float cutoff=args.get(9), gain=args.get(10);
  args.set(9,(float)Math.pow(Math.clamp(cutoff,0,1),p.muffling()));
  args.set(10,(float)Math.pow(Math.clamp(gain,0,1),p.muffling())*direct);}
 @Inject(method="evaluateEnvironment",at=@At("RETURN"),remap=false)
 private static void transit$end(int source,double x,double y,double z,SoundSource category,ResourceLocation sound,float scale,CallbackInfoReturnable<Vec3> ci){transit$profile.remove();transit$distance.remove();}
}
