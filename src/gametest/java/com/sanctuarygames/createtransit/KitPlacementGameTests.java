package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.elevator.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

/** A confirmed Cabin Kit builds exactly what its preview showed, in every facing (V10 workflow:
 *  "the cabin stays exactly where the ghost preview was"; "gold marks the entrance"). */
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class KitPlacementGameTests {
    private static void check(boolean b, String m) { if (!b) throw new GameTestAssertException(m); }
    private static boolean same(AABB a, AABB b) {
        return Math.abs(a.minX - b.minX) < 1e-6 && Math.abs(a.maxX - b.maxX) < 1e-6 && Math.abs(a.minY - b.minY) < 1e-6
            && Math.abs(a.maxY - b.maxY) < 1e-6 && Math.abs(a.minZ - b.minZ) < 1e-6 && Math.abs(a.maxZ - b.maxZ) < 1e-6;
    }

    @GameTest(template = "elevator2_shaft", batch = "kit_placement", timeoutTicks = 60)
    public static void kits_build_their_preview_in_all_four_facings(GameTestHelper h) {
        var l = h.getLevel();
        var player = FakePlayerFactory.getMinecraft(l);
        player.getAbilities().mayBuild = true;
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        int index = 0;
        for (var facing : Direction.Plane.HORIZONTAL) {
            var clicked = h.absolutePos(new BlockPos(6 + index % 2 * 14, 2, 6 + index / 2 * 14));
            index++;
            var stack = new ItemStack(ElevatorRegistry.CABIN_ITEM.get(), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setYRot(facing.toYRot());
            var hit = new BlockHitResult(Vec3.atCenterOf(clicked.below()), Direction.UP, clicked.below(), false);
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
            ElevatorRegistry.CABIN_ITEM.get().useOn(context);
            var planned = Direction.from2DDataValue(ModelCabinItem.plan(stack).getInt("Facing"));
            var preview = ModelCabinItem.previewCabin(ModelCabinItem.plannedDrive(l, clicked, planned));
            var previewBox = preview.getBoundingBox();
            var previewEntrance = ModelCabinItem.entranceBox(preview);
            ElevatorRegistry.CABIN_ITEM.get().useOn(context);

            var mount = ModelCabinItem.compactMount(clicked, planned);
            check(l.getBlockEntity(mount) instanceof ElevatorDriveEntity, "Controller placed in the blue outline, facing " + planned);
            var d = (ElevatorDriveEntity) l.getBlockEntity(mount);
            check(d.installModelLanding(player, d.anchor().getY()), "Home landing installs, facing " + planned + ": " + d.model().warning());
            check(d.activateModel(), "Kit cabin activates, facing " + planned + ": " + d.model().warning());
            var cabin = d.model().entity();
            check(same(cabin.getBoundingBox(), previewBox), "Cabin stays where the preview was, facing " + planned + ": " + cabin.getBoundingBox() + " vs " + previewBox);
            check(same(ModelCabinItem.entranceBox(cabin), previewEntrance), "Doors open on the gold preview side, facing " + planned + ": " + ModelCabinItem.entranceBox(cabin) + " vs " + previewEntrance);

            // The controller and its rail column must not stand in front of the doorway's centre column.
            var landing = d.model().landingPosition(d.anchor().getY(), false);
            var doorway = ModelLandingBlock.part(landing, d.model().cabinFacing(), 1, 0);
            for (var column : java.util.List.of(mount, mount.above()))
                for (int depth = 0; depth <= 2; depth++)
                    check(!column.equals(doorway.relative(d.model().cabinFacing().getOpposite(), depth)) && !column.equals(doorway.relative(d.model().cabinFacing(), depth)),
                        "Controller is clear of the doorway, facing " + planned);
            check(d.deactivateModel(), "Cleanup, facing " + planned);
        }
        h.succeed();
    }
}
