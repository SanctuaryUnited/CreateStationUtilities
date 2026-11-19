# Elevators 3.0 — 0.2.0-dev2 implementation report

Development build for Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10 and Java 21. Mod ID remains `createtransit`. Existing version metadata requires the JAR filename `create-transit-0.2.0-dev2.jar`; the source archive retains the established CREATE Transit Alpha 0.1.0 filename.

## Implemented behavior

The new Cabin Kit places a controller or configures an existing controller/guide rail. Activation creates a dedicated model-cabin entity, with no individual wall/floor assembly. The implementation reuses the existing side-mounted guide rails, dedicated marker rails, floor discovery, labels, call buttons, floor request UI and network ownership. New model cabins use their own vanilla entity renderer rather than the old Create contraption visualizer path.

All 20 primary cabins are implemented: 2×2, 3×3, 4×4 and 5×5 external footprints in rear-panoramic, solid enclosed, cylindrical-rear, wood-trim and scenic-glass-box styles. The geometry has a continuous floor, ceiling, walls, interior handrails, control panel and upper/lower light trim. There are three clear interior blocks above the standing surface. The cylindrical style has a segmented curved rear and complete roof/floor surfaces; its collision follows a conservative segmented outline.

Front and optional rear sliding doors animate independently. Solid and glass door styles can be selected independently; each floor has front/rear service and matching/solid/glass landing appearance controls. Cylindrical cabins cannot have rear doors. Rear doors replace rear panoramic glazing. The entrance width is one block for 2×2/3×3 cabins and two blocks for 4×4/5×5 cabins, so the sliding panels stay within the cabin footprint. Controller-installed landing doors are actual registered multiblocks with animated collision and ownership, not decorative planes.

Configuration includes primary/secondary dyes, optional panoramic side windows and curved glazing, registry-backed wood/floor textures, four ceiling-light designs (panel, strips, ring, bulbs), four handrail choices (brushed, dark, accent, none), chime choice, door/motor audio switches, movement speed and opening/closing delays. Eleven vanilla wood finishes are available in the quick cycle; installed block registry IDs can supply other materials. Pale Oak requires a 1.21.1 backport and is not silently substituted.

The Create-style controller screen has four model settings pages, discovered floor configuration, per-side landing installation and passenger requests. All mutations are server validated. Holding the Cabin Kit at a controller shows a footprint/clearance outline. Activation checks the home marker, landing doors, loaded footprint and collisions. Structural changes require deactivation. The controller prevents breaking an active cabin's drive.

A server-owned state machine performs acceleration/deceleration, swept collision checks, exact floor alignment, FIFO/coalesced requests, door closing confirmation, movement interlocks, obstruction reopening, independent arrival doors, loaded-path pause and fault/retry. Repair Retry now refreshes rail/floor discovery before resuming the saved destination. Landing-door completeness is checked at registered floors, so crossing an ordinary block height after a mid-shaft restart does not create a false interlock fault. Model entity and controller configuration, UUID binding, movement snapshots and door progress persist. A missing/unloaded saved cabin is not replaced by a duplicate entity.

Passengers are carried continuously with the moving floor using vanilla entity collision; no teleport animation or exposed moving platform is used. The entity synchronizes appearance, orientation and door progress through registered entity data, with client position interpolation. This is a dedicated cabin entity, not a general-purpose Create block contraption: arbitrary placed blocks and their block entities are not captured or transported inside it. Cabin decoration is supplied by the model settings.

Existing metal textures and their normal/specular maps are reused. Glass has a separate translucent pass; light trim uses full-bright vertices. This does not create dynamic world illumination. Two new original synthesized door-hiss and motor recordings are included with a CC0 notice and reproducible generator. Existing original Otis/Schindler-styled arrival recordings are reused; these are not manufacturer recordings. No separate close chime was added.

## Source architecture

| Area | Main classes/files |
| --- | --- |
| Validated configuration and production mesh | `elevator/model/CabinConfiguration`, `CabinGeometry` |
| State machine and persistent world adapter | `CabinMotionController`, `ModelCabinController`, `CabinNbt` |
| Moving cabin, synchronization and collisions | `ModelCabinEntity`, `CabinCollisions`, `mixin/ElevatorCabinCollisionMixin` |
| Landing animation, ownership and collision | `ModelLandingBlock`, `ModelLandingEntity` |
| Placement and active controller protection | `ModelCabinItem`, `ModelCabinEvents`, `client/CabinPlacementPreview` |
| Native rendering | `client/ModelCabinRenderer`, `ModelLandingRenderer`, `ElevatorClient` |
| Existing integration upgraded | `ElevatorRegistry`, `ElevatorDriveEntity`, `ElevatorDriveBlock`, `ElevatorGuideRailBlock`, `ElevatorPartEntity`, `ElevatorMenu`, `ElevatorPackets`, `client/ElevatorScreen` |
| Catalog/audio/resources | `inventory/TransitInventory`, `ModSounds`, item/block JSONs, recipe, language entries, sounds |
| Validation and reproducible previews | `tests/elevator3/CabinCoreTest`, `Elevator3GameTests`, `InventoryClientValidation`, `tools/elevator3/ExportCabinAssets.java`, `tools/render_elevator3_assets.py` |

## Compatibility and migration

Gondola, station utilities, escalator/travelator models, dynamic placement, powered LED behavior, inventory icons and call-button height/link data are retained. Against the recovered alpha.6 source, 3,377 existing resource files remain byte-identical; the only existing resource edits are English language entries, sounds.json and the collision mixin list. The 46 deleted resources are exclusively the intentionally retired LRT recipes. The new Station Utilities elevator section exposes Cabin Kit, controller, guide rail, dedicated marker rail and call button. It has five items; the station catalog totals 20 items. Vehicle Kits exposes the gondola family (23 base items, plus the optional cable track when a compatible Steam ’n’ Rails integration is present).

LRT recipes and creative entries were retired in dev1. Its registrations/code/assets and the old assembled-elevator implementation remain for world-save compatibility. Old modular elevator construction items are hidden, not deleted. No destructive world conversion or migration command is included. Return and disassemble an old cabin with its original controller before replacing its physical blocks with a new model cabin. Do not remove retained compatibility code until a migration covering chunks, contraption NBT, inventories, schematics and unloaded entities has been implemented and tested. Historical kit guides are retained as historical documentation.

The packet protocol for the new elevator screen is versioned; use this same mod build on client and server. Steam ’n’ Rails remains optional for the cable-track integration. Native checks here excluded that optional runtime with the project's existing `-PwithoutRailways` option; they do not certify its runtime compatibility.

## Validation

Final fresh-world native Minecraft run: **all 152 required tests passed**, including all five added model-elevator tests and the retained LRT compatibility circuit. The Gradle build, pure core/geometry checks, existing transit checks and native CLIENT registration/inventory data validation passed. Native tests used Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 with the optional Railways runtime excluded. Exact evidence is recorded in `ELEVATORS3_VALIDATION.txt`.

Earlier iterations exposed and fixed repaired-floor refresh and false interlock faults at non-landing block heights. Some earlier full runs also hit the pre-existing intermittent LRT circuit timeout (including a diagnostic with the new elevator fixtures excluded). The final clean-world run passed that circuit; this is not a claim that its intermittency was diagnosed or fixed.

Core tests cover 144 permitted size/style/front/rear settings across all 20 families, serialization, geometry footprint bounds with open and closed doors, request ordering, upward/downward travel, obstruction reopening, unloaded-path pause, fault interlocks and snapshot restoration.

Five added native Minecraft tests cover all 20 actual registered cabin entities and vanilla floor collision; four mounting directions, closed-door collisions, nondefault appearance NBT and actual registry-aware entity-data packet codec; moving controller NBT replacement rebinding the same cabin UUID; two simulated server-player riders plus an entity rider on upward/downward trips with rear-only service; and broken-rail fault/repair/resume without teleporting. The resizing check confirms no old 5×5 landing fragments survive a 2×2 replacement. Simulated server-player actors have fake network handlers; these are not real connected clients.

The native CLIENT data environment validates the actual registered entity renderer provider, landing renderer registration, legacy Flywheel visualizer, native inventory mixins, catalog layout and 3,072 tint cases. It does not open a graphical Minecraft window.

Software previews export the same `CabinGeometry` used by the shipped renderer: 20 primary meshes, 184 distinct structural/door/glazing cases and 19 finish examples. Geometry and output SHA-256 hashes are included in the preview archive. The release model manifest was regenerated to include the new kit icon model and landing particle model; all 897 reachable model resources are packaged, while 1,888 unused generator models stay source-only. These are actual asset renders, not AI concept art. The 19 finish examples are four lights, four handrails and eleven woods; they are representative finishes, not an exhaustive Cartesian product of every dye/material/light setting. Vanilla textures are used only in rendered images, not distributed as loose texture assets.

## Remaining certification and restrictions

- Actual graphical GUI interaction, glass sorting, lighting and rendering on a real client still require in-game testing. Shader metal reflections, Sable/Colorwheel and Complementary Reimagined are not certified by these headless checks.
- Real connected multiplayer passengers, latency/interpolation and dedicated-server visual behavior need manual validation. The new renderer avoids the prior legacy contraption path, but this is not a claim that the user's modpack invisibility report has been reproduced and proven resolved.
- Controller replacement/NBT and network-codec tests passed; an actual server shutdown/restart, chunk unload/reload under travel and remote client reconnection still need an end-to-end test. Unloaded path space pauses the controller; it does not force-load an entire shaft.
- No GPU FPS benchmark was run. Geometry is cached, but no hardware performance claim is made.
- This model system does not move user-built block cabins or arbitrary block entities. Use the preserved legacy assembly workflow for that feature.
- Full LRT/legacy code removal and automated world migration are unfinished. This is a development release, not certification of every item in the original overhaul brief.

## Build and reproduction

Use a full JDK 21. Run `./gradlew -PwithoutRailways build`, then `./gradlew -PwithoutRailways runGameTestServer`. For client registration checks run `./gradlew -PwithoutRailways -PinventoryValidation runInventoryData`. `elevator3CoreCheck` runs the pure core/geometry suite. Use a fresh generated GameTest world for a full regression run; previous failed runs can leave saved cabin/train entities in the reused test world. Standard dependency/asset downloads need network access. Private build proxy/truststore and headless asset substitutions used in this environment are not packaged.

To export geometry, compile `CabinConfiguration.java`, `CabinMotionController.java`, `CabinGeometry.java` and `tools/elevator3/ExportCabinAssets.java` into one directory, then run `java -cp <directory> ExportCabinAssets build/elevator3-assets`. Install NumPy, Pillow and the DejaVu fonts; set `MINECRAFT_CLIENT_JAR` to your Minecraft 1.21.1 client JAR, then run `python tools/render_elevator3_assets.py`. The preview uses the existing software rasterizer in this source tree and approximates glass; it is not a shader screenshot.

See [building guide](ELEVATORS3_BUILDING_GUIDE.md) and [original audio notice](ELEVATOR3_AUDIO_LICENSE.md). The previous dev1 report is retained in `ELEVATORS3_DEV1_HISTORY.md`.
