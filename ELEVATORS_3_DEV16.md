# Create: Transit / Station Utilities — Elevators 3.0, dev16

Minecraft 1.21.1 • NeoForge 21.1.248 • Java 21 • Create 6.0.10

This development update refines the existing dedicated model-cabin implementation from V15.1. It does not rebuild escalators, travelators, fare gates, platform screen doors, vehicles, or creative tabs.

## Delivered changes

### Exact cabin and landing geometry

- Runtime cabin meshes, collision boxes and previews use the same explicit coordinates. Removed the compact cabin's entity/UI scaling, including the old asymmetric depth scaling and 1.5-pixel mount correction.
- The 3×3 cabin occupies X/Z = 1/16 through 47/16 within its nominal 3×3 square: **46×46×40 pixels**, or **2.875×2.875×2.5 blocks**. Height is not reduced below 2.5.
- The centered opening occupies X = 8/16 through 40/16: **32 pixels**, with **7-pixel cabin jambs**. Its sliding panels fully clear that opening.
- Cabin frames occupy **8 pixels of depth inward** from the inset body boundary. The front pocket occupies Z = 1/16 through 9/16. Rear pockets stay inside the rear perimeter.
- 3×3 landing frames retain **48×48 pixels**, **8-pixel jambs**, a **32-pixel opening**, and **8-pixel depth**. In their own block coordinates the landing frame occupies Z = 0.5 through 1.0; it never extends beyond that block boundary.
- Landing geometry no longer inherits fractional translations from the cabin. Half-block positioning remains available. A cabin must ultimately align with the landing block grid before landing installation/service is accepted; pixel offsets do not compensate for a narrowed doorway.
- Added actual occupied-space obstruction checks at cabin placement; player blocks are not removed. Preview colors use that same placement check.
- Solid/glass front and rear doors retain independent selections. Panoramic rear glazing is replaced by framing when rear doors are selected. Cylindrical cabins reject rear doors.

### Models, sizes and specification conflicts

The approved list contains **four** models: Solid, Rear Panoramic, Scenic Glass Box, and Cylindrical. Wood Trim is removed as a selectable model. Cylindrical cabins use segmented voxel geometry.

The explicit new size list contains **2×2 and 3×3**. Existing 4×4/5×5 saved configurations remain readable, with the same one-pixel inset and existing compact 2.5-block height; they are not offered for new selection.

**Unresolved 2×2 requirement:** a 2×2 cabin with one-pixel insets is 30 pixels wide and cannot contain a 32-pixel opening plus framing. The existing **16-pixel passage** is retained for this size. Its exterior is 30×30×40 pixels. This is an explicit exception requiring a design decision, not a claim of full two-block-passage compliance. The smallest compatible adjustment is a one-block 2×2 passage; alternatively use 3×3 as the minimum size.

The “five models / four sizes” wording in the testing section conflicts with the actual model/size lists. Tests cover all four approved models, both listed sizes, and legacy 4×4/5×5 geometry. No fifth model was invented.

### Supplied textures

Only **Modern Gunmetal Gray** and **Brushed Stainless Steel** remain selectable. Wood Trim and Light Transit runtime skin folders, atlas entries and matching reference door models are removed. Retired/unknown saved skin names safely resolve to Modern Gunmetal Gray; serialized legacy identifiers remain readable for migration.

The supplied JPEG sheets are flattened presentation boards rather than separate original 16×16 PNG tiles. Materials are **cropped directly from those sheets and sampled to 16×16 with nearest-neighbor sampling**. No replacement artwork or procedural grain was generated. Original boards, lossless full-resolution crops, crop coordinates, source hashes and runtime tile hashes are preserved in `reference/elevators3/`.

The 96 runtime tiles cover both skins at the existing off/low/medium/high quality settings. Removed stale skin PBR companions rather than retaining maps describing the old art. Shader reflections/PBR enhancement remain outside the verified scope.

Default finishes render with their supplied colors. Existing body/accent dye selections remain available as optional tinting; white body/default trim leave the supplied color values untinted. Solid-floor material overrides remain available.

### Lighting and handrails

- Five geometrically distinct ceiling layouts: Single Large Panel; Four Symmetrical Panels; Perimeter LED Frame; Three Linear LED Strips; Center Panel plus Four Corner Lights.
- All 16 Minecraft dye colors use labeled menu swatches.
- Each shaft stores five independent ceiling colors. Switching layouts restores that layout's saved color.
- Interior brightness is a separate integer 0–15, default 15. Invisible cabin light blocks carry that brightness; fixtures remain enabled without redstone.
- Door-frame LED color remains independent of ceiling color.
- Every model includes rectangular handrails and discreet mounting brackets. Side/rear rails stop at door openings; cylindrical rails follow the segmented outline. Handrails do not add passenger-snagging collision shapes.
- Finishes: Match Cabin Skin (default), Brushed Silver, Dark Metal. Legacy disabled/accent handrails migrate to Match Cabin Skin. No disable option is offered.

### Configuration, indicators and performance

- Updated size, skin, handrail, ceiling, brightness and door controls in the existing controller interface. Removed discontinued choices and fixed-depth/mechanism toggles that conflict with the prescribed entrance.
- Preserved sounds, floor policies, guide rails, call-button linking, hover highlighting, floor labels and endpoint call-direction logic.
- Floor plaques remain landing-only. Cabin/landing indicator artwork is inset from the frame boundaries to avoid crossing the cabin or landing perimeter. It uses the existing full-bright render path; actual shader visibility needs client validation.
- Preserved bounded immutable body/door/texture/UV caches and static open/closed leaf caching. Stationary controller synchronization now checks for state/message changes before sending the normal five-tick update.
- Landing render bounds cover the actual multi-block assembly instead of a six-block expansion in every direction. Cabin entity bounds match its physical inset body.
- No FPS improvement percentage is claimed. No measured multi-elevator benchmark or live shader run was performed.

### Movement and compatibility

`CabinMotionController.java` is unchanged from V15.1: speed profile, acceleration, request queue, leveling, floor stops and door interlocks are retained. Configuring compact geometry no longer silently changes the saved speed to 0.6.

Integration changes are limited to exact body bounds, nominal-grid placement/rotation, landing-grid validation, placement obstruction checking, door geometry and reducing unchanged synchronization. Existing floor/marker/button identities and sound selections remain in the save format.

Cabin configuration schema is now **3** and accepts legacy schemas 1 and 2. Network protocol is **elevator3-dev16**: use matching dev16 client/server versions. Existing offset landings may need the empty cabin deactivated, Auto Center applied, and landing frames reinstalled; migration must not silently move player-built blocks.

## Test report

The companion `verification/dev16/` directory contains captured test output and the coordinate drawing. The drawing is an offline geometry illustration, not an in-game screenshot.

| Check | Result | Evidence / scope |
|---|---|---|
| Java 21 / NeoForge compilation and development JAR | Passed | Gradle `build`; final JAR integrity audit in `verification/dev16/jar-audit.txt` |
| Exact 46×46×40 body, actual mesh extents and collision bounds | Passed | `Elevators30Test` |
| 32px opening, 7px cabin jambs, 8px landing jambs and frame depths | Passed | Geometry/collider assertions and front/side/top coordinate review |
| Four approved models, both skins, front/rear glass/solid combinations | Passed in engine-neutral tests | 224 configurations including legacy sizes; five animation poses |
| Open-door collision clearance | Passed in engine-neutral tests | Cabin and landing aperture intersection assertions |
| Five LED layouts, 16 colors and per-style memory | Passed in engine-neutral tests | 80 style/color selections; five distinct light meshes |
| Brightness and handrail save/load | Passed in engine-neutral tests | All 16 brightness levels and three finishes |
| Legacy migration and door poses | Passed | 96 configuration combinations, 672 poses, 51 style migrations |
| Movement controller regression | Passed in engine-neutral tests | Queues, both directions, floor policies, obstruction reopening, unload pause, snapshot recovery, speed bounds and exact stops |
| Texture fidelity, skin removal and atlas integrity | Passed | 96 byte-for-byte expected crop/sample comparisons; JSON/atlas checks |
| Existing transit network and radio regression checks | Passed | `transitCheck` and `radioBufferCheck` |
| Headless Minecraft integration | Passed: all 4 required tests | Exact entity bounds, standalone three-row landing installation, three-row grid landings, per-block collision extents, NBT settings round-trip, obstructed placement retaining blocks, exact floor stop and empty open center passage |
| Rendered controller menus and hover interaction | Untested in client | Implemented controls; no graphical client inspection |
| Complementary Reimagined / other shaders | Untested | No shader session |
| 2×2 full two-block passage | Not met / conflicting dimensions | 30px exterior cannot contain a 32px opening; retained 16px passage |
| Live multiplayer client/server synchronization | Untested | Server-owned saved settings and existing packet paths retained; no two-client session |
| Full world shutdown/restart | Untested | Properties/Minecraft NBT round-trips are separate checks |
| Passenger walking/riding with a real client | Untested | Core movement tests are not live passenger validation |
| Measured FPS / many-elevator comparison | Untested | No measured performance claim |
| Full archived native regression suite | Untested | Several archived assertions describe intentionally retired geometry/skins; bounded new native suite is separate |

## Remaining limitations and installation

This is a development test release. The 2×2 two-block doorway requirement remains physically incompatible with its footprint. Original separate lossless 16×16 texture files were not supplied; runtime tiles are documented extractions from the supplied sheets.

For an existing test world, use matching client/server JARs and keep a backup. Stop and empty the cabin, deactivate it, select the desired model/size/skin, Auto Center it on the guide rail, inspect the Refresher preview, and install matching grid-aligned landing frames at served floors. Re-activate and verify access at each floor. Test shader appearance and passenger movement before using the build in the main world.

The source package includes production Java geometry, 16 exported inspection meshes (not separately baked Minecraft block models), exact extracted textures, original supplied references, extraction/export tools, tests and this report. Runtime cabins are rendered by the existing custom entity renderer.
