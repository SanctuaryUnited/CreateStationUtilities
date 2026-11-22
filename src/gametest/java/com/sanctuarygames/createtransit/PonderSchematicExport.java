package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.elevator.model.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Builds the structures the elevator Ponder scenes show, the way a player would (Cabin Kit, rails, Landing Door Kits,
 * floor calls, a bank), and saves them as structure files plus each cabin's entity data. Runs only with
 * {@code -PponderExport}, which points the {@code transit.ponderExport} property at
 * {@code src/main/resources/assets/createtransit/ponder}. Positions printed here are the ones ElevatorScenes uses.
 */
@net.neoforged.neoforge.gametest.GameTestHolder("createtransit") @net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class PonderSchematicExport {
    private static final String TARGET = System.getProperty("transit.ponderExport");
    /** Floors are this many blocks apart: three rows of landing door, then the floor of the next hallway. */
    static final int FLOOR_SPACING = 4;

    private static void check(boolean b, String m) { if (!b) throw new GameTestAssertException(m); }

    @GameTestGenerator
    public static Collection<TestFunction> exports() {
        if (TARGET == null) return List.of();
        return List.of(
            new TestFunction("ponder_export", "createtransit:ponder_export_elevator", "createtransit:elevator2_shaft",
                net.minecraft.world.level.block.Rotation.NONE, 400, 0, true, PonderSchematicExport::elevator),
            new TestFunction("ponder_export", "createtransit:ponder_export_bank", "createtransit:elevator2_shaft",
                net.minecraft.world.level.block.Rotation.NONE, 400, 0, true, PonderSchematicExport::bank));
    }

    /** One three-floor elevator, doorway facing south toward the viewer, with a floor call beside every landing. */
    static void elevator(GameTestHelper h) {
        var l = h.getLevel();
        BlockPos min = h.absolutePos(new BlockPos(2, 1, 8));
        int width = 9, depth = 9, floors = 3, height = 1 + FLOOR_SPACING * floors;
        var report = new StringBuilder("PONDER EXPORT elevator\n origin ").append(min).append('\n');
        clear(l, min, width, height, depth);
        var player = player(l);
        var built = build(l, player, min, 4, floors, report);
        for (int floor = 0; floor < floors; floor++) call(l, min, 2, built.home.getY() - min.getY() + floor * FLOOR_SPACING, report);
        save(l, min, new Vec3i(width, height, depth), "elevator", report);
        finish(built.drive, min, "elevator", report);
        h.succeed();
    }

    /** Two two-floor elevators side by side on one hallway wall, joined into a bank by the call between them. */
    static void bank(GameTestHelper h) {
        var l = h.getLevel();
        BlockPos min = h.absolutePos(new BlockPos(14, 1, 8));
        int width = 13, depth = 9, floors = 2, height = 1 + FLOOR_SPACING * floors;
        var report = new StringBuilder("PONDER EXPORT elevator_bank\n origin ").append(min).append('\n');
        clear(l, min, width, height, depth);
        var player = player(l);
        var a = build(l, player, min, 3, floors, report);
        var b = build(l, player, min, 8, floors, report);
        int home = a.home.getY() - min.getY();
        BlockPos first = null;
        for (int floor = 0; floor < floors; floor++) {
            var panel = call(l, min, 5, home + floor * FLOOR_SPACING, report);
            if (first == null) first = panel;
        }
        // Wrench menu, "Create bank", on the home floor's shared button.
        String linked = ElevatorBanks.get(l).link(l, first, player, true);
        report.append(" bank ").append(linked).append('\n');
        // A Destination Selector on the right-hand pillar of the second shaft.
        var selector = min.offset(10, home + 1, 6);
        l.setBlock(selector, ElevatorRegistry.DESTINATION_SELECTOR.get().defaultBlockState().setValue(ElevatorPartBlock.FACING, Direction.SOUTH), 3);
        report.append(" selector ").append(rel(min, selector)).append(" associated ").append(ElevatorBanks.get(l).associateSelector(l, selector, player)).append('\n');
        save(l, min, new Vec3i(width, height, depth), "elevator_bank", report);
        finish(a.drive, min, "elevator_bank_a", report);
        finish(b.drive, min, "elevator_bank_b", report);
        h.succeed();
    }

    record Built(ElevatorDriveEntity drive, BlockPos home) {}

    /** A Cabin Kit clicked at (x, 1, 6) facing north, its rail column, hallway ledges, wall pillars and landing doors. */
    static Built build(ServerLevel l, FakePlayer player, BlockPos min, int x, int floors, StringBuilder report) {
        var facing = Direction.NORTH;
        var clicked = min.offset(x, 1, 6);
        var stack = new ItemStack(ElevatorRegistry.CABIN_ITEM.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setYRot(facing.toYRot());
        var hit = new BlockHitResult(Vec3.atCenterOf(clicked.below()), Direction.UP, clicked.below(), false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        ElevatorRegistry.CABIN_ITEM.get().useOn(context);
        var kit = ModelCabinItem.layout(l, clicked, facing);
        ElevatorRegistry.CABIN_ITEM.get().useOn(context);
        check(l.getBlockEntity(kit.drive()) instanceof ElevatorDriveEntity, "Kit placed its controller at x=" + x);
        var d = (ElevatorDriveEntity) l.getBlockEntity(kit.drive());
        report.append(" kit clicked ").append(rel(min, clicked.below())).append(" drive ").append(rel(min, kit.drive()))
            .append(" landing ").append(rel(min, kit.landingBase())).append('\n');

        // Rail column: the kit's floor marker, then plain rails, with a marker at each higher floor.
        var markerState = l.getBlockState(kit.drive().above());
        check(markerState.is(ElevatorRegistry.FLOOR_MARKER_RAIL.get()), "Kit placed the home floor marker: " + markerState);
        var railFacing = markerState.getValue(ElevatorPartBlock.FACING);
        for (int i = 2; i <= 1 + FLOOR_SPACING * (floors - 1); i++) {
            var q = kit.drive().above(i);
            boolean marker = (i - 1) % FLOOR_SPACING == 0;
            l.setBlock(q, (marker ? ElevatorRegistry.FLOOR_MARKER_RAIL : ElevatorRegistry.GUIDE_RAIL).get().defaultBlockState().setValue(ElevatorPartBlock.FACING, railFacing), 3);
            if (l.getBlockEntity(q) instanceof ElevatorPartEntity part) part.onLoad();
        }
        report.append("  rails ").append(rel(min, kit.drive().above())).append(" to ").append(rel(min, kit.drive().above(1 + FLOOR_SPACING * (floors - 1)))).append(" facing ").append(railFacing).append('\n');
        var home = kit.landingBase();
        var slab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int floor = 0; floor < floors; floor++) {
            int y = home.getY() + floor * FLOOR_SPACING;
            // Upper hallways are small ledges in front of the landing door, so they don't hide the shaft.
            if (floor > 0) for (int dx = -2; dx <= 2; dx++) for (int z = 5; z <= 7; z++) l.setBlock(new BlockPos(clicked.getX() + dx, y - 1, min.getZ() + z), slab, 3);
            // Wall pillars either side of the landing door; floor calls mount on them.
            for (int dx : new int[]{-2, 2}) for (int row = 0; row < 3; row++) l.setBlock(new BlockPos(clicked.getX() + dx, y + row, min.getZ() + 5), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);
        }
        d.rescan();
        check(d.floorList().size() == floors, "Every floor marker is a stop: " + d.floorList().size());
        for (var f : d.floorList()) {
            check(d.installModelLanding(player, f.y()), "Landing at Y=" + f.y() + ": " + d.model().warning());
            report.append("  floor ").append(f.y() - min.getY()).append(" landing ").append(rel(min, d.model().landingPosition(f.y(), false))).append('\n');
        }
        return new Built(d, home);
    }

    /** A floor call on the wall pillar at x, facing the hallway, at button height on the floor whose landing starts at y. */
    static BlockPos call(ServerLevel l, BlockPos min, int x, int y, StringBuilder report) {
        var pos = min.offset(x, y + 1, 6);
        l.setBlock(pos, ElevatorRegistry.FLOOR_CALL.get().defaultBlockState().setValue(ElevatorPartBlock.FACING, Direction.SOUTH), 3);
        report.append("  call ").append(rel(min, pos)).append('\n');
        return pos;
    }

    static FakePlayer player(ServerLevel l) {
        var player = FakePlayerFactory.getMinecraft(l);
        player.getAbilities().mayBuild = true;
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        return player;
    }

    static void clear(ServerLevel l, BlockPos min, int width, int height, int depth) {
        for (var p : BlockPos.betweenClosed(min, min.offset(width - 1, height + 2, depth - 1))) l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        // Ponder's base plate: the ground floor.
        for (var p : BlockPos.betweenClosed(min, min.offset(width - 1, 0, depth - 1))) l.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
    }

    /** Activates the elevator so its cabin exists, saves the cabin for the scene to spawn, then deactivates it. */
    static void finish(ElevatorDriveEntity d, BlockPos min, String name, StringBuilder report) {
        check(d.activateModel(), "Activate " + name + ": " + d.model().warning());
        var cabin = d.model().entity();
        report.append(" cabin ").append(name).append(' ').append(cabin.position().subtract(Vec3.atLowerCornerOf(min)))
            .append(" box ").append(cabin.getBoundingBox().move(Vec3.atLowerCornerOf(min).scale(-1))).append('\n');
        saveCabin(cabin, min, name, report);
        System.out.print(report);
        report.setLength(0);
        d.deactivateModel();
    }

    /** The cabin entity, with its position made relative to the structure, for the scene to spawn when it wants. */
    static void saveCabin(ModelCabinEntity cabin, BlockPos min, String name, StringBuilder report) {
        var tag = new CompoundTag();
        cabin.save(tag);
        var p = cabin.position().subtract(Vec3.atLowerCornerOf(min));
        var pos = new ListTag();
        pos.add(DoubleTag.valueOf(p.x)); pos.add(DoubleTag.valueOf(p.y)); pos.add(DoubleTag.valueOf(p.z));
        tag.put("Pos", pos);
        tag.remove("UUID");
        try {
            NbtIo.writeCompressed(tag, Path.of(TARGET).resolve(name + "_cabin.dat"));
            report.append(" saved cabin ").append(name).append("_cabin.dat\n");
        } catch (java.io.IOException e) { throw new GameTestAssertException("Could not save " + name + " cabin: " + e); }
    }

    static String rel(BlockPos origin, BlockPos p) { return p.subtract(origin).toShortString(); }

    static void save(ServerLevel l, BlockPos min, Vec3i size, String name, StringBuilder report) {
        var template = new StructureTemplate();
        // Ponder ignores entities in structures; cabins are saved separately by saveCabin.
        template.fillFromWorld(l, min, size, false, null);
        try {
            var dir = Path.of(TARGET);
            Files.createDirectories(dir);
            NbtIo.writeCompressed(template.save(new CompoundTag()), dir.resolve(name + ".nbt"));
            report.append(" saved ").append(name).append(".nbt size ").append(size.toShortString()).append('\n');
        } catch (java.io.IOException e) { throw new GameTestAssertException("Could not save " + name + ": " + e); }
    }
}
