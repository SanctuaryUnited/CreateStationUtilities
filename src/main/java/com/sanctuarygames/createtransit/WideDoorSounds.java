package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Each cue combines its piano chime and air hiss in one server sound event. */
public final class WideDoorSounds {
    private WideDoorSounds(){}
    public static void play(Level level,BlockPos pos,boolean open){
        if(level.isClientSide())return;
        level.playSound(null,pos,(open?ModSounds.GONDOLA_OPEN:ModSounds.GONDOLA_CLOSE).get(),SoundSource.BLOCKS,.6f,1f);
    }
}
