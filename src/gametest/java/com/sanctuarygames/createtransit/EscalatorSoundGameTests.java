package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.escalator.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class EscalatorSoundGameTests {
    @GameTest(template = "escalator_strip", batch = "escalator_sounds", timeoutTicks = 30)
    public static void widened_mixed_belt_has_one_nearest_source(GameTestHelper h) {
        var a = EscalatorGameTests.put(h, 4, 1, 10, EscalatorBlock.Kind.FLAT, false);
        var b = EscalatorGameTests.put(h, 5, 1, 10, EscalatorBlock.Kind.FLAT, false);
        var c = EscalatorGameTests.put(h, 4, 1, 9, EscalatorBlock.Kind.FLAT, false);
        h.getLevel().setBlock(b, EscalatorRegistry.part(EscalatorBlock.Kind.FLAT, false).defaultBlockState(), 3);
        h.runAtTickTime(5, () -> {
            var listener = EscalatorSoundscape.position(b, h.getLevel().getBlockState(b));
            var one = EscalatorSoundscape.collect(h.getLevel(), List.of(a, b, c, a), listener);
            var reordered = EscalatorSoundscape.collect(h.getLevel(), List.of(c, b, a), listener);
            h.assertTrue(one.size() == 1, "Wide mixed glass/solid travelator produces one loop");
            h.assertTrue(one.getFirst().section().equals(b), "Source comes from the nearest deck section");
            h.assertTrue(one.equals(reordered), "Emitter identity and location survive seed-order changes");
            h.succeed();
        });
    }

    @GameTest(template = "escalator_strip", batch = "escalator_sounds", timeoutTicks = 40)
    public static void redstone_removes_sound_and_release_resumes_it(GameTestHelper h) {
        var a = EscalatorGameTests.put(h, 4, 1, 10, EscalatorBlock.Kind.LOWER_LANDING, false);
        var b = EscalatorGameTests.put(h, 4, 1, 9, EscalatorBlock.Kind.INCLINE, false);
        EscalatorGameTests.put(h, 4, 2, 8, EscalatorBlock.Kind.UPPER_LANDING, false);
        var listener = Vec3.atCenterOf(a).add(0, 1, 0);
        h.runAtTickTime(5, () -> {
            h.assertTrue(EscalatorSoundscape.collect(h.getLevel(), List.of(a, b), listener).size() == 1,
                "Running escalator produces a source");
            h.getLevel().setBlock(b.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        });
        h.runAtTickTime(10, () -> {
            h.assertTrue(EscalatorSoundscape.collect(h.getLevel(), List.of(a, b), listener).isEmpty(),
                "Redstone stop removes the entire connected belt's sound");
            h.getLevel().setBlock(b.below(), Blocks.AIR.defaultBlockState(), 3);
        });
        h.runAtTickTime(16, () -> {
            h.assertTrue(EscalatorSoundscape.collect(h.getLevel(), List.of(a, b), listener).size() == 1,
                "Sound returns when moving steps resume");
            h.succeed();
        });
    }

    @GameTest(template = "escalator_strip", batch = "escalator_sounds", timeoutTicks = 30)
    public static void opposite_lanes_remain_independent(GameTestHelper h) {
        var a = EscalatorGameTests.put(h, 4, 1, 10, EscalatorBlock.Kind.FLAT, false);
        var b = EscalatorGameTests.put(h, 5, 1, 10, EscalatorBlock.Kind.FLAT, true);
        var listener = Vec3.atCenterOf(a);
        h.runAtTickTime(5, () -> {
            h.assertTrue(EscalatorSoundscape.collect(h.getLevel(), List.of(a, b), listener).size() == 2,
                "Opposite-direction lanes have independent motors");
            h.getLevel().setBlock(a.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        });
        h.runAtTickTime(10, () -> {
            var running = EscalatorSoundscape.collect(h.getLevel(), List.of(a, b), listener);
            h.assertTrue(running.size() == 1 && running.getFirst().section().equals(b),
                "Stopping one lane preserves the other lane's sound");
            h.succeed();
        });
    }

    @GameTest(template = "escalator_strip", batch = "escalator_sounds", timeoutTicks = 30)
    public static void sound_is_local_and_does_not_load_chunks(GameTestHelper h) {
        var a = EscalatorGameTests.put(h, 4, 1, 10, EscalatorBlock.Kind.FLAT, false);
        var listener = Vec3.atCenterOf(a).add(EscalatorSoundscape.RANGE + 2, 0, 0);
        var unloaded = a.offset(100000, 0, 100000);
        h.runAtTickTime(5, () -> {
            h.assertTrue(!h.getLevel().hasChunkAt(unloaded), "Test probe starts outside loaded chunks");
            h.assertTrue(EscalatorSoundscape.collect(h.getLevel(), List.of(a, unloaded), listener).isEmpty(),
                "Distant and unloaded escalators do not produce a source");
            h.assertTrue(!h.getLevel().hasChunkAt(unloaded), "Sound collection never loads remote chunks");
            h.getLevel().setBlock(a, Blocks.AIR.defaultBlockState(), 3);
            h.assertTrue(EscalatorSoundscape.collect(h.getLevel(), List.of(a), Vec3.atCenterOf(a)).isEmpty(),
                "Removed sections cannot leave a source behind");
            h.succeed();
        });
    }

    @GameTest(template = "escalator_strip", batch = "escalator_sounds", timeoutTicks = 30)
    public static void split_belts_and_reversal_have_stable_sound_identity(GameTestHelper h) {
        var a = EscalatorGameTests.put(h, 4, 1, 10, EscalatorBlock.Kind.FLAT, false);
        var middle = EscalatorGameTests.put(h, 4, 1, 9, EscalatorBlock.Kind.FLAT, false);
        var b = EscalatorGameTests.put(h, 4, 1, 8, EscalatorBlock.Kind.FLAT, false);
        var listener = Vec3.atCenterOf(middle);
        h.runAtTickTime(5, () -> {
            var original = EscalatorSoundscape.collect(h.getLevel(), List.of(a, middle, b), listener);
            var player = TransitTestPlayers.makeMockPlayer(h, net.minecraft.world.level.GameType.CREATIVE);
            com.sanctuarygames.createtransit.escalator.EscalatorTestAccess.reverse(h.getLevel(),a);
            var reversed = EscalatorSoundscape.collect(h.getLevel(), List.of(b, middle, a), listener);
            h.assertTrue(original.size() == 1 && reversed.getFirst().key().equals(original.getFirst().key()),
                "Reversal keeps one loop with the same connected-belt identity");
            h.getLevel().setBlock(middle, Blocks.AIR.defaultBlockState(), 3);
            h.assertTrue(EscalatorSoundscape.collect(h.getLevel(), List.of(a, b), listener).size() == 2,
                "Breaking a belt into two independent runs creates two sound sources");
            h.succeed();
        });
    }
}
