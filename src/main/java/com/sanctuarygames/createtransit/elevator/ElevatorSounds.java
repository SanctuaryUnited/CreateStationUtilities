package com.sanctuarygames.createtransit.elevator;
import com.sanctuarygames.createtransit.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.Chime;

/** Original elevator cues, independent of the vehicle opening chimes. */
public final class ElevatorSounds {
 private ElevatorSounds(){}
 public static SoundEvent cue(boolean open,boolean solid){return (open?(solid?ModSounds.ELEVATOR_SOLID_OPEN:ModSounds.ELEVATOR_GLASS_OPEN):ModSounds.GONDOLA_CLOSE).get();}
 public static void play(Level l,BlockPos p,boolean open,boolean solid){if(open&&!l.isClientSide)l.playSound(null,p,cue(open,solid),SoundSource.BLOCKS,1.15f,1f);}
 /** A null cue is the explicitly selected silent preset; legacy cues keep their IDs. */
 public static SoundEvent modelCue(Chime chime){return switch(chime){
  case OTIS -> ModSounds.ELEVATOR_SOLID_OPEN.get();
  case SCHINDLER_HT -> ModSounds.ELEVATOR_GLASS_OPEN.get();
  case ARRIVAL_DING -> ModSounds.ELEVATOR_ARRIVAL_DING.get();
  case ELEVATOR_BELL -> ModSounds.ELEVATOR_BELL.get();
  case JAPAN_ARRIVAL -> ModSounds.ELEVATOR_JAPAN_ARRIVAL.get();
  case CLASSIC_BELL_DOORS -> ModSounds.ELEVATOR_CLASSIC_BELL_DOORS.get();
  case FM_BELL -> ModSounds.ELEVATOR_FM_BELL.get();
  case SYNTH_DING -> ModSounds.ELEVATOR_SYNTH_DING.get();
  case NONE -> null;
 };}
 public static void playModel(Level level,BlockPos position,Chime chime){var event=modelCue(chime);if(event!=null&&!level.isClientSide)level.playSound(null,position,event,SoundSource.BLOCKS,1.15f,1f);}
}
