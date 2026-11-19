# Create: Station Utilities — V15.1

Raised inner-frame direction indicators on cabin and landing doors; fixed floor plaques on landing doors only. See [patch details](docs/V15_1_INSET_INDICATORS.md).

# Create: Station Utilities — V15

See [hallway update and rendering changes](docs/V15_HALLWAY_UPDATE.md), [building guide](docs/ELEVATORS3_SIMPLE_WORKFLOW.md) and [complete report](docs/ELEVATORS3_IMPLEMENTATION.md).

Previous release records follow.

# Create: Station Utilities — V14.1 (`0.2.0-dev14.1`)

Minecraft 1.21.1 · NeoForge 21.1.248 · Create 6.0.10 · Java 21. Mod ID: `createtransit`.

See the [current elevator building guide](docs/ELEVATORS3_SIMPLE_WORKFLOW.md), [V14 cabin and door assets](docs/V14_ELEVATORS3_GUIDE.md), and [current implementation and validation report](docs/ELEVATORS3_IMPLEMENTATION.md).

V14.1 reduces repeated multipart-model baking during client loading and prevents stale server radio metadata from briefly moving an emitter away from its live cabin. It preserves the four V14 skins, four structural models, three selectable door models and existing elevator settings. The source includes the complete original mod architecture, generators and development checks. Historical release notes below describe older builds and are not the current feature selector.

# Create: Transit — Elevators 3.0 dev4

[Current building guide and implementation details](docs/ELEVATORS3_DEV4.md). New: 17 matching cabin/landing door styles, Copycat materials, manually linked two-row landing doors with floor/direction displays, wall-mounted guide rails and configurable idle closing.

Historical development notes follow.

# Elevators 3.0 — model cabin development build (0.2.0-dev3)

A dedicated moving model-cabin system is now integrated with the existing guide rails, marker floors, controller and call buttons. The kit includes 20 primary cabins (four sizes × five styles), independent front/rear sliding doors, owned landing doors, configurable finishes/lights, continuous passenger transport and a server-owned request state machine.

Dev3 keeps both original elevator chimes and adds all six CC0 audio-pack options to the model-cabin selector. [Chime controls and credits](docs/ELEVATORS3_CHIMES_DEV3.md).

Start with the [new building guide](docs/ELEVATORS3_BUILDING_GUIDE.md). Read the [implementation report and certification limits](docs/ELEVATORS3_IMPLEMENTATION.md) and [validation results](VALIDATION.md) before upgrading an existing world. This is a development release; legacy LRT/assembled-elevator IDs are retained for save compatibility. Old release notes below describe historical builds.

# Create Transit

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21.

## Alpha16.4 — Incline workload and visible dye updates

Inclined escalator sections construct the exact occupied collision grid directly and share it between sections with identical motion phases. This removes repeated voxel unions and the costly rotate-and-union path, and caches ray-cast boxes and selection outlines. Moving steps, handrails, glass, both LED trims, sound, and the sectioned inventory remain available. Flat travelator behavior is retained.

Escalator block-entity color packets now invalidate the client's tinted casing and LED meshes. Right-click a section with dye to paint its frame; crouch and right-click with dye to color both inner and outer LEDs. Existing builds do not need to be replaced. See `docs/Escalator-FPS-Dyes-Alpha16.4.md` for installation and verification limits.

## Alpha16.3 — Reusable and instanced escalator rendering

Moving escalators/travelators now reuse visible-surface buffers and use Flywheel instancing when available. The shader fallback emits the same reduced mesh; hidden box faces and subpixel strip sides are omitted. The regression set uses 78.93% fewer moving vertices. Static casings, glass, both LED trims, textures and sounds are retained. Exact collision unions defer intermediate optimization.

The warmed native CPU/allocation comparison is against Alpha16.2, the patch that still caused FPS drops in the user's game. Actual graphical FPS and Complementary shader rendering remain unmeasured. See `docs/Escalator-Performance-Alpha16.3.md` for installation, measurements and scope.

## Alpha16.2 — Escalator performance

Escalators and travelators reuse collision shapes instead of rebuilding them for every collision/outline request. Flat and landing shapes are shared across sections; inclines rebuild only when their actual tick phase or state changes. The renderer shares exact animation geometry between synchronized sections, caches sprite lookups until resource reload, reuses vertex transform destinations and obeys native camera-frustum culling. Client rider updates check the local player directly.

The models, textures, LED trim, full moving geometry, smooth motion, collision surfaces, dyes, sounds and Alpha16.1 inventory layout are retained. See `docs/Escalator-Performance-Alpha16.2.md` for the measured CPU/allocation comparison and verification limits.

## Alpha14.1 — Vehicle kit reset

The old EMU and HSR implementations are removed completely: their blocks, items, entities, models, textures, sounds, recipes, QuickBuilds and generators are deleted from this source and release. Future versions will rebuild those kits from fresh designs. Installing this version into a world with those old blocks leaves missing registrations; old EMU/HSR builds cannot be retained with this release.

The active vehicle kits are Gondola and LRT. Station equipment includes fare gates/readers/kiosks, platform screen doors, independent modular elevators, and glass/solid escalators and travelators. Elevators include floor markers and bank call-button links.

## Alpha16.1 — Creative inventory rework

The creative catalog now has two tabs: **Vehicle Kits** (Gondola and LRT) and **Station Utilities** (Fare Equipment, Platform Screen Doors, Elevators, Escalators & Travelators). Each section starts with a named, full-width cyan divider. Divider and padding rows scroll with the standard creative grid; they are UI decorations, not obtainable items. Search contains only the real kit items. Every dyeable part still has one white entry.

The base catalog contains 69 vehicle items and 42 station items. Installing the optional compatible Steam 'n' Rails integration adds the cable track to Gondola. See `docs/Inventory-Rework-Alpha16.1.md` and `docs/Alpha16.1-Validation.txt`.

## Alpha15.4 — Moving escalator sound

Escalators and travelators play an original, quiet motor/tread loop while their synchronized steps are moving. Each connected belt shares one positional source; opposite-direction lanes remain independent. Redstone stops the loop with a short fade. Breaking/unloading sections, leaving the world, or reloading the sound engine clears stale loops. The Blocks sound slider controls the volume.

The Alpha15.3 inner and outer dyeable LED trim is included. See `docs/Escalators-Sound-Alpha15.4.md` for this patch and `docs/Escalators-Refinement-Alpha15.2.md` for the kit.

## Build and verification

- `./gradlew build -PwithoutRailways`: compile the mod and run timing/network checks.
- `./gradlew elevatorModelCheck lrtModelCheck -PwithoutRailways`: native model and geometry checks.
- `./gradlew escalatorModelCheck -PwithoutRailways`: static and moving escalator mesh checks.
- `./gradlew escalatorSoundCheck -PwithoutRailways`: native client-event registration and loop-resource checks.
- `./gradlew escalatorPerformanceCheck -PwithoutRailways`: compare exact collision unions, selected visible surfaces and instance transforms; benchmark against Alpha16.2, then record warmed native CPU/allocation microbenchmarks.
- `./gradlew runGameTestServer -PwithoutRailways`: test the retained kits and station equipment in Minecraft.
- `./gradlew runInventoryData -PinventoryValidation -PwithoutRailways`: launch NeoForge's client data environment, verify that the inventory mixins apply to the native screen/menu/slots, and export the registered catalog to `build/inventory-validation/inventory-layout.json`.
- `python tools/render_inventory_preview.py`: render an expanded catalog preview from that native export and the shipped item models.
- Generate elevator marker assets with `python tools/generate_elevator_marker_assets.py`.
- Generate the original mono escalator loop with `python tools/generate_escalator_audio.py` (NumPy and ffmpeg).
- Run `python tools/optimize_loading_assets.py` last after asset generators, so the packaged model manifest matches the active kits.

Steam 'n' Rails is optional for the cable track integration; use `-PwithoutRailways` for base-runtime checks. See `LRT_BUILD_GUIDE.md`, `PLATFORM_CUSTOMIZATION_GUIDE.md`, and `docs/Elevator-Floor-Markers-Alpha13.8.md` for retained build guides. Test-only classes are excluded from the release JAR.
