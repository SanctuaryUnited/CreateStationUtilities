# V15.1 — raised inner-frame indicators

Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10, Java 21. Version `0.2.0-dev15.1`, unchanged mod ID `createtransit`.

The arrow modules now use the original black housings on the inner jamb faces. The extra V15 modules on the hallway-facing trim have been removed. Each plate is mounted 1/32 block (half a model pixel) beyond its housing face, toward the opening. Arrow artwork and plaque lettering sit a further finite distance forward to prevent being hidden by the backing.

Both cabin and landing doors have stacked direction arrows. Fixed floor-ID plaques are drawn only by landing doors; the cabin renderer supplies no landing ID and cannot request a plaque. Plaques sit directly beneath the arrows and use the same raised mounting. Header displays stay above both entrances. Their artwork is now fitted to the original black display bounds rather than leaving a surplus black band exposed below it.

All six styles, independent selectors, mirrored header options, lime/red/gray direction signals, per-arrow call-button hover and V15 geometry/material caches remain included. No floor links, shaft layout, collision, door movement or saved cabin settings changed. Existing V15 elevators need no rebuilding.

## Validation

- Gradle build/check passed.
- Native CLIENT mounting assertions cover both jambs, all four cabin sizes and all four frame depths: 32 mounting cases. Plates are beyond the inner housing surface, face the doorway and remain centered in frame depth.
- Explicit plaque-presence assertions reject the cabin's null landing ID and accept G/F2 landing IDs.
- Existing 36 style/NBT combinations, exact split landing geometry, terminal hover selection, 9,216 production mesh configurations, 143,104 multipart states, GUI scales, radio and creative inventory checks passed.
- V15's 187 server GameTests remain prior regression evidence; this client artwork/placement patch does not change movement or server operation. They were not rerun for V15.1.
- The renderer matrix retains 25,184,256 submitted vertices; the curved-body sample remains 7,456 versus the old 12,544. No new hardware FPS claim.

The full-resource native client completed six style views, both call-arrow hover views, and two additional oblique inset close-ups. Inspection confirmed raised inner-jamb arrows and a readable landing F1 plaque, with no plaque below the nearer cabin arrow module. The cabin close-up also sees the separate landing frame farther away; its F1 plaque belongs to that landing. Front views confirm the obsolete front-trim modules and surplus header band are absent. See `V15_1_VISUAL_VALIDATION.log`. Complementary Reimagined and the complete user modpack remain outside the graphical test environment.

Production changes: `DoorIndicatorRenderer`, the landing-only plaque in `StationModelPreview`, and new engine-neutral `DoorIndicatorLayout`; build version. Development changes: placement assertions and two close-up camera views in the full-resource graphical fixture.

---

# Previous implementation reports

# Create: Station Utilities — V15 hallway and rendering update

Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10, Java 21; internal version `0.2.0-dev15`, unchanged mod ID `createtransit`.

## Hallway indicators and call panels

- Six selectable indicator styles: Pixel Matrix, Slimline, Classic LED, Recessed Glass, Transit Sign and Brushed Metal. The header and side/call style selectors are independent. Previous four enum names are unchanged; existing configurations use their previous header style for the new side setting.
- Door indicator surfaces now sit outside their opaque backing on both finished sides. Arrows use actual pixel geometry rather than font glyphs. The mirrored header swaps positions while preserving each arrow's meaning.
- Each landing frame has stationary floor-ID plaques below its paired side indicators. Plaques use the linked landing ID (G, F1, B2, etc.). The header follows the nearest actual elevator floor marker during travel, independently of the landing plaque. Unlinked landings show `--`.
- Side arrows retain lime Up, red Down and gray inactive states, including existing arrival pulses.
- The call panel now draws upright arrow geometry directly. The previous general box helper reversed vertical UV coordinates, flipping the old Up/Down artwork despite correct server floor ordering.
- Hover highlights only the aimed arrow: lime Up or red Down. Other arrows retain their requested or inactive appearance. A pressed effect applies only to the clicked half and transitions to existing request indication. Disabled/fault panels ignore hover.
- The lowest reachable enabled floor offers Up only; the highest offers Down only; intermediate floors show both. Explicit marker links and arbitrary mounting height remain supported. Side/call style follows the owning controller, and frame accent dye remains supported.
- Existing FIFO requests remain floor requests; this does not add multi-car directional dispatch. Indicators remain integrated into landing/cabin door frames rather than separate registered blocks.

## Rendering work reduced

- Bounded immutable geometry cache for cabin bodies and stationary door poses.
- Landing frames and moving leaves render separately: an animation no longer rebuilds and retiles the whole frame.
- Cached client landing configuration, invalidated by block-entity updates, and bounded material-sprite lookups invalidated when the atlas sprite generation changes.
- Rectangular clipped texture tiles submit four vertices instead of two degenerate triangle-quads (eight vertices). Curved clipped polygons retain the triangle-fan path when needed. UVs, normals and animations remain exact; no motion quantization was introduced.
- Surface normals are calculated during tile construction, and opaque/glass passes share one tile lookup.
- V14.1's scoped multipart loading cache and moving-radio position correction remain included.

## Controls and upgrade

Install the new JAR on clients and server, replacing the previous Station Utilities/Transit JAR. Existing V14.1 cabins, floor links and offsets remain saved; no shaft rebuild is required for this patch.

Open the controller's Doors customization page. `Indicator style` chooses the header; `Side / call style` chooses the paired direction modules, plaques and linked call-button design. Scroll to reach both selectors at GUI scale 4. Floor IDs are still edited through Floors, independently of height.

## Validation

- Build/check completed successfully.
- All 187 required dedicated-server GameTests passed, including enabled terminal layouts, side-style propagation and panel state persistence.
- Native CLIENT checks passed 36 independent header/side-style NBT combinations and exact original-versus-split landing geometry at five animation positions.
- The 9,216-case native door geometry emitter submitted 25,184,256 vertices, versus 50,368,512 in V14.1; normalized UV, glass alpha and lighting checks passed. This is the mesh workload, excluding text and the new indicator artwork.
- A 5×5 curved-cabin body sample submitted 7,456 tiled vertices versus the previous fan path's 12,544 (40.6% fewer).
- A bounded 1,500-frame static body-generation/tile-lookup workload measured 157.26 ms for the previous call path and 63.10 ms using the geometry cache in this run. This measures CPU preparation, not total frame time.
- GUI scale 1–4 checks passed 1,088 native controls across 64 viewport cases. Existing audio, tint, inventory order and multipart loading-cache regressions passed.

The first full-resource screenshot pass caught reversed text facing; that issue was corrected before release. The final full-resource Minecraft client produced six style captures plus Up/Down hover captures. The header and both fixed F1 plaques were visually inspected and readable. Actual crosshair hits selected each intended arrow; only Up highlighted lime or Down red in the inspected images. The fixture also verified an Up-only lowest-floor panel and a raised call button explicitly linked to a middle-floor marker. See `V15_VISUAL_VALIDATION.log`. CPU/vertex measurements are renderer-workload evidence, not a promise of a particular GPU FPS increase. See `V15_VALIDATION.log`.

## Boundaries

Complementary Reimagined, the user's full modpack and two-client network latency still require pack testing. Native captures use software OpenGL without the user's shader pack. Shapes, colors and geometry are implemented in Java; the approved AI concept images are not used as texture atlases. Classic styling uses bold pixel lettering; this is not a manufacturer-exact numeric display reproduction.

---

# Historical implementation records through V14.1

# Create: Station Utilities — V14.1 follow-up

Target: Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10, Java 21. Internal version `0.2.0-dev14.1`; mod ID `createtransit`. This patch retains the V14 cabin/door asset range and all previous station systems.

## Changes

- Cache the state-aware multipart bake once per Transit multipart source model and resource load. The previous full-resource client run exhausted its 3 GiB heap while repeatedly compiling identical selector trees. The new cache is limited to Transit blocks using the default rotation and default texture getter. Other namespaces, custom getters and non-default rotations keep the native bake path. New resource model instances have independent caches.
- Use a concurrent selector lookup for the shared Transit baked models so parallel chunk builders can safely populate state selections.
- Resolve moving-cabin radio coordinates from the loaded client cabin immediately when server metadata arrives. An older server position can no longer briefly overwrite the live emitter position before the next sound tick or acoustic-profile publication. Volume, station and other metadata continue to update normally.

## Validation

| Check | Result |
| --- | --- |
| Gradle `build` / checks | Passed |
| Dedicated-server GameTests | All **187** required tests passed |
| Native multipart cache | **143,104** block states across **97** production multipart models preserved the same selected child output as fresh bakes |
| Cache boundaries | Custom texture getters, non-default rotations, other namespaces and fresh reload-model instances bypass the old cached object |
| Parallel chunk builders | Concurrent state-selector population and output checks passed |
| Full graphical client boot | Full production blockstates/models reached the integrated world; **97** multipart builds served **143,007** repeated root-bake requests |
| Real client journey with Sound Physics | **60-block ascent and 60-block descent**; exact client feet-to-floor alignment, unchanged Inside radio source, zero measured emitter error, server door interlocks and linked terminal panels passed |
| Old radio snapshot regression | A real snapshot **3.707033 blocks** behind the loaded cabin was applied; emitter coordinates immediately stayed on the client cabin before another sound tick |
| Local radio controls | Streamer Mode stopped the native radio channel; disabling it and resyncing restored playback |
| Existing native geometry / UI checks | **9,216** configurations / **50,368,512** emitted vertices; all **1,088** controls across **64** viewport cases passed |
| Existing audio checks | PCM buffering, MP3/Ogg decoding, 12 shared readers, ICY/jitter, reconnect and bounded gain checks passed |

The initial strict server rider-in-volume assertion was diagnostic, not a shipping gameplay failure. The passing trip recorded maximum server/client movement-timeline separation of **3.595153 blocks** at speed while the local player remained exactly on the rendered cabin floor. No rider-position forcing was added.

Logs: `V14_1_REGRESSION_VALIDATION.log`, `V14_1_JOURNEY_VALIDATION.log`. The final normal-resource JAR is checked against every compiled production class and included source resource during packaging.

## Scope

Production files: `TransitMultipartBakeMixin`, `MultipartSelectorCacheAccess`, `MultipartBakeMetrics`, `RadioClient`, mixin metadata and build version. New development checks: `MultipartBakeCheck`, `CabinJourneyValidation`, native-client launch option and integration into the existing client validation provider. No cabin collision, movement, floor discovery, door interlock, escalator, travelator, kit registration or asset geometry rewrite was required.

An initial strict server-side rider assertion failed because vanilla client entity interpolation and server player reports represent different movement times. The diagnostic run showed the real client stayed exactly on its cabin floor through both journeys. The final check therefore records and bounds the server timeline difference while independently checking actual client footing, radio continuity and server door interlocks; it does not force or teleport the rider during the journey.

## Remaining limits

The graphical run uses software OpenGL and OpenAL No Output with the actual optional Sound Physics Remastered 1.21.1-1.5.2 mod. It validates native sound sources, position, continuity and controls; it is not a listening test. Complementary Reimagined appearance, multiplayer latency beyond the integrated-server session and the complete user modpack still require gameplay testing. The reference sheets remain the V14 procedural-geometry exports, not shader screenshots.

---

# Historical implementation records through V14

The following records preceding releases. The V14.1 results above supersede their validation limits where explicitly stated.

# Elevators 3.0 / V14 implementation report

## Build target and source

Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10 and Java 21 were verified against the supplied project. Optional Steam 'n' Rails remains the existing pinned dependency; its cable integration was not rewritten. The internal version is `0.2.0-dev14`; mod ID remains `createtransit`.

The supplied `CREATE-Transit-Alpha-0.1.0-Source(1)(1).zip` matched the previous V13 source archive exactly, SHA-256 `4fcfccaf38e2270502275ce84c85a180fbcf62f547767eee1201b183d283f7d2`. That input archive was retained unchanged. Work continued in its existing architecture.

## Implemented

- Four independent cabin skins: Brushed Steel, Modern Dark, Wood Trim and Light Transit. Four current structural models, sizes 2–5, optional supported rear entrances, existing voxel-cylinder option.
- Exactly three selectable door models: Full Glass, Full Solid and Slim Window. Skin is independent of model. The old enum constants remain only as legacy/source compatibility inputs; they normalize to supported models and are not selectable in current controls. Half Glass is removed from the current lineup.
- Shared cabin/landing door mesh, two finished faces, standard sliding default, optional two-speed/left/right behavior, existing timed movement and door interlocks. Glass uses actual openings in the metal mesh. Sliding cutouts retain their dimensions as leaves are clipped into the pockets.
- Original native 16×16 material artwork: 160 base skin/quality tiles, 320 normal/specular companions and 24 touchscreen images. Call images are 16×16 for terminal panels and 16×32 for intermediate panels. Images are generated from pixel code, not cropped from concept art.
- 36 native Minecraft block-model snapshots: 12 skin/model pairs × closed/opening/open. These are reference poses. Actual moving doors use the production Java geometry and 40-tick smooth interpolation; no external animation library is required.
- 192 exported production cabin meshes: four models × four skins × three door models × four sizes. Every mesh includes three cabin-door poses and three landing-door poses. Software preview sheets use that geometry.
- Matched ceiling materials; five current layouts; curved-shell constraints; independent pure-white primary lighting and 16-color LED accents. Default fully enclosed illumination remains level 15. New cabins use the custom checker floor; selected old floor materials remain saved.
- Modern Dark foam strips are above the handrail only. Light Transit lower accents and Dark foam use the secondary dye. Wood wall/door/ceiling sections use the selected block texture.
- Auto Center, reset, half-block and 1/16 X/Z controls. Even-sized mounts now center on a valid block-grid footprint; rotation preserves center. Existing saved positions are not automatically moved.
- Physical Up-only / Up-Down / Down-only touchscreen call panels derived from enabled, reachable floors. Registered request, pressed, hover, disabled and fault visuals. Existing marker links and arbitrary call-button height are preserved. The item model was refreshed too.
- Shared arrival-direction signal: movement arrows, one up flash / two down flashes; terminal arrival uses the available departure direction. Side arrows are lime/up, red/down and gray after the stationary arrival signal. Cabin/landing data synchronizes server-side.
- Stronger, seamless motor loop, retained chimes/hiss and controls. Client elevator-volume multiplier. Music still supports 300% emitter/group gain; local radio volume is an additional 0–100% multiplier.
- Live decoder clock advances without listeners. Shared readers use the same URL timeline, with automatic lag correction and local manual resync. `/csu radiosync` affects only this client.
- Client settings commands `/createstationutilities clientsettings`, `/csu clientsettings`, `/csu settings`; no OP requirement. Saved PBR quality, audio multipliers, radio-only Streamer Mode, Auto Sync, Sync Now, 70–150% station-menu size and 16 dye themes. Volume fields have percentage −/+ controls; existing music/reverb distance controls retain block units.
- Schema 1 → 2 migration keeps floor/link/controller/motion/offset data and saved material IDs. Removed styles map to the closest supported model/skin. Legacy wood-body cabins become Solid + Wood Trim. Legacy RING/BULBS ceiling data remains readable.

## Fixes

- Removed shared exposed faces at side/rear wall joints and front pocket intersections that could fight with frame or glass surfaces.
- Avoided overlapping opaque infills behind door glazing; separated gasket, trim, display and LED planes.
- Preserved true polygon shape when tiling rounded floor/roof fans. World-projected, atlas-bounded texture tiles avoid stretching a single 16px texture over a whole wall or extrapolating fan triangles.
- Moved display text outside its backing surfaces and retained the enlarged merged-header opening. Jamb indicators do not place numbers behind their housings.
- Corrected even-size automatic centering while retaining rail clearance and matching render/collision transforms.
- Preserved default regular sliding rather than forcing compact cabins into telescoping mode.
- Continued live radio time while out of range, retained exhausted-decoder reconnection, and kept the existing Sound Physics periodic source refresh path.
- Made the motor recording louder and seamless rather than repeatedly fading through its loop boundary.

## Completed checks

| Check | Result |
| --- | --- |
| Gradle build/check with release resources | Passed |
| Dedicated-server regression suite | All **187** required GameTests passed |
| Engine-neutral door matrix | **288** configurations, **2,016** poses, **51** legacy migrations passed |
| Ceiling matrix | **320** model/skin/size/layout combinations, lighting on/off passed |
| Indicator/size/lighting persistence matrix | **112** combinations passed |
| Native CLIENT production renderer | **9,216** configurations, **50,368,512** emitted vertices; bounded atlas UVs and valid rounded boundaries passed |
| Official Minecraft block-model parser | All **36** native door snapshots parsed |
| Texture resources | **504** PNGs; native dimensions, glass alpha, separate LED emission and non-emissive foam/accent maps checked |
| GUI viewport checks | **1,088** native controls across **64** viewport cases reachable |
| Graphical captures | **120** real GUI captures at scales 1–4, including new settings/ceilings and 70%/150% size settings |
| Radio HTTP/PCM | MP3/Ogg, ICY metadata, jitter, 12 readers sharing one decoder, EOF reconnect, live clock without listeners, 300% bounded PCM gain passed |
| Sound Physics Remastered 1.21.1-1.5.2 | Native client linkage passed; integrated world exit/re-entry ×3 restored low-pass cutoff **0.049787 → 1.0** and reverb sends |
| Unrelated features | Existing escalator/travelator/door/network tests passed; original inventory tab ordering and tint/icon checks passed |

Logs: `V14_SERVER_VALIDATION.log`, `V14_CLIENT_BUILD_VALIDATION.log`, `V14_GUI_VALIDATION.log`, `V14_RADIO_REENTRY_VALIDATION.log`. GUI captures use deterministic data; integrated acoustic testing uses No Output audio. Neither substitutes for listening or a complete modpack session.

## Main code and resources changed

`CabinConfiguration`, `CabinNbt`, `CabinGeometry`, `DoorGeometry`, `DoorStyleTextures`, `ModelCabinController`, `ModelCabinEntity`, `ModelLandingEntity`, `ModelLandingBlock`, `CabinLightBlock`, `ElevatorCallBlock`, `ElevatorPartEntity`, `ElevatorRegistry`, `ElevatorSounds`, `ElevatorScreen`, `ModelCabinRenderer`, `ModelLandingRenderer`, `ElevatorRenderer`, `MaterialPickerScreen`, `StationUi`, motor client/sound classes, `RadioClient`, `SharedRadioStreams`, `RadioSoundEngineMixin` and client mixin metadata.

New production classes: `IndicatorSignal`, `TiledCabinMesh`, `TouchCallPanelRenderer`, `StationClientSettings`, `StationClientCommands`, `StationSettingsScreen`, `StationUiScaleMixin`.

New generators/exporters: `generate_elevator3_skins.py`, `ExportV14Assets.java`, `export_v14_static_models.py`, `render_v14_preview.py`. Existing audio generator updated. New `elevator3/` texture/model/animation resources and explicit atlas entries. Unrelated escalator, travelator, platform-screen-door, fare and vehicle implementations were retained.

## Validation

Final results are recorded in the included validation logs and release summary. Validation uses the real pinned Minecraft/NeoForge/Create dependencies. Dedicated-server GameTests exercise travel, riders, door safety, persistence, links, terminal layouts and alignment. Native CLIENT validation exercises production vertices, UVs, tint handlers, actual MP3/Ogg HTTP decoding and radio buffering. Graphical GUI tests use a software OpenGL context, real controls and real stitched textures. The sound device is OpenAL No Output: audio was processed, not listened to.

## Limitations / remaining pack tests

- Complementary Reimagined/Iris shader appearance, metal reflections, glass sorting and perceived sound volume still need an in-game check in Jonny's complete modpack. Shader metadata/maps exist; this report does not claim shader certification.
- PBR quality selects elevator texture-map variants. It does not change a shader pack's global settings or promise a measured FPS improvement.
- Custom material support samples a block's particle texture. Arbitrary per-face models, connected-texture rules and another mod's Copycat block-entity material data are not reproduced.
- Call directions are registered visually, while requests coalesce to the linked floor in the existing single-car FIFO controller. Intelligent/directional multi-car dispatch is not implemented.
- Live radio synchronization is local per URL. Different clients can have different network latency. Outages, finite downloads and unsupported AAC/HLS/playlist URLs cannot be made uninterrupted live radio.
- The 36 JSON door models are static reference poses; the runtime mesh/animation is Java. Exported cabin JSON files are production mesh records, not a replacement contraption/GLB entity loader.
- Existing legacy-height cabins keep their saved height until explicitly updated. Existing mounts keep offsets until explicitly centered. Landing positions may need adjustment after a footprint/alignment change.
- A full public-server session, audible listening pass, real player inventory material selection and the complete 160–190-mod pack were not tested here. Native server tests include multiple server-player rider entities; that is not a claim of a two-client gameplay session.


---

## Earlier implementation reports

# V13 / 0.2.0-dev13 — customization and radio update

Implemented the agreed cabin/indicator/rail/material/control scope in the existing
project. No escalator, travelator, fare gate, vehicle kit or tab was rewritten.

## Changes

- CabinConfiguration: persisted IndicatorStyle enum with four choices and old
  constructor overloads/defaults preserved. Existing saves default Pixel Matrix.
- DoorGeometry/CabinGeometry: full-width merged display backing, inside-jamb
  modules, style-specific borders/bold Classic text/Recessed Glass cover, ceiling
  fixture clearance, forward cabin panel planes and matching rear collision.
  Compact cabins no longer force telescoping. Regular leaves are cropped at
  the shell pocket envelope; explicit two-speed uses four leaves.
- DoorIndicatorRenderer: autosized centered floor IDs, mirrored header arrows,
  lime/red inner-frame direction triangles and dim gray inactive states.
  ModelCabinEntity/Controller and ModelLandingEntity now clear the direction
  while stationary, instead of retaining the last travel direction.
- ModelLandingEntity/Block/Controller: style NBT, linking and live propagation
  through the existing validated settings path.
- ElevatorScreen/StationUi/RadioControls: Cabin audio group, translated chimes,
  accessible voxel toggle, indicator selector, volume and distance step buttons,
  and material-picker entry points. MaterialPickerScreen selects inventory or
  Creative block items without moving inventory or bypassing server validation.
- Guide rail/marker/compact driver assets and shapes: one-pixel profiles. New
  driver placement defaults compact; old explicit full-size driver states remain.
  ModelCabinController snug-mount persistence and new Refresher preset rotate
  the entrance away from the mounting wall. ModelCabinEntity's synchronized snug
  transform is used by rendering, inverse coordinates, bounds and collision.
  Existing saved mounts stay put; explicit Snap cabin to rails opts in.
- CabinMotorClient/CabinMotorSound: client-only positioned static-pool loop,
  louder default level, fade-in/out, synchronized on/off/gain from controller,
  24-block attenuation and saved 0–100% motor volume. Dedicated-server checks
  caught an early client-class reference, which was moved out of common code.
- RadioSettings/RadioStations/RadioClient/RadioGainStream: 0–300% music gain,
  bounded PCM amplification per stream reader, group product cap, motion-aware
  Inside eligibility with short exit grace and no stale-source range rejection.
- SharedRadioStreams: retire drained completed sources when no readers remain;
  the next open obtains a fresh decoder/connection. Existing live history,
  source limits, cancellation and retry mechanisms remain.

## Validation

- Final native server run: **all 184 required GameTests passed**; build successful.
- Final normal-resource native CLIENT validation WITH optional SPR: build
  successful. Models, tint/inventory contracts, audio PCM/HTTP continuity and
  optional mixin transformation passed. The extra EOF-reconnect test verified
  a second HTTP connection and fresh audio after the cached source drained.
- GUI: **84 real captures/checks at GUI scales 1–4 passed**, including four
  indicator-style previews with F100 labels, material picker, new Cabin controls,
  color strips, focused E input and long floor-list scrolling. The final extra
  Snap action uses the existing inactive-only mount setting path.
- Core checks: **64 additional style/size/lighting combinations passed** for
  persisted styles, fixture/pocket clearance and normal/optional telescoping
  selection; previous motion, door, network and buffer checks also passed.
- PCM: saved 300% settings, actual 3x sample amplification and bounded positive/
  negative saturation passed, alongside the existing shared twelve-reader tests.
- Package audit verifies every compiled main class and included resource byte,
  source ZIP integrity, metadata, sound references and absence of dev probes.

Logs: V13_NATIVE_VALIDATION.log, V13_CLIENT_SPR_VALIDATION.log and
V13_GUI_VALIDATION.log. Native audio tests do not claim human listening.

The earlier V12.1 Sound Physics channel refresh fix remains intact. Tests with
SPR use the same verified NeoForge 1.21.1-1.5.2 artifact; it is not bundled.

The build environment had lost cached dependency files/assets. Exact existing
project versions of JLayer and Steam 'n' Rails were restored for validation.
Optional `localRadioJar` / `localRailwaysJar` Gradle properties allow explicit
file overrides during validation; the default Maven dependencies remain.
Existing prepared Minecraft artifacts were reused with `-x createMinecraftArtifacts`;
compilation, resource processing, checks, native launch and JAR packaging ran.
The GUI harness's reduced resources are not shipped: the final normal-resource
build and packaging audit verify every compiled main class and packaged resource.

A test fixture that placed an obstruction using world-axis offsets was updated
to use the actual rotated cabin's local center. Assertions were retained. Its
four-turn expectation now reflects the deliberately changed rail-facing preset.
The door mesh test expects a merged header plus two jamb backings instead of
split transom screens. New core tests cover 64 style/size/lighting combinations,
light-pocket clearance, regular/telescoping selection and saved configuration.
New PCM tests verify 3x amplification/saturation and two actual HTTP connections
across a fully exhausted shared decoder.

## Limits and follow-up

Native server travel/collision checks and GUI captures do not certify subjective
audio or the complete mod pack. Moving real-player Inside audio, spatial motor
loudness, shader glass/reflection behavior and connected multiplayer need target
pack testing. The material picker was captured with deterministic menu data;
interactive inventory/Creative selection needs a real-player check. The recovery
fix cannot provide uninterrupted audio when the network supplies no samples.
Regular compact leaves hide their overhang at the pocket boundary; they are not
full-width physical leaves extending through the shaft wall. Indicator styles
share the same layout and differ in housing/display treatment.

---

# V12.1 / 0.2.0-dev12.1 — radio reverb and filter recovery

## Cause and implementation

Verified against the official Sound Physics Remastered 1.21.1 source and
NeoForge 1.21.1-1.5.2 binary. Its default `update_moving_sounds=false` leaves a
long-running sound with its starting environment. The starting context can
include a wall, a distant listener or incomplete arriving chunks. Async radio
startup also outlives SPR's global last-sound identifier.

- New `RadioSoundPhysicsBridge`: NeoForge's PlayStreamingSourceEvent supplies the
  actual channel and sound identity after decoded audio starts. Calls verified
  public SPR `processSound(int,double,double,double,SoundSource,ResourceLocation)`
  on the sound thread. Reflection keeps SPR optional; API failure warns once.
- New `RadioSoundEngineMixin` and `RadioChannelSourceAccess`: capture the current
  native source from the channel handle, then refresh only active Transit radio
  sounds every five ticks when SPR's global periodic updates are off. Work is
  staggered across sound instances. When global updates are on, SPR's own loop
  is used. No global config writes and no cached numeric source IDs or filters.
- `RadioClient`: radio identity predicate, immediate removal/replacement profile
  publication, level-change cleanup and integrated-only custom fade profiles.
- `RadioSoundPhysicsMixin`: processing-distance override accepts/returns the
  actual Double config type instead of looking for Integer. Existing independent
  direct/send gain behavior and genuine room occlusion/reflection are retained.
- `createtransit.mixins.json` registers the two native channel/engine mixins.
  Mod ID and network protocol remain unchanged. No common/server feature logic,
  world data schemas, models, inventory, elevators or escalators were rewritten.

## Native regression and evidence

`RadioReentryValidation` creates a stone room, registers an actual speaker and
local HTTP MP3 fixture, and moves the real client player outside its coverage,
behind the wall and back indoors. The test samples the actual channel on the
sound thread after the blocked position has loaded, then checks recovery after
moving inside. `RadioEnvironmentProbeMixin` records actual SPR environment calls.
`RadioRefreshDisabledMixin` is a development-only negative control. Both probe
mixins are injected into processed resources only for the explicit test flag;
they and their classes are excluded from the release JAR.

Negative control (refresh path disabled): environment calls stayed at **4** and
high-frequency transmission stayed **0.049787067** after returning inside.
This assertion failure is expected and demonstrates the regression. See
`V12_1_RADIO_NEGATIVE_CONTROL.log`.

Patched native run: **three consecutive exits/re-entries passed**. High-frequency
transmission recovered from **0.049787067 → 1.0**, direct gain from
**0.09257846 → 0.68741**, and reverb-send parameter sum returned to **2.6461728**.
Update counts advanced 25→35, 50→61 and 75→85. The real native source stopped
outside coverage and restarted, reusing source ID 1. Global moving updates
remained disabled; the ordinary SPR processing-distance setting was 8 blocks,
so the 14-block blocked sample also checks the corrected range override.
`V12_1_RADIO_NATIVE.log` records the **BUILD SUCCESSFUL** result.

Final normal-resource `build runGameTestServer runInventoryData` without SPR:
**BUILD SUCCESSFUL; all 184 required native server tests passed**. Native CLIENT
resource/model/tint/inventory, PCM, range and acoustic envelope validation passed.
A separate normal-resource `build runInventoryData` WITH official SPR also
**passed**, including real optional mixin transformation. The SoundEngine and
Channel classes are explicitly loaded by the client validator in both cases.
Pure core, door, motion, radio buffer and compatibility checks passed as part of
build. No GUI controls changed in this patch; the prior V12 64-capture GUI run
was not repeated. Logs: `V12_1_BASE_VALIDATION.log` and
`V12_1_SPR_VALIDATION.log`.

Native test invocation: `-PwithoutRailways -PsoundPhysicsValidation
-PsoundPhysicsJar=/absolute/path/to/soundphysics.jar -PradioReentryValidation
-PuiCaptureOnlyResources runClient` against the development test world. Add
`-PradioReentryBaseline` only for the negative control. The GUI-only resource
reduction replaces unrelated Transit block models with air for the test; the
vanilla stone room, actual speaker model, server state and audio are unchanged.
A normal-resource rebuild and package audit restore/check production resources.
The old unrelated ClientRenderValidation harness is now gated by its explicit
clientValidation property so it does not run inside this audio fixture.

## Limits

The native engine used OpenAL's No Output device. We validated real channel
lifetime and applied environment parameters, not sound heard by a person.
Listening with the complete target mod pack and connected multiplayer still
need an in-game check. The optional API target remains SPR NeoForge
1.21.1-1.5.2. All V12 features are retained; see history below.

---

# V12 / 0.2.0-dev12 — speaker acoustics, groups and cabin placement

Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10. Saved-world mod ID remains
`createtransit`; archive/display naming remains Create: Station Utilities.
This adds to the existing project and keeps the legacy features and assets.

## Production implementation

- `RadioRegistry`, `StationSpeakerBlock`, `StationSpeakerEntity`: four registered
  speaker models, six-face placement with corrected UP/DOWN model rotations,
  matching collision/selection bounds, primary/secondary dyes, NBT and update
  packets, plus the shared CabinColors interface for connected primary/secondary
  bulk dyeing. Existing speaker IDs remain valid. New blockstates, block/item models,
  language entries and release-model whitelist entries are included.
- `RadioStations`: world SavedData groups, 64-name cap, validated names,
  persistence, shared station/power/mode/master volume, local coverage/gain.
  `SpeakerMenu` retains menu ownership, distance, permission and update throttle
  checks while validating shared operations and color ranges server-side.
- `RadioNetwork`, `RadioSettings`: discover emitters across their complete
  coverage, validate/clamp direct/reverb radius 4–256 and gain 0–1, retain old
  radius migration, persist all new settings with safe backward defaults.
- `RadioControls`, `StationUi`, `ElevatorClient`, inventory registrations:
  scrolling speaker/group/acoustic controls, separate local/master volumes,
  dye swatches, tints and named variants. Existing stations, caches, stream
  limits, vehicle tab and station tab are retained.
- `RadioClient`, new `RadioAcoustics`: immutable sound-thread source profiles,
  per-source range selection and independent linear fade envelopes, restart
  sources when acoustic/integration mode changes, reuse buffered URL decoding.
- New optional `RadioSoundPhysicsMixin` targets the verified official SPR
  NeoForge 1.21.1-1.5.2 API. It modifies only identified Transit radio sources:
  removes common native rolloff, extends that source's processing distance,
  multiplies four EFX send gains by the quieter reverb envelope, and multiplies
  direct gain separately. Existing SPR occlusion/reflection calculation stays
  intact. The early-return environment applies direct fade. No global SPR
  configuration is changed; there is no bundled or synthetic echo replacement.
  A unique owned `station_radio_stream.ogg` resource identifies radio sources;
  live PCM continues to come from the existing custom stream.
- `CabinConfiguration`, `CabinGeometry`, `ModelCabinEntity`, `ElevatorScreen`:
  optional persisted voxel cylinder, axis-aligned stepped mesh and corresponding
  collision/inside tests. Smooth cylinders remain backward-compatible.
- `ModelCabinController`, `ModelCabinEntity`, `CabinPlacementPreview`,
  `ElevatorScreen`: persisted four-way rotation, inactive-only server setting,
  center-preserving transform independent of rail/driver orientation, rotated
  landing matching/installation and compact-width world preview. Rotate controls
  appear in both alignment editors. Existing half-block nudge controls remain.
- Static block collisions no longer veto generated-model activation, in-place
  geometry upgrades or vertical sweep. World blocks are never removed. Other
  cabin overlap, chunk/world bounds, guide continuity, passenger doorway checks
  and landing interlocks remain enforced. Legacy assembled block contraptions
  and manual landing overwrite protection are unchanged.

## Validation

- Final normal-resource Gradle `build runGameTestServer runInventoryData`:
  **BUILD SUCCESSFUL**, all **184 required native server GameTests passed**.
  Includes rotated static-block assembly/travel, all four orientations,
  connected speaker dyes, group save/reload and voxel collision tests.
- Pure core checks passed: dynamic short/long trips, 144 cabin combinations,
  FIFO requests, accurate alignment, obstruction reopening, unload recovery,
  204 door style/layout/size combinations and 24 mechanism/frame/speed sets.
- Native CLIENT validation passed without SPR: resource/model/tint/inventory
  checks, streaming PCM regression checks and direct/reverb envelope math.
- Graphical client WITH official SPR enabled: **BUILD SUCCESSFUL**; all **64
  GUI checks/captures at scales 1–4 passed**. Includes Rotate reachability,
  voxel preview, groups, focused E typing, 24-floor scrolling and dye strips.
  Source and config detection ran against the installed official mod.
- Final normal-resource CLIENT validation WITH official SPR: **BUILD SUCCESSFUL**;
  its real SoundPhysics class transformed with the optional Transit mixin.
  Independent direct/reverb gain envelopes and non-radio source isolation passed.
- Release packaging audits compare every compiled main class and packaged
  resource to the project outputs, then verify source ZIP paths/contents and
  sound references. Screenshots are development checks, not generated art.

Logs: `V12_VALIDATION.log`, `V12_GUI_VALIDATION.log`, `V12_SPR_VALIDATION.log`.
The release is rebuilt using normal resources after the graphical harness's
capture-only resource reduction, so that reduction is not shipped.

Native server tests exercise the real registered blocks, block entities,
serialization and movement adapter. Graphical captures use real production
widgets/atlas geometry and deterministic menu data; they are not multiplayer
or full gameplay sessions. The optional SPR client starts on OpenAL's No Output
device in this environment; no audible listening claim is made.

Test additions: `SpeakerUpdateGameTests` verifies persisted groups, independent
local coverage/gain, ceiling mounts/dyes and voxel geometry/collision.
`CabinAlignmentGameTests` verifies all driver facings, four center-preserving
turns, inactive/live rules, saved orientation, unchanged rail/driver state,
assembly with a real static block inside the bounding volume, rotated landing
matching and actual controller travel without removing that block.
`RadioAcousticsCheck` exercises source isolation, direct/wet envelopes and
optional official class transformation. `ElevatorUiCapture` includes Rotate,
voxel cylinder and group views alongside previous typing/scrolling/dye checks.

The train circuit GameTest fixture now has a fixed chunk-aligned origin,
independent of test-grid growth. No production train code or assertion was
removed. Inventory test expectations were updated for the three extra speakers.

Optional integration runtime flag for source validation:
`-PsoundPhysicsValidation -PsoundPhysicsJar=/absolute/path/to/official.jar`.
Verified optional artifact: `sound-physics-remastered-neoforge-1.21.1-1.5.2.jar`,
SHA-256 `108cac1fe49d0dc210183d69787db5a359cdc03c5b580eb8ab0208be7f072982`.
API references: [official 1.21.1 source](https://github.com/henkelmax/sound-physics-remastered/tree/1.21.1)
and [official release project](https://modrinth.com/mod/sound-physics-remastered).
SPR is not bundled and is not a mandatory dependency. Base server validation
uses `-PwithoutRailways`, omitting only optional Steam 'n' Rails cable tests;
production cable classes still compile. See the saved V12 logs for commands.

## Remaining limitations

- Listen to real live streams in the target pack to tune direct/reverb gains and
  confirm audible reflections in your room geometry. No Output OpenAL validates
  initialization rather than sound heard by a person. Other SPR versions are not
  certified by this build.
- Connected multiplayer, shaders and the complete third-party mod pack have not
  been exercised here. No sample-perfect cross-client synchronization claim.
- Horn/column/ceiling are model variants with radial coverage. Groups offer
  join/create/leave, not group deletion/renaming or building-wide dispatch.
- Rotation changes which entrance needs landing doors. Reposition/reinstall
  landings at each floor after rotating; unrelated world blocks are protected.
- Removing static-block checks does not remove solid blocks or their ordinary
  collisions with players. Build an open riding passage even though assembly is
  no longer rejected for sharing cells with a rail or shaft decoration.

---

# V11.1 / 0.2.0-dev11.1 — in-place cabin upgrade and dye palette

Added **Setup → Update Cabin**, using the existing validated configuration
packet/menu permission path. ModelCabinController.updateCabin preflights the
new geometry and landing coordinates before committing compact configuration.
It requires a loaded, empty cabin at an aligned idle/open stop, with no queued
or active requests. Occupancy includes the doorway. Failed preflight restores
the previous configuration. Repeated updates are harmless. Entity UUID,
controller identity and position, motion state, floor records, radio, policies
and all appearance settings are retained. Existing linked landing offsets are
resynchronized without recreating doors, buttons or controllers. This is an
in-place geometry update, not an automatic rail relocation or a conversion of
legacy assembled block contraptions.

Replaced both numeric dye selectors in ElevatorScreen with named color strips.
StationUi draws each swatch and contrasting text in a single widget render pass;
the existing smoothly scrolling picker draws all 16 matching dye strips. Native
Minecraft dye IDs remain unchanged in saves and packets. Tooltips show names.

New native tests cover standard and wide entrances in all four orientations,
occupied/pending request refusal, entity/controller/floor/landing identity,
appearance and movement-state preservation, repeat updates and persistence.
Graphical checks include Update Cabin reachability and named palettes at GUI
scales 1–4. See V11_1_VALIDATION.log and V11_1_GUI_VALIDATION.log for final results.
Final results: build/check passed; **179 required native server tests passed**,
including both conversion tests with occupied and pending-request rejection.
Native CLIENT model/tint/inventory/audio checks passed. All **56 graphical UI
checks at GUI scales 1–4 passed**, including Update Cabin reachability and named
dye strips. Package validation checks compiled bytes, metadata, resources and
source ZIP integrity. Shader appearance and connected multiplayer still require
target-pack checks.

---

# V11 — compact rail-first elevators / 0.2.0-dev11

Display name and Gradle archive: Create: Station Utilities /
`create-station-utilities-0.2.0-dev11.jar`. The saved-world mod ID remains
`createtransit`. Dependencies remain MC 1.21.1, NeoForge 21.1.248 and Create 6.0.10.

## Implementation

- ModelCabinItem: rail click creates a default cabin and a slim driver below
  the column, selecting the clicked/nearest enabled marker. Failed creation rolls back the generated driver. Existing
  shaft clicks open settings without duplicating an active cabin.
- CabinConfiguration: persisted compact flag, default false for older saves;
  source-compatible old constructors retained. New refresher cabins enable
  compact and default max speed 0.6. Existing saved geometry stays unchanged.
- ModelCabinController/Entity: shaft-internal mounting transform, 2.5-block
  envelope, shared render/collision height mapping, accurate coordinate
  inversion, cabin-first spawn while travel still requires matching manually
  placed landings, and lowest-enabled home floor for new mounts.
- CabinGeometry/DoorGeometry/ModelCabinRenderer: half-block cabin door pockets,
  telescoping wide panels, rear pocket containment, compressed header/roof,
  floor/arrow display positioning and pure-white ceiling lights.
- CabinLightBlock/registry: itemless invisible level-15 real light source;
  air-only placement, non-collision/empty selection shape, scheduled expiry
  checks cabin occupancy after removal and reload. Light maintenance updates
  every five ticks and expiry checks every ten. No forced chunks.
- CabinMotionController: start acceleration ramp and distance-based braking;
  speed setting stays the maximum. Exact final position is applied to the
  collision adapter before the engine enters arrival state. Grounded riders also
  receive a sub-milliblock correction when vanilla Entity.move skips a tiny
  leveling displacement, preventing an ascending floor from penetrating their
  feet. Gravity remains enabled; normal collision sweeps remain in use.
- ElevatorScreen/SpeakerScreen: consume focused-field key presses before the
  container inventory-close action; all floor rows scroll, smooth main-panel
  motion, smooth picker scrolling and draggable scrollbar, cursor-scoped scrolling, GUI scale support.
- RadioNetwork/RadioClient/SharedRadioStreams: nearest-first bounded discovery,
  32-block prewarm margin, silent Inside-mode prewarming, 12 active emitters,
  one connection/decoder per URL, independent bounded PCM cursors, 30-second
  warm retention, cleanup on level changes and backoff for failed warmups.
  Six-second startup buffering and V10.1 PCM continuity fixes retained.

## Validation

- Gradle build/check and native NeoForge CLIENT validation passed, including
  shared-URL twelve-reader PCM continuity, one HTTP connection, immediate warm
  re-entry, MP3/Vorbis/ICY/bursty-network regressions, 256-block native sound
  attenuation, inventory sorting, model emitter/tints and GUI widget reachability.
- All **177 required server GameTests passed**. The six new tests use a shaft
  fixture tall enough for the full travel sweep. The compact trip test manually
  places real Landing Kits and keeps gravity enabled through an up/down round
  trip, checking the rider's feet against the physical floor every tick.
- Pure core checks passed for dynamic short/long peak speeds, exact arrival,
  persistence, queues, unload pauses, collision and door interlocks; existing
  door style/mechanism and escalator checks passed.
- Real graphical UI validation at scales 1–4 checks E typing and scrolling
  through 24 floors; the last of 24 destinations is reachable by dragging the scrollbar, and the
  style picker schedules smooth motion at each scale. All 52 graphical checks
  completed successfully; see V11_GUI_VALIDATION.log.
- The final packaged JAR is checked against compiled classes and source resource
  bytes. All existing assets/data remain byte-identical to V10.2 except the drive
  blockstate and Refresher display name; compact driver/light resources are new.

Logs: V11_VALIDATION.log and V11_GUI_VALIDATION.log. Test code includes native
four-facing enclosed-shaft creation, duplicate prevention, 2.5-height and
3×3-fit checks, marker discovery, actual light creation/expiry, compact
front/rear pocket bounds, manual landing placement and passenger travel,
short/long motion profiles, and twelve shared station readers over HTTP.

## Practical limits

Old cabins are intentionally not automatically resized or repositioned. The
refresher-created driver needs one clear block directly below the rail column;
rails do not need extension pieces. An obstructed footprint fails in chat.
New default cabin uses the existing narrow standard opening; wide kit settings
remain available. Manual landing kits retain their existing two/three-block
registration footprints. The 3×3 shaft dimension is external cabin clearance,
not a 3×3 clear interior. Rail mounting consumes a 3/16-block wall-side strip.

Shared radio playback preserves independent spatial emitters. Late readers
start at the current four-second OpenAL queue origin; readers that fall more
than eight seconds behind advance to retained history. Warm re-entry avoids
reconnection but a cold/unavailable URL or lost source chunk can still delay
playback. Format support remains direct MP3 and Ogg Vorbis streams.

No Complementary Reimagined/Sound Physics audible/shader, connected multiplayer
or target-pack FPS certification is claimed. Inset door appearance, subjective
ride/scroll comfort and shader-specific exposure still need user-pack testing.

---

# V10.2 — radio range / 0.2.0-dev10.2

Radio and wall/ceiling speaker ranges now support 4–256 blocks, with 256 as the
new-emitter default. The server validation, saved-state clamp, shared slider
mapping/narration and native Sound attenuation distance all use the same limits.
A one-time RangeSchema migration expands old saved 64-block maximum settings to
256, preserving smaller saved values. New settings include the schema, so a
player explicitly selecting 64 now keeps 64 on reload.

A live radius edit replaces its current sound instance, ensuring OpenAL receives
the new attenuation distance rather than retaining the old distance until a
manual restart. The existing startup prebuffer applies to that replacement.
Nearby cabin radio follows the cabin entity when tracked, and otherwise retains
its updated server-advertised coordinates instead of stopping at the visual
tracking boundary. Inside-only mode retains its presence/interior check.

Server snapshots retain the configured range plus the existing eight-block
handoff margin; client retention keeps its four-block margin. Native linear
attenuation fades the direct sound toward zero at the selected radius. This
patch does not force-load source chunks or extend visual/entity tracking. A
speaker/controller must remain loaded. Sound Physics' own reverb/occlusion rules
remain controlled by that mod and have not been audibly certified here.

The four active streams per client, 32 advertised-source bound, V10.1 PCM buffering
fix, radio format support, doors, elevator movement and station assets are retained.
Changed classes: RadioSettings, RadioNetwork, RadioClient, RadioControls and
StationUi; build version, range regressions and docs updated. Common range logic
is exercised by server tests and the production Sound factory by native CLIENT
checks. RadioNetwork.snapshot exposes the existing validated server collection
step separately from sending, so its actual distance filtering can be tested.

Validation passed using the installed Minecraft 1.21.1 / NeoForge 21.1.248 /
Create 6.0.10 dependencies and Java 21:

- Gradle build/check completed successfully.
- All 171 native server GameTests passed, including new range persistence,
  old-maximum migration, all integer slider round trips, and actual loaded
  speaker advertisement at 65, 128, 255, 256 and 263 blocks (handoff margin),
  rejection at 265, disabled-source and unpowered-source filtering.
- Native NeoForge CLIENT validation passed: Sound attenuation at 4, 16, 64,
  128 and 256, streaming/location preservation, clamp and slider endpoints.
- Existing real local HTTP MP3/Vorbis continuity, ICY metadata, bursty-network
  playback with zero underruns, cancellation, GUI scales 1–4, inventory sorting,
  tint/model-emitter and registration checks also passed.

The server distance test keeps the source chunk loaded and uses a FakePlayer;
it does not certify connected multiplayer reception or unloaded chunk behavior.
See RADIO_V10_2_VALIDATION.log for the complete validation run.
No external-station listening, Sound Physics reverb, shader or connected
multiplayer certification is claimed. Earlier results below belong to their
respective releases.

---

# V10.1 — radio continuity fix / 0.2.0-dev10.1

The V10 PCM queue held four 8192-byte decoder chunks (about 0.37 seconds of mono
44.1-kHz audio). Installed Minecraft 1.21.1 Channel.attachBufferStream requests
four one-second buffers immediately. V10 filled missing bytes with silence, so
normal decoder-chunk boundaries could become large audible gaps. The original
short-file decoder smoke test did not exercise Minecraft's real startup/read pattern.

Replaced that queue with RadioPcmBuffer: an eight-second bounded, frame-aligned
PCM ring, with six seconds prebuffered before exposing a live stream to Minecraft.
The initial four one-second reads retain about two seconds of reserve. Available
partial audio is returned directly without silence padding. A quarter-second
silent fallback exists only when a live buffer is actually empty, to keep the
OpenAL source alive through a genuine interruption. Network/decode backpressure
and startup waiting remain off the game/audio threads.

RadioAudio also strips ICY metadata before MP3/Vorbis decoding when servers send
it despite the metadata-off request. MP3 read sizes are exactly frame aligned.
Closing cancels prebuffering promptly; blocking decoder/network cleanup runs in
a separate bounded daemon executor. Producer failures can drain buffered audio
before the existing reconnect path. RadioClient cancels abandoned startup futures
without reporting a user stop as a connection error. The public-station URL list
still starts empty and is preserved in existing worlds.

## Patch validation

- Gradle build/check and native NeoForge CLIENT validation passed.
- New pure PCM regression reproduces Minecraft's four immediate one-second reads;
  verifies exact sample order, a two-second reserve, ring wrapping, bounded producer
  backpressure, partial reads without padding, genuine underrun recovery, short EOF
  and canceled startup waits.
- Real HTTP tests decode twelve-second MP3 and Ogg fixtures. Plain and ICY-interleaved
  routes produce byte-identical complete PCM. Four startup reads contain full seconds.
- A paced bursty HTTP route, including recurring 200-ms delivery gaps, produces
  nine consecutive one-second buffers (four startup + five paced refills), each
  byte-identical to the reference PCM, with zero underruns. Reads stay below the
  test's 100-ms upper bound and never wait for network data.
- A stalled HTTP response is canceled during startup within the one-second test
  deadline, without waiting for its network timeout.
- Existing 144 cabin/core, 204 door-layout, 24 Door Kit 3, 4608 production renderer,
  1088 scale-layout widget and native inventory/tint checks passed in this build.
- Production textures, models, blockstates, recipes and sounds remain byte-identical
  to V10. No GUI/world rendering or elevator mechanics change in this patch.

The 169 server GameTests and 52 graphical captures below were completed for V10;
this client-audio patch does not claim a rerun of those server/gameplay captures.
No audible test of the user's actual public station, Sound Physics reverb,
connected multiplayer or full modpack was available. These tests establish PCM
continuity for the reproduced buffering failure and controlled network/metadata
conditions; a stream/network outage can still interrupt audio. Direct MP3/Vorbis
only remains the supported format. Starting a live station intentionally buffers
several seconds before playback.

Changed: RadioAudio, RadioClient, new RadioPcmBuffer, generated test tones,
RadioAudioCheck/RadioBufferCheck, validation hook, build version/check registration
and release docs. See RADIO_V10_1_VALIDATION.log for the actual build output.

---
Earlier release history follows.

# V10 — Create: Station Utilities / 0.2.0-dev10

This release implements the attached GUI modernization requirements in the
existing project and includes the pending compact-mount, inset-indicator,
half-block-alignment and marker-to-marker rail fixes. The public display name
and redesigned interfaces use Create: Station Utilities. Internal `createtransit`
IDs, dependency ranges, old save keys and development JAR naming are preserved.

## Implemented behavior

The controller uses a reusable navy/cyan theme with authored tab icons, rounded
pixel borders, gradients, illuminated selection, styled toggles, popup dropdowns
and volume/radius sliders. Seven functional categories wrap to two rows on larger
logical windows, or one icon row with named tooltips on compact windows. All
pages scroll with fixed footer actions; server updates retain the scroll position.
Floor rows retain service controls and server-owned Go requests, with a drawn
pencil icon instead of Edit text. Floor names/IDs remain independent of height.
The existing server menu, permission/reach checks and action validation remain
responsible for movement and configuration. Landing Kit use on a linked entrance
opens the same Doors category, preventing separate incompatible door mechanisms.

New confirmed Cabin Kits generate the controller two blocks inward from the old
corner and add only the home marker above it. A saved compensating baseline keeps
the cabin at its original preview location. Reset restores that baseline. Existing
shafts are not relocated. Half-block nudge buttons use exact eight-pixel deltas;
legacy saved one-pixel offsets keep their exact position. Fractional landing
render and dynamic collision offsets now match the cabin rather than rounding
half-block adjustments into its volume. Explicit landing realignment is available
while deactivated, with existing clearance/build-permission checks.

Removed rail-top-versus-roof validation. Both model movement and the legacy real
Create contraption path check rail continuity only through destination markers.
Physical cabin/roof collision checks and loaded-chunk requirements remain intact.

Door settings add Standard/Flush/Recessed/Protruding depth choices (0.5/1/0.75/0.625
blocks), Center/Left/Right slide, and single-/two-speed panels. Telescoping panel
positions and collisions share one layout function and use separate depth lanes;
fully open panels clear the opening. New Cabin Kits opt into the three-high
entrance with a two-wide/two-high opening. Existing saves retain their two-high
layout. Size-2 compact cabins keep their smaller legacy entrance. Larger cabin
sizes keep wall infill around the central entrance. Changing the entrance layout
requires matching manually placed/replaced landing doors before activation.

Floor and arrow text use two actual recessed transom pockets above the moving
leaves, on opposite sides and both faces. They remain visible when open. Mirror
and visibility options persist and synchronize. New three-high frames use larger
readable display pockets; older thin headers retain compact pockets. The GUI mesh
previews use the production cabin/landing geometry and indicator renderer.

The optional radio is functional, with a world SavedData station registry that
starts empty, guarded server actions, persistent cabin/speaker settings, and a
wall/ceiling speaker block with recipe, loot, model and inventory entry. Nearby
emitters synchronize to clients; the server never downloads stream audio. MP3 uses
an unmodified nested JLayer dependency; Vorbis uses Minecraft's installed decoder.
Both feed bounded mono PCM buffers through the verified NeoForge SoundInstance
getStream hook into Minecraft's normal positioned Blocks sound channel. Background
network/decoder work does not block the game or audio thread. Four client streams,
16 world stations and 4–64 block radii bound resource usage. Errors go to chat with
rate limits. The status display describes the client mixer. Speaker redstone and
cabin Nearby/Inside modes are functional. Existing nine chime choices are retained.

## Modified and added areas

- ElevatorDriveEntity, ModelCabinController, ModelCabinItem and placement preview:
  shaft validation, generated mount, alignment, saved state and controller snapshot.
- CabinConfiguration, DoorGeometry, CabinGeometry, DoorStyleTextures:
  compatible schema defaults, mechanisms, frame depths, displays and collisions.
- ModelCabinEntity, ModelLandingBlock/Entity/Item and both renderers:
  shared door options, three-row opt-in, fractional offsets and live indicators.
- ElevatorScreen, StationUi, StationModelPreview, DoorIndicatorRenderer,
  RadioControls and SpeakerScreen: production interfaces and shared components.
- ElevatorMenu/Packets, RadioSettings, RadioStations, RadioNetwork, RadioClient,
  RadioAudio, RadioRegistry and StationSpeakerBlock/Entity/Menu: validated settings,
  persistent sources, bounded client playback and speaker interaction.
- CreateTransit, ElevatorRegistry/Client, ModSounds and TransitInventory:
  required registrations only. Existing creative tabs/order and catalog families remain.
- Resources: seven authored GUI icons, speaker texture/model/blockstate/recipe/loot,
  English labels, streaming sound declaration, JLayer jar-in-jar metadata and license.
- Build/tests: private capture profile, graphical capture harness, live decoder
  fixtures, expanded native geometry, alignment, rail, persistence and scale checks.

## Validation and limits

Gradle build, all 169 required native server GameTests, the 144-case motion/config
core, 204 existing door configurations and 24 new mechanism/depth/speed cases passed.
Native CLIENT checks passed 4608 render configurations (9621504 vertices), 1088
widget placements across 64 scale/resolution viewports, actual registrations,
creative tab order/mixins and dye tints. Local HTTP tests decoded real generated
MP3 and Ogg stereo fixtures into mono 44.1-kHz/16-bit PCM, checked nonblocking
buffer reads and closed the streams. Test tones are not in the production JAR.

Graphical captures use production screens/widgets and atlas meshes with
repeatable demonstration data in a software OpenGL Minecraft client. The
capture-only resource profile omits unrelated Transit world-block models to fit
the workspace's memory limit. It does not alter the GUI/cabin meshes and is not
used for the final JAR. Initial full graphical launches exhausted the environment's
memory budget; these are not successful full-game runtime checks. All 52 final
captures completed successfully and the representative controller, floor editor,
radio, compact scale-4 navigation, dropdown and scrolled 3D previews were inspected.
See ELEVATORS3_V10_VALIDATION.txt and the screenshot archive.

No connected multiplayer click/latency certification, external public station
listening test, Sound Physics reverb test, Complementary shader test, hardware FPS
claim or full modpack certification is made. Positioned Minecraft audio exposes
normal sound events to compatible processing mods, but actual reverb compatibility
must be checked with Sound Physics installed. Radio supports direct MP3/Vorbis
URLs only: AAC, HLS, playlists, authenticated streams, Now Playing metadata and
optional audio-zone dispatch are not implemented. No stations/music/manufacturer
recordings are bundled. The existing model-cabin restrictions and chunk-loading
requirements remain. Source ZIP includes the guide, changelog and test report.

---
Earlier release history follows; old descriptions refer to their own versions.

# Elevators 3.0 — 0.2.0-dev9 / Sneak + Break / Place linking

Manual linking now uses ordinary remappable Minecraft actions with both hands
empty. Sneak+Attack/Break selects a floor marker without mining it. Sneak+Use/Place
links a complete landing or adds a call-button marker link. Repeated Use is
idempotent. Sneak+Attack/Break on a button clears its manual links without removing
the block, restoring automatic detection. Sneak+Attack on a controller can select
it for the existing controller-link fallback. Automatic linking and saved manual
links remain supported; legacy wrench shortcuts are optional, not required.

Added ElevatorLinkingEvents and a validated source-selection helper. Native
NeoForge interaction events honor the player's normal action bindings; no custom
key registration, physical key polling or tool requirement was introduced.
Reach, build permission, loaded-controller and landing-completeness checks run
server-side. Client prediction/mining is canceled for the handled empty-hand
selection gesture; held dyes, blocks and tools keep their existing interaction.
Feedback uses chat. Updated linking hints. Sneak+Use on landings is now reserved
for manual linking, so Copycat glass layout can be chosen through the existing
Customize page; it now propagates to owned linked landings.

Build, all 164 required native Minecraft GameTests, GUI scale 4 checks for 72
native widget placements, and 4608 production render configurations passed.
New tests exercise registered native event handlers, selection, door linking,
button idempotence/clearing, held-item pass-through and reach refusal. Installed
NeoForge patches were inspected for canceled creative/survival prediction paths.
No live graphical input/remapped-key test or connected multiplayer certification.
3427 resources unchanged; only English interaction hints changed. Existing
uncapped alignment, chimes, textures, movement and station features are retained.
See ELEVATORS3_GESTURE_LINKING_VALIDATION.txt and the updated simple guide.

---
Earlier release history follows.

# Elevators 3.0 — 0.2.0-dev8 / remove quarter-block alignment restriction

Removed the +/- 4-pixel alignment cap from both nudge operations and saved-state
loading. The existing controls still move one sixteenth of a block per click,
and Reset and the active-cabin lock remain. Larger offsets survive reload.
Integer overflow is rejected instead of wrapping the cabin across coordinates.
Physical clearance, loaded chunks, floor markers and matching landing doors
remain required. Landings must be repositioned if a large move changes the
entrance grid position. Landing Kit size resolution now prefers the uniquely
matching adjusted cabin entrance over distance to the controller, supporting
landing placement when offsets put the cabin beyond the old nearest-drive radius.

Build and all 162 required native Minecraft GameTests passed. Updated alignment
fixtures verify positive/negative offsets of several blocks and persistent reload
without clamping across all four orientations. No texture, audio, recipe, block
registration or GUI layout changed; all 3428 existing resource files match dev7.
Dev7 GUI scale 4 and native renderer checks are retained evidence, not rerun in
this patch. Graphical/in-game mouse/ghost appearance and live connected multiplayer
remain unverified. See ELEVATORS3_UNCAPPED_ALIGNMENT_VALIDATION.txt.

---
Earlier release history follows.

# Elevators 3.0 — 0.2.0-dev7 / fine cabin alignment

Added Setup > Fine alignment. Inactive cabins can be nudged east/west/north/south
in exact 1/16-block increments, limited to +/- 1/4 block per horizontal axis.
Reset clears both offsets. The ghost preview and actual spawn use the same
saved offsets; vertical movement preserves them. Floor marker leveling, controller
and rail positions and block-grid landing positions remain unchanged. Adjusting
an active cabin is refused server-side. Ordinary collision/activation checks
still reject offsets into obstructions.

Changed ModelCabinController, CabinPlacementPreview and ElevatorScreen; added
native regression fixtures and expanded scale-4 layout coverage. Old saves default
to zero offset. No block registrations, assets, recipes or chimes changed.

Build and all 162 required native Minecraft GameTests passed. Native CLIENT
checks passed for 72 representative scale-4 widget positions at three resolutions
and retained 4,608 production rendering configurations. New tests cover all four
mount orientations, precise spawn, persistence, bounds/reset, active locking and
an offset trip with correct vertical leveling. All 3428 existing resource files
remain byte-identical to dev6. Live graphical appearance and connected multiplayer
still require a user client check. The existing simplified workflow, chat errors,
manual link overrides and GUI scale 4 scrolling are retained.

See ELEVATORS3_SIMPLE_WORKFLOW.md for controls and limits and
ELEVATORS3_ALIGNMENT_VALIDATION.txt for results.

---
Earlier release history follows.

# Elevators 3.0 — 0.2.0-dev6 / simplified workflow and GUI scale 4

Implemented two-stage Cabin Kit placement with a synchronized translucent ghost,
crouch-click rotation and separate confirmation; automatic landing snapping and
unique controller linking; physical-landing-based call buttons at independent
heights; simple Setup and a separate Customize area. Existing manual links win
over automatic call resolution. Errors now enter chat, with duplicate suppression
and automatic nearby/rider fault notifications. Rail/construction rules and the
moving cabin state machine are retained.

GUI scale 4 uses a scrolling content viewport and fixed footers. Removed the
reduce-GUI-scale restriction. Native CLIENT checks confirmed 60 representative
widget placements are reachable at three scale-4 resolutions. The production
render checks still cover 4,608 configurations / 9,179,136 vertices. Build and all
160 native GameTests passed, including three new workflow/chat fixtures. No
live graphical mouse/ghost/shader certification or connected multiplayer claim.

Primary modified classes: ModelCabinItem, CabinPlacementPreview,
ModelCabinRenderer, ModelLandingItem, ModelLandingEntity, ModelCabinController,
ElevatorNetwork, ElevatorCallBlock, ElevatorMenu, ElevatorDriveEntity,
ElevatorScreen, ElevatorLinking and ElevatorFloorMarkerRailBlock. Added
ElevatorFeedback and native workflow/scale-4 validation. Wave 1 texture assets,
escalator/travelator geometry and all other registered features are retained.

See ELEVATORS3_SIMPLE_WORKFLOW.md for the new short building guide and explicit
limits, and ELEVATORS3_WORKFLOW_VALIDATION.txt for validation details.

# Create: Transit — simple elevator setup (0.2.0-dev6)

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21.
Replace the previous Transit JAR on clients and server. Existing elevator saves,
Wave 1 door textures, chimes, ceiling lighting and manual links remain supported.

## Build it

1. **Preview a cabin.** Right-click a clear placement spot with the Model Elevator
   Cabin Kit. A translucent cabin shows its position; the gold outline marks the
   entrance. Crouch-right-click to rotate the preview. Right-click the same spot
   again to confirm the controller. Previewing does not spend the kit. Clicking
   another clear spot updates the preview. The default cabin is 3x3.
2. **Run the rails.** Build one continuous Elevator Guide Rail column directly
   above the controller, against the shaft wall. Leave the moving cabin volume
   clear. Extend the column above the highest cabin roof.
3. **Mark your floors.** Use dedicated Elevator Floor Marker Guide Rails inside
   the column. A marker belongs one block above the hallway floor block: floor
   block Y=64 means marker Y=65. The lowest marker must be immediately above the
   controller. Stops discover and label themselves F1, F2, F3, etc. Open Setup
   and Rescan if you want an immediate refresh; use Floors to rename/disable stops.
4. **Place the doors and buttons.** Face the shaft and place a Landing Door Kit
   at a marked floor's entrance. Clicking any bottom frame cell snaps the full
   frame into the matching opening, within one block of the intended frame.
   Correctly aligned doors link automatically; unlinked doors retry every second
   so placing a marker afterward also works. Place a Floor Call button near the
   entrance at your preferred height. It resolves the nearest complete owned
   landing without matching the marker's height. No wrench step is needed for
   an ordinary single shaft.
5. **Activate.** Open Setup. Set Cabin size before building the landings, set
   Speed and Door Timer as desired, then press Activate. Enter and empty-hand
   right-click the cabin to choose a floor. The existing door interlocks,
   passenger movement and floor alignment handle the trip.

## Setup and customization

The main Setup page contains Cabin size, Floors, Speed, Door Timer, Customize,
Activate/Deactivate, and Rescan/Retry. Door Timer cycles stay-open, 5, 10, 15 and
20 seconds. The existing speed range is above zero through 1 block/tick.
Customize retains the five body, lighting/material, audio/door, movement and
style/Copycat pages. Size, body and rear-entrance structural changes still require
an empty deactivated cabin. Rear service is configured in Floors when needed.

GUI scale 4 is supported with a scrollable content viewport. Scroll the mouse
wheel over the center of the screen; the scrollbar shows extra controls. Footer
controls stay fixed. This also applies to customization and floor editing.

## Errors and advanced linking

Elevator interaction messages are now sent to chat, not the action bar.
Rejected controller actions and rail warnings are reported in chat. New runtime
model-cabin faults notify nearby players and riders once; identical notices are
suppressed for five seconds to prevent spam. The controller still shows normal
operating state and a generic rail-attention indicator.

Automatic call buttons look for complete linked model landings within six blocks
horizontally and 256 blocks vertically, selecting the nearest physical landing.
An equal-distance tie does not guess between different shafts/floors. Disabled
nearest floors do not silently redirect the button to another floor. Explicit
wrench links take priority and remain available for banks, unusual mounting
positions or intentional remote controls. A selected marker followed by a
crouch-wrench-click on a call button adds/removes its explicit link. Existing
saved manual links are retained.

Landing auto-linking requires the exact cabin entrance position, size, facing and
marker height, and a unique matching controller. Doors do not auto-create or cut
building blocks. If a door remains unlinked, check the gold entrance outline,
rail column and marker height; use a wrench-selected marker for manual linking.

The preview footprint uses the actual model geometry. Its first version previews
the default 3x3 cabin before placement; the placed controller allows all four
sizes and existing variants. The preview is cosmetic and does not assemble a
moving cabin until Activate. Loaded chunks and unobstructed travel are required.

## Validation boundary

See ELEVATORS3_WORKFLOW_VALIDATION.txt for build/native test results. Native CLIENT
checks validate actual widget placement at GUI scale 4 for 1920x1080, 1280x960
and 2560x1440, with all tested controls reachable by scrolling. This does not
certify a graphical Minecraft window, shader appearance, mouse feel or connected
multiplayer latency; those still require a live client check. Optional Steam 'n'
Rails is excluded from the automated runtime using the existing project option.


---
Previous release history:

# Elevators 3.0 — 0.2.0-dev5 / Texture Wave 1

Implemented the six approved door designs on cabin and landing doors. Build,
all 157 required native Minecraft GameTests, native CLIENT checks covering
4,608 render configurations / 9,179,136 emitted vertices, and geometry/core
regressions passed. Thirty PNG base/companion assets were validated. Live
shader appearance and connected multiplayer still need an in-game check.

The native gameplay addition verifies controller-selected styles and wood reach
every owned landing member and persist through NBT reload. Existing escalator,
travelator, movement, door interlock, passenger, inventory and audio fixtures passed.
See `ELEVATOR_DOOR_WAVE1_VALIDATION.txt` for the validation boundary.

# Elevator texture Wave 1 — 0.2.0-dev5

Minecraft 1.21.1 · NeoForge 21.1.248 · Create 6.0.10 · Java 21.
Mod ID remains `createtransit`. Replace the previous Transit JAR on clients and
servers with `create-transit-0.2.0-dev5.jar`; keep only one Transit JAR installed.

## Approved designs in this wave

| Style | New finish and panel details |
| --- | --- |
| 01 Solid | Original brushed silver tile, two recessed rectangular sections on each leaf, dark trim and dyeable jamb LEDs. |
| 02 Glass | Clear glass with restrained pixel highlights, slim metal borders and no horizontal crossbars. |
| 03 Wood Trim | Silver panels with selected-wood door inserts, outer-jamb inserts and header inserts. Vertical inserts rotate the chosen block's particle texture; oak is the default. |
| 04 Slim Window | Solid silver leaves with one narrow, tall window per leaf, metal surrounds and dark glazing seals. |
| 05 Divided Glass | Exactly two glass sections on each leaf: one horizontal divider per leaf, four glass sections in total. |
| 06 Industrial | Original ribbed charcoal steel, narrower viewing windows, metal surrounds, yellow center-seam markings, lower-jamb hazard bands and bolt details. |

These are shipped assets and production mesh changes, translated from the
approved concept directions. The presentation images are not used as UV atlases.
The ten other existing styles and Copycat retain their previous designs for a
later approval wave. Their saved enum names/order have not changed.

Both landing and cabin doors use the same mesh and sprite contract. Fixed landing
frames still occupy a two-block height and half-block depth envelope. Panels
slide sideways on the exposed shaft-facing surface, with no depth or swing phase.
Decoration does not change the cabin footprint or door collision behavior.

## Selecting the new textures

1. Open the elevator controller's model-cabin settings and go to the fifth model
   page, containing **Door style**. Cycle through the first six styles.
2. Newly installed controller-generated landings inherit the selected door style,
   glazing, material samples and wood. Changing **Door style** also updates the
   members of existing complete landings linked to that controller. For manually
   placed landings, link them first, then select the controller style to match.
3. To style a landing independently, crouch and wrench-click the fully closed
   landing. The existing style cycle remains available.
4. Use the existing primary dye control for the metal body/panels and secondary
   control for the LED accent. Wood inserts retain the selected wood's color;
   safety markings, glazing seals and neutral metal edging retain their colors.
5. The existing **Wood** field / **Next wood** control changes the selected wood.
   Linked landings receive that selection and save it across reloads. Independently
   configured landing styles can be chosen again after a controller style change.

Guide rails, floor marker height, call-button links, floor requests, chimes,
movement speeds, idle-close settings and ceiling lighting use the existing
construction guide in `ELEVATORS3_DEV4.md`. No new door items are required.

## Textures, renderer and preview

Ten original RGBA PNGs live in
`assets/createtransit/textures/block/elevator_door/`, with matching flat normal
and LabPBR specular/emission companions: 30 small PNGs total. Base tiles have
explicit entries in the mod's contribution to the Minecraft block atlas.
Shader companions follow the existing elevator kit's map convention. Metal keeps
world lighting; LED vertices use full brightness. Shader bloom/reflections still
depend on the user's renderer and shader configuration and are not certified here.

`DoorStyleTextures` defines the shared sprite contract. `DoorGeometry` supplies
window cutouts, trim, real glass sections and wood/safety details. `CabinGeometry.Face`
now carries sprite-local UV bounds. Cropped metal parts sample their corresponding
part of the full leaf texture rather than stretching a complete tile around each
window. New material enum entries are appended; old indices retain their meanings.
Copycat continues using its existing block-particle material sampling.

`generate_elevator_door_wave1.py` reproduces the pixel artwork, companion maps,
manifest hashes and atlas entries. `ExportDoorStyles` exports the actual meshes,
UVs and material mapping; `render_elevator_door_wave1.py` renders them with the
packaged base textures. The preview is a software asset render, not a Minecraft
or Complementary screenshot. Dynamic header labels/arrows are omitted from the
asset preview and remain handled by the existing in-game text renderer.

## Validation scope

See `ELEVATOR_DOOR_WAVE1_VALIDATION.txt` for this build's results. Native CLIENT
checks exercise the production vertex emitter with real Minecraft rendering
types and native PNG decoding; they do not stitch/upload a GPU atlas or open a
graphical game window. The gameplay suite runs the real headless Minecraft server.
Graphical shader appearance, live connected multiplayer, real audio output and
hardware FPS remain in-game checks. The optional Steam 'n' Rails runtime is
excluded with the project's existing `withoutRailways` validation option.

## Scope and compatibility

Modified production classes: `CabinGeometry`, `DoorGeometry`, `ModelCabinRenderer`,
`ModelCabinController`, `ModelLandingBlock` and `ModelLandingEntity`.
Added production class: `DoorStyleTextures`. Other modifications are build version,
atlas sources, exporters, documentation and validation fixtures. Existing
escalator/travelator assets, registrations, movement code, creative inventory,
vehicle assets and audio are retained. Existing model doors refresh their mesh
from saved style settings; this patch does not require rebuilding two-row landings.


---

Earlier release history follows.

# Elevators 3.0 — 0.2.0-dev4

The door/construction update adds 17 shared cabin/landing styles, two-row slab-depth manual landings, independent Copycat material slots and glass layouts, linked frame displays, wall-mounted rail placement, stopped speed changes and timed idle-door closing. See [complete dev4 building guide, implementation details and test limits](ELEVATORS3_DEV4.md). All 156 native GameTests and the build passed; real client/shader/audio checks remain.

The earlier implementation report is retained below as release history.

---

# Elevators 3.0 — 0.2.0-dev3 implementation report

Development build for Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10 and Java 21. Mod ID remains `createtransit`. Existing version metadata requires the JAR filename `create-transit-0.2.0-dev3.jar`; the source archive retains the established CREATE Transit Alpha 0.1.0 filename.

## Dev3: keep both original chimes and add the six CC0 presets

The model-cabin selector now includes eight audible choices: Otis-style, Schindler HT-style, Arrival ding, Elevator bell, Japan arrival, Classic bell + doors, FM bell and Synth ding, plus Off. The original two recordings remain byte-identical to dev2. New presets use the exact six mono Vorbis conversions in the previously delivered CC0 selection pack. They have distinct sound events and translated selector/subtitle labels. Selection uses existing server validation, controller/entity synchronization and saved name-based configuration; existing worlds retain their selected Otis, Schindler or Off value.

The selected cue plays on opening from fully closed doors. Door hiss, motor controls and the absence of a separate close chime are unchanged. Legacy assembled-block elevators keep their existing material-based cues; the eight-option selector belongs to the model-based Elevators 3.0 cabins.

Some sourced recordings include doors or room ambience; they were retained as selected rather than silently replaced or re-created. Five source files came from public Freesound HQ MP3 previews and one from an original OpenGameArt OGG; transcoding does not restore lossless quality. The six are not claimed to be manufacturer-endorsed or brand-verified recordings. Their authors, CC0 dedication, source/download URLs and audio hashes ship inside the JAR under META-INF/createtransit-elevator-chimes-cc0.json and the companion credits text. The rest of the mod retains its existing license.

Full Gradle build and native CLIENT registration/inventory checks passed. **All 153 required Minecraft GameTests passed** in a fresh world, including the new chime registration/configuration/NBT fixture and the retained movement/door/passenger tests. Core tests verify all nine saved chime selections. External ffmpeg/ffprobe validation passed for all six mono Vorbis clips; provenance hashes match the selected pack and the original Otis/Schindler audio matches dev2 byte for byte. This remains headless validation: audible playback and graphical selector interaction still need an in-game client check. See ELEVATORS3_CHIMES_DEV3_VALIDATION.txt.

See [chime controls and credits](ELEVATORS3_CHIMES_DEV3.md). The earlier dev2 report is retained in ELEVATORS3_DEV2_HISTORY.md.

## Implemented behavior

The new Cabin Kit places a controller or configures an existing controller/guide rail. Activation creates a dedicated model-cabin entity, with no individual wall/floor assembly. The implementation reuses the existing side-mounted guide rails, dedicated marker rails, floor discovery, labels, call buttons, floor request UI and network ownership. New model cabins use their own vanilla entity renderer rather than the old Create contraption visualizer path.

All 20 primary cabins are implemented: 2×2, 3×3, 4×4 and 5×5 external footprints in rear-panoramic, solid enclosed, cylindrical-rear, wood-trim and scenic-glass-box styles. The geometry has a continuous floor, ceiling, walls, interior handrails, control panel and upper/lower light trim. There are three clear interior blocks above the standing surface. The cylindrical style has a segmented curved rear and complete roof/floor surfaces; its collision follows a conservative segmented outline.

Front and optional rear sliding doors animate independently. Solid and glass door styles can be selected independently; each floor has front/rear service and matching/solid/glass landing appearance controls. Cylindrical cabins cannot have rear doors. Rear doors replace rear panoramic glazing. The entrance width is one block for 2×2/3×3 cabins and two blocks for 4×4/5×5 cabins, so the sliding panels stay within the cabin footprint. Controller-installed landing doors are actual registered multiblocks with animated collision and ownership, not decorative planes.

Configuration includes primary/secondary dyes, optional panoramic side windows and curved glazing, registry-backed wood/floor textures, four ceiling-light designs (panel, strips, ring, bulbs), four handrail choices (brushed, dark, accent, none), chime choice, door/motor audio switches, movement speed and opening/closing delays. Eleven vanilla wood finishes are available in the quick cycle; installed block registry IDs can supply other materials. Pale Oak requires a 1.21.1 backport and is not silently substituted.

The Create-style controller screen has four model settings pages, discovered floor configuration, per-side landing installation and passenger requests. All mutations are server validated. Holding the Cabin Kit at a controller shows a footprint/clearance outline. Activation checks the home marker, landing doors, loaded footprint and collisions. Structural changes require deactivation. The controller prevents breaking an active cabin's drive.

A server-owned state machine performs acceleration/deceleration, swept collision checks, exact floor alignment, FIFO/coalesced requests, door closing confirmation, movement interlocks, obstruction reopening, independent arrival doors, loaded-path pause and fault/retry. Repair Retry now refreshes rail/floor discovery before resuming the saved destination. Landing-door completeness is checked at registered floors, so crossing an ordinary block height after a mid-shaft restart does not create a false interlock fault. Model entity and controller configuration, UUID binding, movement snapshots and door progress persist. A missing/unloaded saved cabin is not replaced by a duplicate entity.

Passengers are carried continuously with the moving floor using vanilla entity collision; no teleport animation or exposed moving platform is used. The entity synchronizes appearance, orientation and door progress through registered entity data, with client position interpolation. This is a dedicated cabin entity, not a general-purpose Create block contraption: arbitrary placed blocks and their block entities are not captured or transported inside it. Cabin decoration is supplied by the model settings.

Existing metal textures and their normal/specular maps are reused. Glass has a separate translucent pass; light trim uses full-bright vertices. This does not create dynamic world illumination. Two new original synthesized door-hiss and motor recordings are included with a CC0 notice and reproducible generator. Existing original Otis/Schindler-styled arrival recordings are retained alongside the six CC0 options described above. No separate close chime was added.

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

The dev2 fresh-world native Minecraft run: **all 152 required tests passed**, including all five added model-elevator tests and the retained LRT compatibility circuit. The Gradle build, pure core/geometry checks, existing transit checks and native CLIENT registration/inventory data validation passed for that dev2 milestone. Native tests used Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 with the optional Railways runtime excluded. Exact evidence is recorded in `ELEVATORS3_VALIDATION.txt`.

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
