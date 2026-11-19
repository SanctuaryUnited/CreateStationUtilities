package com.sanctuarygames.createtransit.escalator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** One nearby sound source per connected belt, regardless of its width. */
public final class EscalatorSoundscape {
    public static final int RANGE = 12;

    public record Emitter(BlockPos key, BlockPos section, Vec3 position) {}

    private EscalatorSoundscape() {}

    public static boolean moving(BlockState state) {
        return state.getBlock() instanceof EscalatorBlock
            && !state.getValue(EscalatorBlock.STOPPED);
    }

    public static boolean audible(Level level, BlockPos pos, BlockState state) {
        return moving(state) && level.getBlockEntity(pos) instanceof EscalatorEntity section && section.soundEnabled();
    }

    public static Vec3 position(BlockPos pos, BlockState state) {
        // Put the motor/tread source inside the deck, including diagonal sections.
        double height = EscalatorBlock.kind(state) == EscalatorBlock.Kind.INCLINE ? .65 : .15;
        return Vec3.atLowerCornerOf(pos).add(.5, height, .5);
    }

    public static List<Emitter> collect(Level level, List<BlockPos> nearby, Vec3 listener) {
        var emitters = new ArrayList<Emitter>();
        var visited = new HashSet<BlockPos>();
        for (var seed : nearby) {
            if (visited.contains(seed) || !level.hasChunkAt(seed)
                || !moving(level.getBlockState(seed))) continue;
            var members = EscalatorNetwork.group(level, seed);
            visited.addAll(members);
            if (members.isEmpty()) continue;
            BlockPos key = members.getFirst();
            BlockPos closest = null;
            Vec3 source = null;
            double distance = RANGE * RANGE;
            for (var pos : members) {
                if (pos.compareTo(key) < 0) key = pos;
                var state = level.getBlockState(pos);
                if (!audible(level,pos,state)) continue;
                var point = position(pos, state);
                double squared = point.distanceToSqr(listener);
                if (squared < distance) {
                    closest = pos;
                    source = point;
                    distance = squared;
                }
            }
            if (closest != null) emitters.add(new Emitter(key, closest, source));
        }
        return List.copyOf(emitters);
    }
}
