# Elevators 3.0 — development checkpoint 0.2.0-dev1

**Incomplete implementation. This checkpoint is not the requested finished Elevators 3.0 update.**

## Inspected project

Baseline: Create: Transit alpha.6. Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10+mc1.21.1, Java 21. The existing optional Steam ’n’ Rails compile API uses Modrinth project L3Jv0QZI, version HOPX5oro.

- `registry/ModBlocks`, `ModItems`, `ModCreativeTabs`: gondola and station registrations and creative tabs.
- `inventory/TransitInventory`: shared creative catalog and section headings.
- `escalator/`: dynamic regular/redstone escalators and level travelators, shared family placement, LED state, reusable rendering.
- `block/`: platform barriers, fares, gondola cabins and connected two-channel dye.
- `elevator/ElevatorRegistry`: rails, marker rails, call buttons, modular walls/doors, drive, menus and old cabin entity registration.
- `ElevatorRailDiscovery`, `ElevatorNetwork`, `ElevatorLinking`, `ElevatorFloor`: connected rail scanning, labels, linking, floor data and raised call-button support.
- `ElevatorDriveEntity`: queue, floor configuration, motion, interlocks, and persistence, closely coupled to ControlledContraptionEntity.
- `ElevatorContraption` and `ElevatorCabinEntity`: assembled-block capture and Create-controlled moving entity.
- `ElevatorMenu`, `ElevatorPackets`, `client/ElevatorScreen`: existing synchronized configuration and passenger UI.
- `client/ElevatorClient` and `ElevatorRenderer`: entity/block entity registration, animated parts and dye rendering.
- `lrt/`: the remaining removable vehicle kit. No Metro/EMU/HSR production source or asset paths are present in this baseline.

## Changes made

1. Removed LRT from Vehicle Kits and changed that tab's icon to the gondola floor. Removed 46 LRT crafting recipes.
2. Retained existing LRT registrations, models and mechanics temporarily for save compatibility. This is acquisition retirement, not complete vehicle code removal. Gondola and station resources are byte-identical to the baseline (1,782 resource files checked).
3. Added `elevator/model/CabinConfiguration`: 2–5 footprints, five style families, independent door appearance, rear-door restrictions, side window rules, dye indices, materials, lighting/chime selection, and validated versioned serialization. These are settings, not rendered models. Registry-backed material resolution is pending. Pale Oak is not vanilla in 1.21.1; a backport can supply an optional material.
4. Added `elevator/model/CabinMotionController`: pure server-engine core with FIFO/coalesced requests, acceleration/deceleration, exact stopping, independent per-floor door service, obstruction reopening, unloaded-path pause, swept movement authorization, door interlocks, and versioned save/restore. Its world interface is NOT yet connected to a Minecraft entity or controller. The existing in-world system still uses block assembly.
5. Added the missing Flywheel entity visualizer registration for the legacy cabin. Create's normal ContraptionEntityRenderer skips block geometry when visualization is active, so a custom entity needs both registrations. Normal rendering stays enabled for moving block entities and backend-off fallback. This addresses a concrete source defect; shader/Sable compatibility and visual behavior still require runtime checks.
6. Added core tests to Gradle's `check` task and extended native client validation with the legacy elevator visualizer and gondola-only catalog assertions.

## Save compatibility and migration

No registered block, item, block entity or entity IDs are removed in this checkpoint. Existing LRTs and assembled elevator entities retain the code/assets needed to load. Existing guide rail, marker and call-button data formats are untouched.

Full removal requires a separate explicit migration that scans both world chunks and Create contraption NBT, inventories, item components, schematics and unloaded entities. Export old LRT builds with the old version first. Existing assembled elevators must return to their assembly floor and restore their blocks before being replaced with model cabins. An active old elevator must not be silently converted to a different entity representation. Conversion/import commands, reports, rollback and user-facing migration notices remain to be implemented. Do not remove the retained compatibility code before that work is tested.

## Validation

`tests/elevator3/CabinCoreTest.java` compiles with Java 21 and passes:
- 144 permitted size/style/front/rear setting combinations across all 20 primary families; serialization round trips.
- Cylindrical rear-door rejection; rear panoramic window exclusion when rear doors are enabled; solid wall invariants.
- FIFO requests, duplicate suppression, upward/downward stops and front/rear floor policies.
- Obstruction reopening, unloaded-path freeze, moving snapshot serialization and resumption.
- Swept-movement rejection, open-door travel fault, duplicate floor IDs and unknown destinations.

These tests use a deterministic fake world. They do not test Minecraft collision, passenger transport, networking, entity rendering or actual chunk unloading.

Full Gradle `build` passed, including the new core regression task and the existing transitCheck. Native CLIENT data generation passed, including Flywheel visualizer registration and the gondola-only catalog assertions. This is registration/logic validation, not a rendered window. No new model-based cabin entity is in-world yet. No new multiplayer, passenger, shader or hardware performance tests were run. The previous 147 GameTests were not rerun for this checkpoint. Private proxy/truststore and empty-headless-assets build settings are not packaged.

## Exact work still missing

- Dedicated Minecraft cabin entity, server/controller integration, network payloads, interpolation and real passenger/collision adapter.
- All 20 production 3D cabin models, curved geometry, materials, lighting, glass sorting and shaders. No new cabin model assets are delivered here.
- Animated independent solid/glass front/rear doors and matched landing geometry, sensor/obstruction integration.
- Placement item, preview, footprint checks and activation workflow without block assembly.
- New configuration screen and server-validated settings packets, block-material lookup (including optional Pale Oak), preview, floor side/appearance settings.
- Persistent controller/entity binding, ownership, reload/restart reconciliation, chunk lifecycle and world-save integration for the new engine.
- Audio selection/playback integration, licensed/original motor and door sounds, ambience.
- Tested legacy world migration and final removal of LRT code/assets and old cabin construction inventory.
- Native entity, multiplayer/dedicated server and graphical tests; Complementary Reimagined/Sable/Colorwheel compatibility and performance verification.

## Build instructions

Install a full JDK 21 (including javac). From the source root run `./gradlew -PwithoutRailways build`. This still downloads the optional Steam ’n’ Rails compile API. Add `-PinventoryValidation` to enable `runInventoryData`. Run `./gradlew elevator3CoreCheck` for the new core regression suite. Maven/Modrinth and NeoForge/Minecraft artifacts must be reachable.

For an independent core-only check, compile the two classes under `src/main/java/com/sanctuarygames/createtransit/elevator/model` and `tests/elevator3/CabinCoreTest.java` with javac, then run `CabinCoreTest`. This does not compile the Minecraft mod.
