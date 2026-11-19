package com.sanctuarygames.createtransit.escalator.client;

import com.sanctuarygames.createtransit.escalator.EscalatorSoundscape;
import com.sanctuarygames.createtransit.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Client-only loop. The synchronized STOPPED state is the source of truth. */
public final class EscalatorSoundInstance extends AbstractTickableSoundInstance {
    private static final float VOLUME = .35f;
    private final ClientLevel level;
    private EscalatorSoundscape.Emitter emitter;
    private boolean retired;
    private float gain;

    public EscalatorSoundInstance(ClientLevel level, EscalatorSoundscape.Emitter emitter) {
        super(ModSounds.ESCALATOR_RUNNING.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.level = level;
        this.emitter = emitter;
        looping = true;
        delay = 0;
        relative = false;
        attenuation = Attenuation.LINEAR;
        pitch = 1;
        volume = .001f;
        x = emitter.position().x;
        y = emitter.position().y;
        z = emitter.position().z;
    }

    public void update(EscalatorSoundscape.Emitter next) {
        emitter = next;
        retired = false;
    }

    public void retire() { retired = true; }

    public void terminate() {
        retired = true;
        volume = 0;
        stop();
    }

    @Override public boolean canStartSilent() { return true; }

    @Override public void tick() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != level || !level.hasChunkAt(emitter.section())
            || !(level.getBlockState(emitter.section()).getBlock()
                instanceof com.sanctuarygames.createtransit.escalator.EscalatorBlock)) {
            stop();
            return;
        }
        boolean moving = !retired && EscalatorSoundscape.audible(level, emitter.section(), level.getBlockState(emitter.section()));
        gain = moving ? Math.min(1, gain + 1f / 8) : Math.max(0, gain - 1f / 4);
        volume = VOLUME * gain;
        if (!moving && gain == 0) {
            stop();
            return;
        }
        // The nearest deck section follows the listener along a long belt.
        x += (emitter.position().x - x) * .35;
        y += (emitter.position().y - y) * .35;
        z += (emitter.position().z - z) * .35;
    }
}
