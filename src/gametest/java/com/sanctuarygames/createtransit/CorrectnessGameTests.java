package com.sanctuarygames.createtransit;

import com.mojang.serialization.JsonOps;
import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.elevator.model.*;
import com.sanctuarygames.createtransit.radio.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

/** Roadmap items 7, 8 and 10 (0.5.1). */
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class CorrectnessGameTests {
    private static void check(boolean b, String m) { if (!b) throw new GameTestAssertException(m); }

    private static ElevatorDriveEntity drive(GameTestHelper h, BlockPos relative) {
        var l = h.getLevel();
        var p = h.absolutePos(relative);
        l.setBlock(p, ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorPartBlock.FACING, Direction.SOUTH), 3);
        var d = (ElevatorDriveEntity) l.getBlockEntity(p);
        d.onLoad();
        for (int i = 1; i <= 13; i++) {
            var q = p.above(i);
            l.setBlock(q, (i == 1 || i == 13 ? ElevatorRegistry.FLOOR_MARKER_RAIL : ElevatorRegistry.GUIDE_RAIL).get().defaultBlockState().setValue(ElevatorPartBlock.FACING, Direction.SOUTH), 3);
            ((ElevatorPartEntity) l.getBlockEntity(q)).onLoad();
        }
        d.rescan();
        return d;
    }

    /** Item 7: a controller copied with its data gets its own identifier; the original keeps its own. */
    @GameTest(template = "elevator2_shaft", batch = "correctness")
    public static void copied_controller_gets_its_own_identifier(GameTestHelper h) {
        var l = h.getLevel();
        var original = drive(h, new BlockPos(5, 2, 5));
        var id = original.identifier();
        var at = h.absolutePos(new BlockPos(15, 2, 5));
        l.setBlock(at, ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorPartBlock.FACING, Direction.SOUTH), 3);
        var copy = (ElevatorDriveEntity) l.getBlockEntity(at);
        copy.loadWithComponents(original.saveWithoutMetadata(l.registryAccess()), l.registryAccess());
        check(copy.identifier().equals(id), "Fixture: the copy arrives with the original's identifier");
        copy.onLoad();
        check(!copy.identifier().equals(id) && original.identifier().equals(id), "Copy re-identifies, original keeps its identifier");
        var again = copy.identifier();
        copy.onLoad();
        original.onLoad();
        check(copy.identifier().equals(again) && original.identifier().equals(id), "Reloading keeps both identifiers stable");
        h.succeed();
    }

    /** Item 8: the reused landing probe always matches a freshly built cabin after every change. */
    @GameTest(template = "elevator2_shaft", batch = "correctness")
    public static void cached_landing_lookup_tracks_every_change(GameTestHelper h) {
        var d = drive(h, new BlockPos(9, 2, 9));
        var m = d.model() == null ? null : d.model();
        check(d.configureModel("size", "3"), "Size");
        m = d.model();
        int floor = d.anchor().getY();
        String[][] steps = {{"nudge", "east"}, {"nudge", "north"}, {"rotate", "clockwise"}, {"mirrorIndicators", "true"}, {"rearEnabled", "true"}, {"nudge", "reset"}};
        for (var step : steps) {
            m.landingPosition(floor, false); // warm the cache before the change
            check(d.configureModel(step[0], step[1]), "Change accepted: " + step[0] + "=" + step[1] + " " + m.warning());
            var fresh = new ModelCabinEntity(ElevatorRegistry.MODEL_CABIN.get(), h.getLevel());
            fresh.configure(m.config()); fresh.bind(d); fresh.mirrorIndicators(m.mirrorIndicators()); fresh.setPos(m.placementOrigin(floor)); fresh.updateBounds();
            var p = fresh.world(.5, 0, -.500001);
            var expected = BlockPos.containing(p.x, floor + 1, p.z);
            check(m.landingPosition(floor, false).equals(expected), "Landing after " + step[0] + "=" + step[1] + ": " + m.landingPosition(floor, false) + " vs " + expected);
            check(m.landingPosition(floor + 12, false).equals(expected.above(12)), "Another floor reuses the probe correctly after " + step[0]);
        }
        h.succeed();
    }

    /** A running cabin swaps between models that share its footprint; rounded still needs a deactivated cabin. */
    @GameTest(template = "elevator2_shaft", batch = "correctness")
    public static void running_cabin_changes_model_and_glass(GameTestHelper h) {
        var d = drive(h, new BlockPos(5, 2, 5));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        check(d.configureModel("size", "3"), "Size");
        for (var f : d.floorList()) check(d.installModelLanding(player, f.y()), "Landing: " + d.model().warning());
        check(d.activateModel(), "Activate: " + d.model().warning());
        var m = d.model();
        var cabin = m.entity();
        for (var v : new String[]{"REAR_PANORAMIC", "SCENIC", "SOLID", "REAR_PANORAMIC"}) {
            check(d.configureModel("variant", v), "Running cabin accepts model " + v + ": " + m.warning());
            check(cabin.configuration().variant().name().equals(v), "The cabin entity shows model " + v);
        }
        check(d.configureModel("leftGlass", "true") && cabin.configuration().leftGlazed(), "Left glass shows on a rear panoramic cabin");
        check(!d.configureModel("variant", "CYLINDRICAL"), "Rounded needs a deactivated cabin");
        check(cabin.configuration().variant() == CabinConfiguration.Variant.REAR_PANORAMIC, "A refused change leaves the cabin as it was");
        // Swappable models must not move a single wall, or a rider could end up inside one.
        for (boolean legacy : new boolean[]{false, true}) {
            java.util.List<CabinGeometry.Box> first = null;
            for (var v : new CabinConfiguration.Variant[]{CabinConfiguration.Variant.SOLID, CabinConfiguration.Variant.REAR_PANORAMIC, CabinConfiguration.Variant.SCENIC}) {
                var props = CabinConfiguration.defaults(3, v).save();
                props.setProperty("legacyGeometry", "" + legacy); props.setProperty("v13Geometry", "" + legacy);
                props.setProperty("leftGlass", "true"); props.setProperty("rightGlass", "true");
                var boxes = CabinGeometry.collision(CabinConfiguration.load(props), 0, 0);
                if (first == null) first = boxes;
                else check(first.equals(boxes), "Model " + v + " keeps the same walls (legacy=" + legacy + ")");
            }
        }
        h.succeed();
    }

    /** Item 10: the speaker block codec keeps each style instead of always decoding a box speaker. */
    @GameTest(template = "empty", batch = "correctness")
    public static void speaker_codec_keeps_its_style(GameTestHelper h) {
        for (var block : java.util.List.of(RadioRegistry.SPEAKER.get(), RadioRegistry.CEILING.get(), RadioRegistry.COLUMN.get(), RadioRegistry.HORN.get())) {
            // Decoding would construct a block, which a running game forbids, so check what is encoded.
            var json = StationSpeakerBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, block).getOrThrow().getAsJsonObject();
            var style = json.has("style") ? json.get("style").getAsString() : "box"; // box is the default and may be left out
            check(style.equals(block.style), "Style is encoded: " + block.style + " -> " + json);
        }
        h.succeed();
    }
}
