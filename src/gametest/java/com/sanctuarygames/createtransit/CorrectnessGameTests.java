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

    /** Riders anywhere on the floor, including the front corners beside the doorway, travel with the cabin. */
    @GameTest(template = "elevator2_shaft", batch = "correctness", timeoutTicks = 900)
    public static void riders_in_every_corner_travel_with_the_cabin(GameTestHelper h) {
        var d = drive(h, new BlockPos(5, 2, 5));
        var installer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        check(d.configureModel("size", "3") && d.configureModel("variant", "SCENIC"), "Scenic 3x3");
        for (var f : d.floorList()) check(d.installModelLanding(installer, f.y()), "Landing: " + d.model().warning());
        check(d.activateModel(), "Activate: " + d.model().warning());
        var e = d.model().entity();
        var c = e.configuration();
        double n = c.size(), wall = 2 * CabinGeometry.INSET, front = CabinGeometry.passageEnd(c, false), half = .3, gap = .02;
        // Local positions: both front corners against the doorway jambs, both back corners, and the middle.
        double[][] spots = {{wall + half + gap, front + half + gap}, {n - wall - half - gap, front + half + gap},
            {wall + half + gap, n - wall - half - gap}, {n - wall - half - gap, n - wall - half - gap}, {n / 2, n / 2}};
        var riders = new java.util.ArrayList<net.minecraft.world.entity.LivingEntity>();
        for (int i = 0; i < spots.length; i++) {
            var at = e.world(spots[i][0], CabinGeometry.FLOOR_TOP, spots[i][1]);
            net.minecraft.world.entity.LivingEntity rider;
            if (i == 0) {
                // A server player in the corner the bug was seen in; the rest are armour stands.
                var player = new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "corner-rider"));
                h.getLevel().addNewPlayer(player);
                rider = player;
            } else {
                rider = new net.minecraft.world.entity.decoration.ArmorStand(h.getLevel(), at.x, at.y, at.z);
                h.getLevel().addFreshEntity(rider);
            }
            rider.setPos(at.x, at.y, at.z);
            riders.add(rider);
        }
        int base = d.anchor().getY();
        check(d.requestFloor(base + 12), "Trip requested");
        boolean[] moved = {false}, lagged = {false};
        var late = new net.minecraft.world.entity.decoration.ArmorStand(h.getLevel(), 0, 0, 0);
        h.getLevel().addFreshEntity(late);
        var lateSpot = e.world(n / 2, CabinGeometry.FLOOR_TOP, front + half + gap);
        late.setPos(lateSpot.x, lateSpot.y, lateSpot.z);
        h.onEachTick(() -> {
            var s = d.model().state();
            if (s == null) return;
            if (s.state() == CabinMotionController.State.FAULT) throw new GameTestAssertException(s.fault());
            if (s.state() == CabinMotionController.State.MOVING) {
                moved[0] = true;
                // Walking into the side wall and the front jamb while moving: the walls must stop the rider.
                var walker = riders.get(0);
                var out = e.world(spots[0][0] - .3, CabinGeometry.FLOOR_TOP, spots[0][1] - .3).subtract(e.world(spots[0][0], CabinGeometry.FLOOR_TOP, spots[0][1]));
                walker.move(net.minecraft.world.entity.MoverType.SELF, out);
                var w = e.local(walker.position());
                check(w.x >= wall + half - .01 && w.z >= front + half - .01, "Moving cabin walls stop a walking rider: local " + w + " at y " + s.y());
                var back = e.world(spots[0][0], CabinGeometry.FLOOR_TOP, spots[0][1]);
                walker.setPos(walker.getX() + (back.x - walker.getX()), walker.getY(), walker.getZ() + (back.z - walker.getZ()));
            }
            // A rider whose feet fell behind the rising floor (as a late client update does) is put back on it.
            if (moved[0] && !lagged[0] && s.y() > base + 2) {
                late.setPos(late.getX(), e.getY() + CabinGeometry.FLOOR_TOP - .3, late.getZ());
                lagged[0] = true;
            } else if (lagged[0]) {
                var q = e.local(late.position());
                check(Math.abs(q.y - CabinGeometry.FLOOR_TOP) < .05, "A rider that fell behind the floor is put back on it: local y " + q.y);
            }
            for (int i = 0; i < riders.size(); i++) {
                var q = e.local(riders.get(i).position());
                check(Math.abs(q.y - CabinGeometry.FLOOR_TOP) < .05 && Math.abs(q.x - spots[i][0]) < .05 && Math.abs(q.z - spots[i][1]) < .05,
                    "Rider " + i + " stays on the floor where it stood: local " + q + " expected (" + spots[i][0] + ", " + spots[i][1] + ") state " + s.state() + " y " + s.y());
            }
            if (moved[0] && s.state() == CabinMotionController.State.DOORS_OPEN && Math.abs(s.y() - (base + 12)) < .001) {
                riders.forEach(net.minecraft.world.entity.Entity::discard);
                late.discard();
                check(lagged[0], "Late rider was tested");
                h.succeed();
            }
        });
    }

    /** Cabin walls are found for collision anywhere on the cabin, even far from its anchor corner across a section line. */
    @GameTest(template = "elevator2_shaft", batch = "correctness", timeoutTicks = 40)
    public static void cabin_walls_found_far_from_the_anchor_corner(GameTestHelper h) {
        var l = h.getLevel();
        var cabin = new ModelCabinEntity(ElevatorRegistry.MODEL_CABIN.get(), l);
        cabin.configure(CabinConfiguration.defaults(5, CabinConfiguration.Variant.SOLID));
        var origin = h.absolutePos(new BlockPos(2, 2, 2));
        cabin.setPos(origin.getX(), origin.getY(), origin.getZ());
        cabin.updateBounds();
        l.addFreshEntity(cabin);
        var missed = new java.util.ArrayList<String>();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz += 5) {
            cabin.setPos(origin.getX() + dx + .5, origin.getY(), origin.getZ() + dz + .5);
            cabin.updateBounds();
            // A player-sized box against the far side wall, as far from the anchor as the cabin allows.
            for (var spot : new double[][]{{4.5, 2.5}, {2.5, 4.5}, {4.5, 4.5}}) {
                var p = cabin.world(spot[0], 1, spot[1]);
                var query = new net.minecraft.world.phys.AABB(p.x - .3, p.y, p.z - .3, p.x + .3, p.y + 1.8, p.z + .3);
                if (CabinCollisions.add(l, null, query, java.util.List.of()).isEmpty()) missed.add("anchor+" + dx + "," + dz + " spot " + spot[0] + "," + spot[1]);
            }
        }
        cabin.discard();
        check(missed.isEmpty(), "Cabin collision not found at " + missed.size() + " spots, e.g. " + missed.stream().limit(3).toList());
        h.succeed();
    }
}
