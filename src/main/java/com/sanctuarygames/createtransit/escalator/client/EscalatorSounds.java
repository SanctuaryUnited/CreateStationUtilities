package com.sanctuarygames.createtransit.escalator.client;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.escalator.EscalatorEntity;
import com.sanctuarygames.createtransit.escalator.EscalatorSoundscape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;

@EventBusSubscriber(modid = CreateTransit.MOD_ID, value = Dist.CLIENT)
public final class EscalatorSounds {
    private static final Map<BlockPos, EscalatorSoundInstance> loops = new HashMap<>();
    private static ClientLevel observedLevel;
    private static int refresh;

    private EscalatorSounds() {}

    private static void clear() {
        if (!loops.isEmpty()) {
            var manager = Minecraft.getInstance().getSoundManager();
            loops.values().forEach(sound -> {
                sound.terminate();
                manager.stop(sound);
            });
        }
        loops.clear();
        observedLevel = null;
        refresh = 0;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    // FML puts the sound engine reload on the mod bus and ticks/logout on the game bus, by event type.
    @EventBusSubscriber(modid = CreateTransit.MOD_ID, value = Dist.CLIENT)
    public static final class SoundReload {
        @SubscribeEvent public static void reload(SoundEngineLoadEvent event) { clear(); }
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != observedLevel) {
            clear();
            observedLevel = minecraft.level;
        }
        var level = observedLevel;
        var camera = minecraft.getCameraEntity();
        if (level == null || camera == null) return;
        // Muting a category can discard its native channels. Recreate our loops
        // when unmuted instead of retaining a silent, apparently active instance.
        if (minecraft.options.getSoundSourceVolume(SoundSource.BLOCKS) <= 0
            || minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0) {
            clear();
            observedLevel = level;
            return;
        }
        if (minecraft.isPaused()) return;
        if (refresh-- > 0) return;
        refresh = 9;

        var listener = camera.getEyePosition();
        var nearby = new ArrayList<BlockPos>();
        int minX = ((int) Math.floor(listener.x - EscalatorSoundscape.RANGE)) >> 4;
        int maxX = ((int) Math.floor(listener.x + EscalatorSoundscape.RANGE)) >> 4;
        int minZ = ((int) Math.floor(listener.z - EscalatorSoundscape.RANGE)) >> 4;
        int maxZ = ((int) Math.floor(listener.z + EscalatorSoundscape.RANGE)) >> 4;
        // Inspect only already-loaded chunk entities twice a second. No world scan,
        // forced chunk loads, sound packets, or per-section loops.
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            for (var entity : chunk.getBlockEntities().values()) {
                if (entity instanceof EscalatorEntity && entity.getBlockPos().getCenter().distanceToSqr(listener)
                    < (EscalatorSoundscape.RANGE + 1) * (EscalatorSoundscape.RANGE + 1)) {
                    nearby.add(entity.getBlockPos());
                }
            }
        }
        var manager = minecraft.getSoundManager();
        var wanted = new HashSet<BlockPos>();
        for (var emitter : EscalatorSoundscape.collect(level, nearby, listener)) {
            wanted.add(emitter.key());
            var sound = loops.get(emitter.key());
            if (sound == null || sound.isStopped() || !manager.isActive(sound)) {
                if (sound != null) {
                    sound.terminate();
                    manager.stop(sound);
                }
                sound = new EscalatorSoundInstance(level, emitter);
                loops.put(emitter.key(), sound);
                manager.play(sound);
            } else sound.update(emitter);
        }
        var iterator = loops.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!wanted.contains(entry.getKey())) {
                entry.getValue().retire();
                if (entry.getValue().isStopped() || !manager.isActive(entry.getValue())) iterator.remove();
            }
        }
    }
}
