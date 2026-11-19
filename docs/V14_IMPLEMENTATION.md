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
