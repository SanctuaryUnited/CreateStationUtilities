package com.sanctuarygames.createtransit.elevator.client.ponder;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.elevator.ElevatorRegistry;
import com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Storyboards for the elevator Ponder scenes. Positions match the structures PonderSchematicExport builds:
 * "elevator" is one three-floor shaft (floors 4 blocks apart), "elevator_bank" two two-floor shafts side by side.
 */
final class ElevatorScenes {
    private ElevatorScenes() {}

    // elevator.nbt: kit clicked on (4,0,6); controller in the floor at (7,0,3); rails at x=7, z=3, markers at y=1, 5, 9;
    // landing doors x=3..5, z=5, from y=1, 5, 9; hallway ledges at y=4 and 8; wall pillars at x=2 and 6; calls at x=2.
    private static final BlockPos CLICKED = new BlockPos(4, 0, 6), DRIVE = new BlockPos(7, 0, 3);
    private static final int RAIL_X = 7, RAIL_Z = 3, FLOORS = 3, SPACING = 4;

    private static void setup(SceneBuilder scene, String id, String title, int plate) {
        scene.title(id, title);
        scene.configureBasePlate(0, 0, plate);
        // Three floors are tall: zoom out and lower the view so the top floor stays on screen.
        scene.scaleSceneView(.45f);
        scene.setSceneOffsetY(-2.5f);
        // Look at the hallway side, where the doorways are.
        scene.rotateCameraY(180);
    }

    // ----------------------------------------------------------------------------------------------------------
    // Cabin Kit: preview colours, turning, building.

    static void cabinKit(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "elevator_cabin_kit", "Starting an elevator with a Cabin Kit", 9);
        // Before the kit is built the controller's spot is ordinary floor.
        scene.world().setBlock(DRIVE, Blocks.SMOOTH_STONE.defaultBlockState(), false);
        scene.showBasePlate();
        scene.idle(15);

        var floorTop = util.vector().topOf(CLICKED);
        scene.overlay().showText(80).text("A Cabin Kit builds the bottom of an elevator: its controller, the first floor marker and the cabin")
            .pointAt(floorTop).placeNearTarget().attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(floorTop, Pointing.DOWN, 50).rightClick().withItem(new ItemStack(ElevatorRegistry.CABIN_ITEM.get()));
        scene.overlay().showText(70).text("Face where the shaft should go and right-click the floor where the doorway should be")
            .pointAt(floorTop).placeNearTarget().attachKeyFrame();
        scene.idle(80);

        var cabin = new AABB(3.0625, 1, 2.0625, 5.9375, 3.8, 5);
        var doorway = new AABB(3.2, 1, 4.9, 5.8, 3.1, 5.05);
        var landing = new AABB(3, 1, 5, 6, 4, 6);
        var controller = new AABB(7, 0, 3, 8, 2, 4);
        int shown = 230;
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, "cabin", cabin, shown);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, "doorway", doorway, shown);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, "landing", landing, shown);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, "controller", controller, shown);
        scene.overlay().showText(60).text("A preview shows where everything goes. Gold is the cabin's doorway...")
            .colored(PonderPalette.OUTPUT).pointAt(new Vec3(4.5, 2, 5)).placeNearTarget();
        scene.idle(70);
        scene.overlay().showText(50).text("...white is where the landing door goes...")
            .colored(PonderPalette.WHITE).pointAt(new Vec3(4.5, 3.5, 5.5)).placeNearTarget();
        scene.idle(60);
        scene.overlay().showText(50).text("...and blue is the controller")
            .colored(PonderPalette.BLUE).pointAt(new Vec3(7.5, 1, 3.5)).placeNearTarget();
        scene.idle(60);
        scene.overlay().showControls(floorTop, Pointing.DOWN, 40).rightClick().whileSneaking();
        scene.overlay().showText(50).text("Crouch-right-click to turn the preview").pointAt(floorTop).placeNearTarget().attachKeyFrame();
        scene.idle(60);

        scene.overlay().showControls(floorTop, Pointing.DOWN, 40).rightClick().withItem(new ItemStack(ElevatorRegistry.CABIN_ITEM.get()));
        scene.overlay().showText(50).text("Right-click the same block again to build").pointAt(floorTop).placeNearTarget().attachKeyFrame();
        scene.idle(30);
        scene.world().destroyBlock(DRIVE);
        scene.idle(5);
        scene.world().restoreBlocks(util.select().position(DRIVE));
        scene.world().showSection(util.select().position(RAIL_X, 1, RAIL_Z), Direction.DOWN);
        spawnCabin(scene, "elevator");
        scene.idle(30);
        scene.overlay().showText(80).text("The controller goes in the floor block under the blue outline. The kit mines that block for you, so the cabin floor is level with the hallway")
            .pointAt(util.vector().topOf(DRIVE)).placeNearTarget().attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70).text("Next: build the shaft, add a landing door on every floor, then activate").independent(20);
        scene.idle(80);
    }

    // ----------------------------------------------------------------------------------------------------------
    // Guide rails and floor markers.

    static void shaft(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "elevator_shaft", "Building the elevator shaft", 9);
        scene.showBasePlate();
        scene.world().showSection(util.select().position(RAIL_X, 1, RAIL_Z), Direction.DOWN);
        spawnCabin(scene, "elevator");
        scene.idle(20);

        scene.overlay().showText(70).text("Stack Elevator Guide Rails straight up from the Cabin Kit's floor marker, with no gaps")
            .pointAt(util.vector().centerOf(RAIL_X, 1, RAIL_Z)).placeNearTarget().attachKeyFrame();
        scene.idle(30);
        for (int floor = 1; floor < FLOORS; floor++) {
            int base = floor * SPACING;
            // Plain rails from just above the previous marker up to just below this floor's marker.
            for (int y = (floor - 1) * SPACING + 2; y <= base; y++) {
                scene.world().showSection(util.select().position(RAIL_X, y, RAIL_Z), Direction.DOWN);
                scene.idle(5);
            }
            scene.world().showSection(hallway(util, floor), Direction.DOWN);
            scene.idle(15);
            scene.world().showSection(util.select().position(RAIL_X, base + 1, RAIL_Z), Direction.DOWN);
            scene.overlay().showOutline(PonderPalette.GREEN, "marker" + floor, util.select().position(RAIL_X, base + 1, RAIL_Z), 60);
            if (floor == 1) {
                scene.overlay().showText(80).text("At each higher floor, use an Elevator Floor Marker Guide Rail instead, at the height you stand on that floor")
                    .colored(PonderPalette.GREEN).pointAt(util.vector().centerOf(RAIL_X, base + 1, RAIL_Z)).placeNearTarget().attachKeyFrame();
                scene.idle(90);
            } else scene.idle(30);
        }
        scene.overlay().showText(70).text("Stop at the top floor's marker. The rails don't need to go any further")
            .pointAt(util.vector().centerOf(RAIL_X, 1 + SPACING * (FLOORS - 1), RAIL_Z)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        var shaft = new AABB(3, 1, 2, 6, 1 + SPACING * (FLOORS - 1) + 3, 5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, "shaft", shaft, 80);
        scene.overlay().showText(70).text("Keep the space the cabin travels through clear of blocks")
            .colored(PonderPalette.RED).pointAt(new Vec3(4.5, 7, 3.5)).placeNearTarget();
        scene.idle(90);
        scene.overlay().showText(70).text("Once activated, the cabin stops level with each floor marker")
            .pointAt(util.vector().centerOf(RAIL_X, 1 + SPACING, RAIL_Z)).placeNearTarget().attachKeyFrame();
        scene.addInstruction(new MoveCabin(null, SPACING, 50));
        scene.idle(80);
        scene.addInstruction(new MoveCabin(null, -SPACING, 50));
        scene.idle(60);
    }

    // ----------------------------------------------------------------------------------------------------------
    // Landing Door Kits.

    static void landingDoors(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "elevator_landing_doors", "Adding landing doors", 9);
        scene.showBasePlate();
        showShaft(scene, util, false);
        spawnCabin(scene, "elevator");
        scene.idle(20);

        var homeFloor = util.vector().topOf(CLICKED);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, "landing", new AABB(3, 1, 5, 6, 4, 6), 70);
        scene.overlay().showText(70).text("Every floor needs a landing door. On the home floor, the Cabin Kit's white outline shows where it goes")
            .colored(PonderPalette.WHITE).pointAt(new Vec3(4.5, 3, 5.5)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        for (int floor = 0; floor < FLOORS; floor++) {
            var front = util.vector().topOf(CLICKED.above(floor * SPACING));
            scene.overlay().showControls(front, Pointing.DOWN, 30).rightClick().withItem(new ItemStack(ElevatorRegistry.LANDING_ITEM.get()));
            if (floor == 0) {
                scene.overlay().showText(80).text("Stand in the hallway facing the shaft and use an Elevator Landing Door Kit on the floor in front of the doorway")
                    .pointAt(homeFloor).placeNearTarget().attachKeyFrame();
                scene.idle(40);
            } else scene.idle(20);
            scene.world().showSection(landing(util, floor), Direction.SOUTH);
            scene.idle(floor == 0 ? 50 : 25);
        }
        scene.overlay().showText(70).text("The doors link to the elevator by themselves. If one can't, link it with a Create Wrench")
            .pointAt(new Vec3(4.5, 6, 5.5)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
    }

    // ----------------------------------------------------------------------------------------------------------
    // Activating the controller.

    static void activate(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "elevator_activate", "Activating the elevator", 9);
        scene.showBasePlate();
        showShaft(scene, util, true);
        spawnCabin(scene, "elevator");
        scene.idle(25);

        var controller = util.vector().topOf(DRIVE);
        scene.overlay().showControls(controller, Pointing.DOWN, 40).rightClick();
        scene.overlay().showText(70).text("When every floor has its landing door, right-click the controller to open its panel")
            .pointAt(controller).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(80).text("Check that Floors lists every stop (F1, F2, F3), then press Activate")
            .pointAt(controller).placeNearTarget();
        scene.idle(90);
        scene.overlay().showText(80).text("Doors close before the cabin moves, stay locked while it travels, and open at the floor it stops at")
            .pointAt(new Vec3(4.5, 2, 5)).placeNearTarget().attachKeyFrame();
        scene.addInstruction(new MoveCabin(null, SPACING * 2, 70));
        scene.idle(100);
        scene.addInstruction(new MoveCabin(null, -SPACING, 50));
        scene.idle(70);
        scene.overlay().showText(70).text("If something blocks it, the panel says what to fix, such as a missing rail or a block in the shaft")
            .pointAt(controller).placeNearTarget().attachKeyFrame();
        scene.idle(80);
    }

    // ----------------------------------------------------------------------------------------------------------
    // Floor calls.

    static void floorCall(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "elevator_floor_call", "Calling and riding the elevator", 9);
        scene.showBasePlate();
        showShaft(scene, util, true);
        var cabin = spawnCabin(scene, "elevator");
        scene.world().modifyEntity(cabin, e -> { e.setPos(e.getX(), e.getY() + SPACING * 2, e.getZ()); e.setOldPosAndRot(); });
        scene.idle(25);

        var call = new BlockPos(2, 2, 6);
        scene.world().showSection(util.select().position(call), Direction.NORTH);
        scene.idle(15);
        scene.overlay().showText(80).text("Put an Elevator Floor Call on the wall beside each landing door: within 6 blocks of it, and no more than 2 blocks up")
            .pointAt(util.vector().blockSurface(call, Direction.SOUTH)).placeNearTarget().attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(util.vector().blockSurface(call, Direction.SOUTH), Pointing.LEFT, 30).rightClick();
        scene.overlay().showText(60).text("Press it to call the cabin to this floor")
            .pointAt(util.vector().blockSurface(call, Direction.SOUTH)).placeNearTarget().attachKeyFrame();
        scene.idle(20);
        scene.addInstruction(new MoveCabin(cabin, -SPACING * 2, 70));
        scene.idle(80);
        for (int floor = 1; floor < FLOORS; floor++) scene.world().showSection(util.select().position(2, 2 + floor * SPACING, 6), Direction.NORTH);
        scene.overlay().showText(80).text("Inside, tap a floor on the cabin's touchscreen to ride there")
            .pointAt(new Vec3(4.5, 2, 4)).placeNearTarget().attachKeyFrame();
        scene.idle(20);
        scene.addInstruction(new MoveCabin(cabin, SPACING, 50));
        scene.idle(80);
    }

    // ----------------------------------------------------------------------------------------------------------
    // Banks: two cars sharing hall calls, and the Destination Selector.

    static void bank(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "elevator_bank", "Grouping elevators into a bank", 13);
        scene.scaleSceneView(.55f);
        scene.showBasePlate();
        // elevator_bank.nbt: shafts at x=2..4 and 7..9, rails at x=6 and 11, the shared calls at (5, 2/6, 6),
        // and the Destination Selector at (10, 2, 6).
        var calls = util.select().position(5, 2, 6).add(util.select().position(5, 6, 6));
        var selector = new BlockPos(10, 2, 6);
        Selection everything = util.select().layersFrom(1).substract(calls).substract(util.select().position(selector));
        scene.world().showSection(everything, Direction.DOWN);
        var a = spawnCabin(scene, "elevator_bank_a");
        var b = spawnCabin(scene, "elevator_bank_b");
        scene.world().modifyEntity(b, e -> { e.setPos(e.getX(), e.getY() + SPACING, e.getZ()); e.setOldPosAndRot(); });
        scene.idle(25);

        scene.overlay().showText(70).text("Build each elevator first, side by side, with their landing doors on the same hallway wall")
            .pointAt(new Vec3(6, 3, 5.5)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        var home = new BlockPos(5, 2, 6);
        scene.world().showSection(util.select().position(home), Direction.NORTH);
        scene.overlay().showText(70).text("On the lowest floor they share, put an Elevator Floor Call between two neighbouring landing doors")
            .pointAt(util.vector().blockSurface(home, Direction.SOUTH)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(home, Direction.SOUTH), Pointing.LEFT, 40).rightClick()
            .withItem(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
        scene.overlay().showText(70).text("Use a Create Wrench on it and choose Create bank. For more cars, wrench the button between the next pair and choose Add adjacent shafts")
            .pointAt(util.vector().blockSurface(home, Direction.SOUTH)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.world().showSection(util.select().position(5, 6, 6), Direction.NORTH);
        scene.overlay().showText(60).text("Buttons on the higher floors join the bank by themselves")
            .pointAt(util.vector().blockSurface(new BlockPos(5, 6, 6), Direction.SOUTH)).placeNearTarget();
        scene.idle(70);
        scene.overlay().showControls(util.vector().blockSurface(home, Direction.SOUTH), Pointing.LEFT, 30).rightClick();
        scene.overlay().showText(60).text("A call now goes to whichever car can get there first")
            .pointAt(util.vector().blockSurface(home, Direction.SOUTH)).placeNearTarget().attachKeyFrame();
        scene.idle(20);
        scene.addInstruction(new MoveCabin(b, -SPACING, 50));
        scene.idle(70);

        scene.world().showSection(util.select().position(selector), Direction.NORTH);
        var selectorTop = util.vector().topOf(selector.above());
        scene.overlay().showText(70).text("Optional: an Elevator Destination Selector beside a bank landing lets riders pick their floor first")
            .pointAt(selectorTop).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(60).text("It tells them which car (A, B, ...) to take").pointAt(selectorTop).placeNearTarget();
        scene.idle(70);
        scene.overlay().showText(70).text("The wrench menu also sets the bank's button style, colours and floor names")
            .pointAt(util.vector().blockSurface(home, Direction.SOUTH)).placeNearTarget();
        scene.idle(80);
    }

    // ----------------------------------------------------------------------------------------------------------
    // Shared pieces of the "elevator" structure.

    private static Selection hallway(SceneBuildingUtil util, int floor) {
        int y = floor * SPACING;
        var pillars = util.select().fromTo(2, y + 1, 5, 2, y + 3, 5).add(util.select().fromTo(6, y + 1, 5, 6, y + 3, 5));
        return floor == 0 ? pillars : util.select().fromTo(2, y, 5, 6, y, 7).add(pillars);
    }

    private static Selection landing(SceneBuildingUtil util, int floor) {
        int y = floor * SPACING + 1;
        return util.select().fromTo(3, y, 5, 5, y + 2, 5);
    }

    /** Shows the rails, hallways and, optionally, the landing doors of the "elevator" structure. */
    private static void showShaft(SceneBuilder scene, SceneBuildingUtil util, boolean landings) {
        Selection shown = util.select().fromTo(RAIL_X, 1, RAIL_Z, RAIL_X, 1 + SPACING * (FLOORS - 1), RAIL_Z);
        for (int floor = 0; floor < FLOORS; floor++) {
            shown = shown.add(hallway(util, floor));
            if (landings) shown = shown.add(landing(util, floor));
        }
        scene.world().showSection(shown, Direction.DOWN);
    }

    /** Spawns a cabin PonderSchematicExport saved beside the structure (assets/createtransit/ponder/<name>_cabin.dat). */
    static ElementLink<EntityElement> spawnCabin(SceneBuilder scene, String name) {
        return scene.world().createEntity(level -> {
            var id = ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID, "ponder/" + name + "_cabin.dat");
            try (var in = Minecraft.getInstance().getResourceManager().open(id)) {
                var tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
                return EntityType.create(tag, level).orElseThrow();
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Missing Ponder cabin " + id, e);
            }
        });
    }

    /** Moves a cabin (or, with no link, every cabin) by dy blocks, easing in and out like the real motor. */
    static final class MoveCabin extends TickingInstruction {
        private final ElementLink<EntityElement> cabin;
        private final double dy;
        private double startY;
        MoveCabin(ElementLink<EntityElement> cabin, double dy, int ticks) { super(false, ticks); this.cabin = cabin; this.dy = dy; }

        private void each(PonderScene scene, java.util.function.Consumer<ModelCabinEntity> action) {
            if (cabin == null) scene.forEachWorldEntity(ModelCabinEntity.class, action);
            else scene.resolveOptional(cabin).ifPresent(element -> element.ifPresent(e -> { if (e instanceof ModelCabinEntity c) action.accept(c); }));
        }
        @Override protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            each(scene, e -> startY = e.getY());
        }
        @Override public void tick(PonderScene scene) {
            super.tick(scene);
            double t = 1 - remainingTicks / (double) totalTicks, eased = t * t * (3 - 2 * t);
            // Ponder draws entities between their old and new positions and never updates the old one itself.
            each(scene, e -> { e.setOldPosAndRot(); e.setPos(e.getX(), startY + dy * eased, e.getZ()); });
        }
    }
}
