package com.sanctuarygames.createtransit.escalator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Gametest access to package-private escalator configuration. */
public final class EscalatorTestAccess {
    private EscalatorTestAccess() {}

    /** Flips the direction of the connected run at pos, keeping its speed, sound and colours,
     *  exactly as the settings menu does. */
    public static boolean reverse(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof EscalatorEntity section)) return false;
        boolean reversed = level.getBlockState(pos).getValue(EscalatorBlock.REVERSED);
        return EscalatorNetwork.configure(level, pos, section.speedIndex(), !reversed,
            section.paint(), section.accent(), section.soundEnabled());
    }
}
