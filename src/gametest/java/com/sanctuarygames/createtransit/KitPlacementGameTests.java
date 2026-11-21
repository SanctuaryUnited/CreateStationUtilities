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
 *  "the cabin stays exactly where the ghost preview was"; "gold marks the entrance"). Kits are aimed
 *  from the doorway: the clicked block is in front of the door and the cabin is straight ahead. */
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
            // One quadrant per facing, with room for the cabin straight ahead of the doorway.
            var clicked = h.absolutePos(switch (facing) {
                case NORTH -> new BlockPos(7, 2, 20);
                case EAST -> new BlockPos(4, 2, 6);
                case SOUTH -> new BlockPos(20, 2, 4);
                default -> new BlockPos(24, 2, 18);
            });
            index++;
            var stack = new ItemStack(ElevatorRegistry.CABIN_ITEM.get(), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setYRot(facing.toYRot());
            var hit = new BlockHitResult(Vec3.atCenterOf(clicked.below()), Direction.UP, clicked.below(), false);
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
            ElevatorRegistry.CABIN_ITEM.get().useOn(context);
            var planned = Direction.from2DDataValue(ModelCabinItem.plan(stack).getInt("Facing"));
            var kit = ModelCabinItem.layout(l, clicked, planned);
            var preview = ModelCabinItem.previewCabin(ModelCabinItem.plannedDrive(l, kit.target(), kit.driveFacing()));
            var previewBox = preview.getBoundingBox();
            var previewEntrance = ModelCabinItem.entranceBox(preview);
            ElevatorRegistry.CABIN_ITEM.get().useOn(context);

            var mount = kit.drive();
            check(l.getBlockEntity(mount) instanceof ElevatorDriveEntity, "Controller placed in the blue outline, facing " + planned);
            var d = (ElevatorDriveEntity) l.getBlockEntity(mount);
            // Aimed from the doorway: the clicked block is right in front of the landing's middle column,
            // the landing is where the preview outlined it, and the cabin is straight ahead.
            var home = d.model().landingPosition(d.anchor().getY(), false);
            check(home.equals(kit.landingBase()) && d.model().cabinFacing() == kit.landingFacing(), "Landing goes where the preview outlined it, facing " + planned);
            check(ModelLandingBlock.part(home, kit.landingFacing(), 1, 0).relative(planned.getOpposite()).equals(clicked), "Clicked block is the doorway approach, facing " + planned);
            // Level with the floor the player clicked: no step up into the landing or the cabin.
            check(home.getY() == clicked.getY(), "Home landing stands on the clicked floor, facing " + planned + ": y=" + home.getY() + " vs " + clicked.getY());
            check(mount.getY() == clicked.getY() - 1, "Controller sits in the floor block, facing " + planned);
            check(d.installModelLanding(player, d.anchor().getY()), "Home landing installs, facing " + planned + ": " + d.model().warning());
            check(d.activateModel(), "Kit cabin activates, facing " + planned + ": " + d.model().warning());
            var cabin = d.model().entity();
            check(Math.abs(cabin.getY() + CabinGeometry.FLOOR_TOP - clicked.getY()) < 1e-6, "Cabin floor is level with the clicked floor, facing " + planned + ": " + (cabin.getY() + CabinGeometry.FLOOR_TOP));
            check(same(cabin.getBoundingBox(), previewBox), "Cabin stays where the preview was, facing " + planned + ": " + cabin.getBoundingBox() + " vs " + previewBox);
            check(same(ModelCabinItem.entranceBox(cabin), previewEntrance), "Doors open on the gold preview side, facing " + planned + ": " + ModelCabinItem.entranceBox(cabin) + " vs " + previewEntrance);
            var ahead = cabin.getBoundingBox().getCenter().subtract(Vec3.atCenterOf(clicked));
            check(ahead.x * planned.getStepX() + ahead.z * planned.getStepZ() > 2 && Math.abs(ahead.x * planned.getStepZ() - ahead.z * planned.getStepX()) < 0.6, "Cabin is straight ahead of the player, facing " + planned + ": " + ahead);

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

    /** The kit mines the floor block for its controller, but never an unbreakable block or one holding items. */
    @GameTest(template = "elevator2_shaft", batch = "kit_placement", timeoutTicks = 60)
    public static void kit_mines_the_controller_floor_block_only_when_it_may(GameTestHelper h) {
        var l = h.getLevel();
        var player = FakePlayerFactory.getMinecraft(l);
        player.getAbilities().mayBuild = true;
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        var facing = Direction.NORTH;
        player.setYRot(facing.toYRot());
        var clicked = h.absolutePos(new BlockPos(7, 2, 20));
        var kit = ModelCabinItem.layout(l, clicked, facing);
        var floor = kit.drive();
        for (var blocker : java.util.List.of(net.minecraft.world.level.block.Blocks.BEDROCK, net.minecraft.world.level.block.Blocks.CHEST, net.minecraft.world.level.block.Blocks.STONE)) {
            l.setBlock(floor, blocker.defaultBlockState(), 3);
            var stack = new ItemStack(ElevatorRegistry.CABIN_ITEM.get(), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(clicked.below()), Direction.UP, clicked.below(), false);
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
            ElevatorRegistry.CABIN_ITEM.get().useOn(context);
            ElevatorRegistry.CABIN_ITEM.get().useOn(context);
            boolean built = l.getBlockEntity(floor) instanceof ElevatorDriveEntity;
            if (blocker == net.minecraft.world.level.block.Blocks.STONE) {
                check(built, "Ordinary floor block is mined for the controller");
                check(stack.isEmpty(), "Kit used up once built");
                check(!l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(floor).inflate(1), e -> e.getItem().is(net.minecraft.world.item.Items.COBBLESTONE)).isEmpty(), "Mined floor block drops like normal mining");
            } else {
                check(!built && l.getBlockState(floor).is(blocker), blocker + " is never removed for the controller");
                check(stack.getCount() == 1, "Kit kept when refused on " + blocker);
            }
        }
        h.succeed();
    }
}
