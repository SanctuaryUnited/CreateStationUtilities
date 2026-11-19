package com.sanctuarygames.createtransit.elevator.client;
import com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity;import com.sanctuarygames.createtransit.radio.client.*;import com.sanctuarygames.createtransit.registry.ModSounds;import net.minecraft.client.resources.sounds.*;import net.minecraft.client.sounds.*;import net.minecraft.sounds.SoundSource;import net.minecraft.util.RandomSource;import java.util.concurrent.CompletableFuture;
public final class CabinMotorSound extends AbstractTickableSoundInstance {
 private final ModelCabinEntity cabin;private volatile double boost=1;
 public CabinMotorSound(ModelCabinEntity cabin){super(ModSounds.MODEL_MOTOR.get(),SoundSource.BLOCKS,RandomSource.create());this.cabin=cabin;looping=true;volume=.01f;updatePosition();}
 private void updatePosition(){var p=cabin.world(cabin.configuration().size()/2d,1,cabin.configuration().size()/2d);x=p.x;y=p.y;z=p.z;}
 public void end(){stop();}
 public void tick(){float target=cabin.isRemoved()?0:Math.min(1,cabin.motor());volume+=(target-volume)*.2f;boost=Math.max(1,cabin.motor());updatePosition();if(target==0&&volume<.01){stop();cabin.motorStopped();}}
 @Override public WeighedSoundEvents resolve(SoundManager m){var r=super.resolve(m);if(sound!=null)sound=new Sound(sound.getLocation(),sound.getVolume(),sound.getPitch(),sound.getWeight(),sound.getType(),false,false,20);return r;}

}
