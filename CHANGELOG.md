## 0.4.1 — Elevator Cabin Kit placement

### Fixed
- Cabin Kits now build the cabin exactly as the preview showed it, in every facing, with the doors on the highlighted gold side.
  - Before this fix, every placed kit cabin was turned a quarter from its preview, so the doors were not on the gold side.
  - Facing east or south, the home-floor landing then landed on the kit's own controller and couldn't be installed ("Landing space obstructed").
  - Facing west, the controller stood in front of the middle of the doorway.
- A cabin rotated so a doorway faces its own guide rail now says so ("Rotate it in Alignment so the rail is beside the cabin, not behind a doorway"). Before, it reported "Cabin overlaps blocks, another cabin, or is unloaded".

### Upgrade
- Only newly placed kits change, because each controller saves its layout. To fix a cabin already built from a kit, empty and deactivate it, then use **Alignment → Rotate** until the preview's entrance faces your landing. Or break the controller and place the kit again.

## 0.4.0 — Default stations, saved-data cleanup and radio memory

### Added
- Server config `radio.defaultStations` in `createtransit-server.toml`: a list of `"Name|URL"` stations added to each world automatically. Each one is added once per world, so a station an operator removes stays removed. Entries follow the same rules as stations added in game (public address, `radio.allowedHosts`, 16-station limit), and any refused entry is logged as a warning. Edits apply on the next server start or when the config file is reloaded. The list is empty by default, so the mod still ships with no stations.

### Changed
- The radio settings screen no longer says "no bundled stations". It now reads "MP3 / Ogg • direct live streams". The mod still ships with no stations.

### Fixed
- Elevator bank data no longer grows forever. Cars whose drive was broken, banks left with no cars, selector links to broken panels, and calls that can no longer be served are now cleaned up automatically, including data left by older versions. Only loaded areas are checked, so elevators that are just out of range are never affected. A call that no car can serve for 60 seconds now expires, so its call button light turns off, and riders get a message.
- Radio groups are no longer kept forever. The world now records which speakers belong to each group, and a group with no speakers left is deleted after 24,000 ticks (one in-game day) of server time. The delay covers speakers riding on contraptions or trains. Only loaded areas are checked, so speakers that are just out of range keep their group. This also frees slots once a world reaches the 64-group limit. Groups saved by older versions are never deleted automatically, because some of their speakers may not have loaded since the upgrade.
- Radio streaming no longer allocates off-heap (direct) memory for every audio chunk. Decoding and the shared live buffer now use ordinary heap memory, and only the buffer handed to the sound engine is direct. This cuts native-memory churn per station, including stations that warm up with no one listening.
- A stereo MP3 frame with an odd number of samples could crash the radio decoder. It now decodes safely.

## 0.3.1 — Inventory icons and sound recovery

### Fixed
- Speakers and cabin radios went silent after F3+T or a change of Windows audio device, while still showing "Playing". They now reconnect within about a second.
- The elevator motor hum no longer stays silent after the sound engine restarts mid-trip.
- 62 inventory icons are resized and centred so they fit their slots. Gondola walls, corners, LED strips and doors no longer spill into neighbouring slots or draw as bars along the slot edge, most visibly in creative search. Placed blocks and held items are unchanged.

## 0.3.0 — Radio stream safety and stability

### Breaking
- Only operators (permission level 2) or the owner of a single-player world can now add or remove radio stations. Upgrade: to restore the old behaviour, set `radio.stationEditors = "BUILDERS"` in `serverconfig/createtransit-server.toml`.

### Added
- Server config `createtransit-server.toml` with `radio.stationEditors` and `radio.allowedHosts`, an optional list of approved stream hosts (`*.example.net` wildcards allowed).

### Fixed
- Clients refuse radio streams, and redirects, that point at their own computer, local network, link-local or cloud-metadata addresses, multicast, CGNAT or IPv6 private ranges.
- The server rejects stations with localhost names or private IP addresses, and stops advertising saved stations that fail these rules or the allow-list.
- Fixed a rare race in single-player and LAN worlds, where the client and built-in server threads changed the same elevator tracking maps at once. It could cause a hang, or an elevator part that was never registered.
- Speakers whose radio group no longer exists, for example ones pasted from another world, no longer recreate that group just by being checked. Once a world had 64 groups, that check could kick the nearby player. The speaker now plays its own settings until it joins a group.

## 0.2.0-dev18.1 — Elevator and client interface refinements

- Live Y-level returns to cabin headers and appears on landing displays during travel. Animated arrows scroll repeating chevrons. Landing arrival lanterns offer accent or green-up/red-down colors.
- Cabin and landing door status strips flash green during opening, stay green while open, flash red during closing, and switch off when closed.
- Cabin selector tiles show hover and stay lit for all queued stops until served. The full passenger menu opens from the upper header. Hallway selectors show six floors per page in three rows of two.
- Cylindrical queue displays move inward and farther from the doorway. Hall calls regain hover lighting, and round and square Full Metal and Ring Light styles use brushed metal. Ring Light illuminates both arrow and rim.
- Classic Brushed Steel and the dark hall call backing have LabPBR maps. The wood door retains its existing texture.
- Ambient traffic adds Extreme and a per-shaft rider toggle. Queued stops honor the configured fully-open door timer.
- Use /csu streamermode to toggle personal radio playback, and /csu settings for the 16-color client theme.


## 0.2.0-dev18 — Elevator service and customization

- Direction-aware cabin queues coalesce duplicate floor selections, insert safe intermediate stops, and reverse at a stop. Destination dispatch retains the boarded passenger's priority. Queue displays show the next stop and scheduled order with independent left and right layouts.
- Separate landing arrival lanterns and floor-position arrows. Cabin and landing arrow animation can be switched independently. Position arrows default to static and use the configured LED dye.
- Add Classic Brushed Steel, OG Steel, and Classic Wood center-opening doors. Steel styles offer solid, full, and slim glazing. Existing solid and glass appearances remain readable. New cabins and landing kits start with Classic Brushed Steel.
- Add front-only rear LED runs and connect the cylindrical strips along the existing rear contour. Apply block-scale UVs to horizontal floor and ceiling faces. Use an upward-facing baked block sprite for selected floor materials when available.
- Refine cabin touchscreen typography and highlight selected floors. Keep one shared page per cabin. Hall destination selectors show four compact floor IDs per page and can display smaller floor names. Assignment text uses the dispatcher’s actual car letter.
- Add bank merge previews, retained primary letters, undo of the most recent merge, and a manual selector override for nearby ambiguous banks. Assignments continue to require registered service at pickup and destination.
- Add per-shaft Ambient Elevator Traffic with Low, Medium, and High frequency. It defaults off, uses random served floors, and yields to passenger calls through the motion controller.
- Add six hall call faces, bank-color and directional lighting, and positional acknowledgment audio with volume controls for hall calls and the cabin touchscreen.

# 0.2.0-dev17.5 — Landing indicator rendering fix

- Mount one arrow and floor plaque on each side of each landing jamb.
- Render display borders and backing as adjacent surfaces.

# 0.2.0-dev17.4 — Thresholds, displays, and configuration sharing

- Put landing and V17 cabin doors on adjacent threshold tracks.
- Add two-sided alternating landing displays and restore inner-jamb LED strips.
- Show the floor name at rest and destination ID with live Y during travel.
- Page the cabin touchscreen with six stops, footer arrows, and mouse-wheel input.
- Add Copy Config and Paste Config, including radio settings.
- Set Brushed Steel, light level 6, and speed 0.5 for new elevators.

# 0.2.0-dev17.3 — Restore V13 elevator rendering

- Restore the V13 face and UV rendering path for elevator cabins, landing doors, and previews.
- Keep V17 geometry, the original V13 texture set, cabin lighting controls, and floor display overlays.

# 0.2.0-dev17.2 — Original V13 elevator artwork

- Replace both current elevator skin sets with original V13 textures.
- Restore distinct cabin shell, door frame, solid leaf, and glazing sprites.
- Map the original tall leaf and glass textures across their faces.

# 0.2.0-dev17.1 — V13 cabin rollback

- Restore the compact V13 shell, door frame, door geometry, and collision when using Revert Cabin.
- Keep the V13 compact mesh centered on the existing V17 anchor and preserve the landing assemblies, links, and settings.
- Keep V16 saves readable and allow a direct switch to V13 from Setup.

# 0.2.0-dev17 — Elevator 3.0 cabin geometry and rollback

- Standardize new cabins at 3 × 3 blocks and 40 px high, with 7 px cabin jambs and 8 px door recesses.
- Add mirrored interior floor selectors and displays to enabled rear entrances and keep hallway indicators and LED strips.
- Keep landings on the block grid and expose the Flush and Recessed frame profiles.
- Add per-cabin geometry upgrade and rollback with stopped-cabin and clearance checks.
- See `ELEVATORS_3_DEV17.md` for upgrade instructions and scope.

# 0.2.0-dev16 — Elevators 3.0 exact geometry and approved finishes

- Replace compact-cabin render scaling with explicit 46×46×40px 3×3 geometry, centered 32px doors, 7px cabin jambs and inward 8px pockets.
- Keep 48×48px landing frames on the block grid with 8px jambs/depth and no shaft intrusion.
- Integrate the supplied Gunmetal and Stainless sheets by documented direct extraction; remove retired skin assets and the Wood Trim model choice.
- Add five independent ceiling-color memories, 16 dye swatches, separate 0–15 interior brightness, and mandatory three-finish handrails.
- Keep front/rear glass/solid selections independent; preserve movement-controller code and saved speed.
- Reduce unchanged controller synchronization and tighten landing render bounds. Keep existing stationary geometry caches.
- Configuration schema 3; matching-client protocol `elevator3-dev16`. Preserve old skin identifiers for safe fallback.
- Explicit design exception: 2×2 retains a 16px passage because a 30px inset body cannot fit a 32px opening. New selection lists 2×2/3×3; saved 4×4/5×5 remain readable.
- Built with NeoForge 21.1.248 / Java 21 / Create 6.0.10. Detailed changes, actual validation and outstanding limitations are in `ELEVATORS_3_DEV16.md`.

# V11.1 / 0.2.0-dev11.1

Add Setup → Update Cabin to convert stopped, empty model cabins in place while
retaining settings and links, including wide entrances. Replace numeric dye
selectors with named color strips and contrasting labels.

# V11 / 0.2.0-dev11

Rail-first one-click compact cabins, integrated under-rail driver, 2.5-block
height and half-block door pockets; level-15 cabin lighting; smooth floor/menu
scrolling and E typing; gentler distance-based acceleration/braking; twelve
speaker emitters, URL-shared warm radio connections; Station Utilities JAR name.
Existing saved cabins retain their original mount and dimensions.

# V10.2 — 256-block radio and speaker falloff

- Raise radio/speaker radius controls, saved validation and native sound falloff
  to 256 blocks; new emitters default to 256.
- Upgrade old emitters at the former 64-block cap once, preserving smaller custom
  settings and newly chosen 64-block settings.
- Apply live radius edits by restarting the stream with its new attenuation.
- Keep Nearby cabin radio using server positions beyond visual entity tracking.
- Retain stream-count, decoded-buffer and loaded-chunk limits.

---

# V10.1 — radio audio continuity

- Fix silence being inserted at decoder-chunk boundaries during Minecraft's
  four-buffer audio startup and ongoing streaming.
- Six-second live prebuffer and bounded eight-second PCM reserve.
- Preserve exact samples across partial reads and ring wrapping.
- Strip ICY station/title metadata before audio decoding.
- Cancel startup and close connections without blocking the audio thread.
- Add real HTTP codec, jitter, continuity and cancellation regressions.

---

# V10 — Create: Station Utilities

- New shared navy/cyan GUI styling and seven controller tabs: Setup, Doors,
  Cabin, Motion, Alignment, Radio and Advanced. Compact icon navigation at small
  logical heights, fixed footers, preserved scroll position and responsive previews.
- Floor selection retains server-owned Go requests, editing and service flags;
  pencil-only edit buttons and highlighted current floors.
- Controller/first marker generated two blocks inward for new confirmed Cabin
  Kits; the preview cabin stays in its original position. Existing shafts stay put.
- Continuous rails only between stopping markers. Removed the requirement to
  extend rails through the highest cabin roof for model and legacy contraptions.
- Half-block alignment buttons, uncapped movement, compact-baseline Reset,
  persistent legacy offsets, and matching fractional landing meshes/collisions.
- Shared Center/Left/Right slide and single-/two-speed panels; no outward motion.
  Separate telescoping depth lanes clear the opening fully.
- New kits use a 3x3 entrance with 2x2 clear opening; standard depth 0.5, Flush
  depth 1. Existing smaller entrances remain saved and supported.
- Recessed floor/arrow screens on opposite ends, visible with doors open,
  both faces finished, persistent mirror and display visibility options.
- Shared door settings accessible by using the Landing Door Kit on a linked door.
- New per-world, empty-by-default station list and cabin radio. Direct HTTP(S)
  MP3/Vorbis streaming through Minecraft's positioned sound system.
- Wall/ceiling Station Speaker, shared stations, volume/radius and redstone modes.
- Bounded client audio buffers, background decoding, connection feedback in chat,
  retained chimes, floor detection, linking, dye behavior and station utilities.
- Mod display/GUI branding is Create: Station Utilities; mod ID, registry names
  and versioned development JAR convention remain compatible.

---

# 0.2.0-dev4 — landing doors, styles and construction

- Add the manual Landing Door Kit and marker/entrance validation.
- Add 17 shared door styles and independent Copycat materials with three glass layouts.
- Revise landing doors to a 2-block-high, half-block-deep frame and sideways surface sliding, with floor/direction header displays.
- Add wall-face guide rail placement, idle-close controls and stopped speed changes.
- Preserve existing chimes, cabin families and station/escalator/travelator systems.

# 0.2.0-dev3 — eight selectable elevator chimes

- Preserve the original Otis-style and Schindler HT-style recordings and existing saved selections.
- Add Arrival ding, Elevator bell, Japan arrival, Classic bell + doors, FM bell and Synth ding from the six-clip CC0 pack as distinct selectable opening cues in model-cabin settings; retain Off.
- Store new selections through existing server/controller/entity synchronization and persistence. Keep legacy assembled-cabin cues, door hiss/motor controls and closing behavior.
- Include original authors, source/license references, audio provenance and hashes in the JAR. Validate decoding, registration, all nine saved selector values and unchanged original audio.

# 0.2.0-dev2 — integrated model elevator cabins

- Add Cabin Kit placement/configuration, all 20 model cabins and native renderer/collision support, independent front/rear sliding doors and controller-installed owned landing multiblocks.
- Connect the model motion engine to existing guide rails, discovered marker floors, call buttons and server-synchronized controller/passenger screens.
- Add two-channel color, glazing, wood/floor material, ceiling-light and handrail settings, chime/audio controls, speed and delay settings.
- Persist controller/entity binding and movement; support swept travel, passengers, door interlocks and repaired-rail retry without losing the saved destination or faulting at ordinary block heights between landings.
- Hide legacy elevator construction items from the new catalog while retaining registered save data and legacy behavior. Preserve the dev1 gondola-only Vehicle Kits catalog and station/escalator/travelator systems.
- Add five native elevator test fixtures and actual-geometry previews for all 20 base cabins, 184 structural/door/glazing combinations and 19 finish examples. See the current implementation report for exact validation and remaining in-game checks.

# 0.2.0-dev1 — incomplete Elevators 3.0 foundation checkpoint

- Retire LRT from Vehicle Kits and remove its 46 recipes. Preserve existing registrations/assets pending tested save migration.
- Keep gondola and station resources unchanged.
- Register the legacy elevator Flywheel visualizer while retaining normal door/block entity rendering.
- Add validated cabin configuration and deterministic motion/door engine core; not connected to Minecraft entities yet.
- Add core regression coverage and client registration/catalog checks. See docs/ELEVATORS3_IMPLEMENTATION.md for exact remaining work.

## Alpha16.5 — smart escalators and repaired landing joints

- Joined flat/diagonal rubber handrails with matching mitered edges; removed overlapping terminal faces.
- Added one-item automatic glass/solid escalators and redstone-powered variants.
- Neighbour-aware shapes and width preserve colors, block entities and animation phase.
- Powered runs start with a signal; regular runs retain redstone stopping. Separate control networks keep modes independent.
- Updated Station Utilities to four smart escalator styles and two travelators; old section IDs and existing builds remain supported.
- Preserved the confirmed Alpha16.4 FPS collision/selection caches and dye refresh.

## Alpha16.4 — Escalator incline workload and dye refresh

- Build exact moving incline collision grids directly instead of repeated unions and optimization; rotate before voxelization.
- Cache ray-cast boxes and selection-outline edges per exact phase.
- Share identical exact-phase incline shapes between synchronized sections, glass/solid skins and belt directions.
- Refresh client chunk tint and model data after receiving frame/LED color packets.
- Verify actual frame and crouched LED dye clicks on all eight escalator/travelator parts and all sixteen dye colors, offhand usage, survival consumption, creative preservation and packet persistence.
- Retain the Alpha16.3 moving mesh and Flywheel/fallback renderer, speed, riders, sounds, inner/outer LEDs and Alpha16.1 inventory.

## Alpha16.3 — Escalator renderer rework

- Build moving visible surfaces directly into bounded reusable arrays; remove per-frame Part/quad/mesh allocations, including independent-clock cache thrashing.
- Omit hidden step box sides/undersides and subpixel strip edges; retain tread tops/front risers, metal lips, yellow edges, handrail seams and outward-facing terminal signs. Native regression vertices: 953,856 → 200,960 (78.93% fewer).
- Add Flywheel visualization with six shared unit-surface models and reusable, bounded, light-aware instance pools. Hide unused surfaces and delete all instances when visuals are removed. Retain extended culling/light bounds.
- Use the same reduced geometry in the native shader fallback; skip its block-entity render only when visualization is supported.
- Defer intermediate native collision union optimization; preserve exact physical unions in all orientations and step phases.
- Preserve static kit assets, inner/outer LEDs, glass, dyes, motion speed, sound, saved builds and the two-tab inventory.
- Record CPU/allocation results against Alpha16.2 and state graphical FPS/shader verification limits explicitly.

## Alpha16.2 — Escalator performance

- Cache per-section incline collision at the exact game-tick phase; share phase-independent flat/landing shapes and static rail geometry. Preserve all moving collision surfaces and underside slices.
- Share packed animation geometry between synchronized sections, cache atlas sprites until model/texture reload, and reuse native matrix destinations instead of temporary vectors per vertex.
- Restore normal camera-frustum culling with cached bounds that contain every moving step, handrail seam and direction indicator.
- Replace each section's client-side living-entity scan with direct local-player checks. Keep server rider physics and the existing one-move-per-tick guard.
- Preserve models, textures, glass layers, inside/outside LED trim, animation speed, direction indicators, dyes, motor sound and the two-tab inventory rework.

## Alpha16.1 — Creative inventory rework

- Replace the six previous tabs with Vehicle Kits and Station Utilities.
- Add full-width named divider rows for Gondola, LRT, Fare Equipment, Platform Screen Doors, Elevators, and Escalators & Travelators.
- Keep all 111 base catalog items, default-white dyeable entries, existing kit ordering and optional cable track integration. Keep separator/padding cells out of search and obtainable inventories.
- Make UI decoration cells ignore hover/click actions; preserve normal item, hotbar and other-tab behavior. Retain integration-added entries in an Additional Parts section.
- Verify actual client mixin transformation in NeoForge's client data launch, pass all 101 required native GameTests, and export/render the registered catalog. All Alpha15.4 geometry, textures, blockstates and audio are preserved.

## Alpha14.1 — Remove old EMU and HSR kits

Deleted the old designs, asset generators, resources, registrations and QuickBuilds. Removed their shared horn/gangway hooks while retaining the LRT implementations. The two kits will be redesigned from scratch in a future release. This is a removal patch: old EMU/HSR blocks are no longer registered.

## Alpha13.8 — Elevator Floor Marker

- Added an independently craftable and dyeable stationary floor marker plate.
- Persistent drive binding and marker-to-button links using the Create wrench.
- One button or redstone input calls multiple linked elevators to their markers.
- Marker floors appear in the existing cabin floor menu.
- Unlinked buttons retain existing nearest-drive behavior; missing linked targets never trigger a fallback.

## Alpha13.7 — Elevator chime correction

- Solid cabin: same bell struck twice, 0.65 seconds apart; second ring has a longer decay.
- Glass cabin: deeper descending 659.25 Hz to 523.25 Hz pair, 0.32 seconds apart.
- Elevator cabin and landing doors close silently; vehicle chimes remain unchanged.
- Focused Java 21 playback checks and audio/archive checks passed. Full native game tests were not rerun for this patch.

## Alpha13.6

- Wall-face call button placement with matching collision.
- Original classic bell opening cue for solid doors and early HT-style cue for glass doors, including moving cabins.
- Floor/guide-rail tinting, accent dye for the original ceiling lamp, and dye synchronization across both door rows.
- Updated four elevator QuickBuilds with mounted call buttons.
- Exact model canonicalization within each kit and reachable-model packaging: 2,332 fewer packaged models and about 9 MB less model JSON. All blocks, items and selected meshes remain.

# 0.5.0-alpha.12-platforms.2

- Added three 512 × 512 native Create LRT circuit schematics: CRN boards,
  NoCRN, and track/stations only. Both closed running loops use standard gauge,
  12-block track centers, eight broad curves and 16 block signals.
- Added one center-platform and one side-platform station with 116-block
  platforms, two-block level boarding, matched screen-door bays, footbridges,
  berth observers, manual door controls and decorative catenary.
- Included a staged three-car LRT with native bogeys, separate glue groups,
  a conductor and prepared cyclic station schedules.
- Reduced six platform blockstate multipart files from 7,344 to 892 rules.
  Geometry comparison preserves all 3,024 reachable states. No kit block IDs,
  textures, sounds or gameplay code change in this patch.
- Added independent NBT topology/clearance checks and native Create circuit
  assembly/navigation GameTests. See `CIRCUIT_VALIDATION.txt` for results.

# Alpha 11 — compact rounded HSR and expanded LRT kit

- HSR front coach is now 5 × 5 × 31, with three full interior rows and a ceiling
  in the fourth row above the deck. Roof AC and animated pantograph are recessed.
- Sculpted HSR cab now uses a continuous rounded spline and domed tip.
- HSR seats have integral full-block pedestals and correctly positioned native
  Create seat attachments; 32 passenger seats remain in 2+2 rows.
- Added HSR Bogey Deck and flattened door thresholds. Own doors, status lights
  and original UK-style opening/closing cues are retained.
- Added 5 LRT ceiling lights, 6 window variants and 5 body/bogey skirt modules.
- Updated the LRT single-car and both articulated QuickBuilds without moving or
  replacing native Create bogeys. Added connected ring and window/collision checks.
- Existing station, gondola, LRT and EMU registrations are retained. No livery
  or intermediate HSR coach is added in this patch.

# 0.5.0-alpha.10-hsr.1

- Added 25 separately registered HSR kit modules, own models/materials/entity types and creative tab.
- Added a sculpted six-section nose, 31 × 5 front coach with six block rows to the roof and a sealed rear.
- Added 32 passenger seats in 2+2 rows plus driver, integrated wall-seats and a clear supported aisle.
- Added one-wide three-high outward plug-slide doors, selectable left/right travel, status LEDs, warning dwell, original two-tone opening chimes and quiet closing beeps.
- Added own animated pantograph, roof AC, ceiling strips and native CRN display supports.
- Added front-coach CRN/NoCRN NBT QuickBuilds with native standard Create bogeys/glue.
- Removed competing floor/wall faces in the new HSR modules and kept solid/glass meshes in separate render layers.
- Existing EMU and other kit blocks remain compatible; no livery or future double-width HSR door is included.

# 0.5.0-alpha.9-emu.1

- Round the EMU's own front corners, windshield, headlight strip, accent pillars and roof brow. Extend only the outer cabs by four blocks; preserve the original wheelbase, bogey coordinates, passenger doors, decks and coupling gaps.
- Add an EMU Cab Floor and five side-return modules, with fitted depth slices and matching native collision. Keep all models and textures within the EMU kit. Preserve depth through pick block/drops and add wrench/placement support.
- Regenerate all ten QuickBuilds. Cab coaches are 35 blocks; intermediate bodies remain 31. Export the four-block body-origin marker and document placement over existing bogeys. Keep native per-car glue groups intact.
- Pass 66 base-mod Minecraft GameTests, including rounded-cab walking and exact exported train assembly with every bogey position checked. Validate 932 model bakes and 14,924 EMU states; refresh the actual-mesh preview and source backup.

# 0.5.0-alpha.8-emu.1

- Separate the bi-level EMU into 39 registered parts with independent items, entities, model loader, models and materials. Replace LRT parts in all ten EMU schematics with dedicated EMU equipment; retain native Create bogeys and optional CRN boards.
- Extend both decks across the five-wide body to the walls. Add lower and upper floor-edge modules, boxed upper-deck beams, vented underframes and upper seats with integrated floor plates.
- Rework glazing into wider connected banks with dedicated sill, header and automatic continuous mullion modules. Add matching EMU door frames, sliding leaves, status lights, animated pantograph, cab console, roof horn and full-height dyeable gangway.
- Regenerate the sealed single front coach, intermediate car and two-, five- and seven-car Sanctuary consists, each with a NoCRN option and separate per-car glue entities.
- Pass all 65 required base-mod Minecraft GameTests, including native deck movement and exact NBT train assembly. Check 674 native model bakes, 225 EMU dynamic faces and 8,012 EMU block states; refresh the actual front-coach preview and portable source.

# 0.5.0-alpha.7-sanctuary.1

- Reconstruct the lost bi-level EMU on the retained Alpha 6 source, with 25 initial shell modules, Sanctuary textures and two-, five- and seven-car schematics.

# 0.5.0-alpha.6

- Make articulated bellows inherit dye from their anchor, with narrow accent-color ridges, alternating fold shade, sampled segment lighting and a fabric labPBR normal/specular texture. Preserve the gangway’s physical collision.
- Add a dyeable low-profile Roof Horn to the LRT kit and all cab schematics. Holding left Shift at Create controls drives Create’s sustained whistle on the horn-equipped train; release stops it. The existing Create sprint horn key continues to work.
- Make cab nose lamps white when leading and red when trailing, updating with train direction and retaining full-bright visible status on assembled carriages.
- Preserve native bogeys, CRN displays, door motion and prior schematics; add horn/headlight/bellows regressions, refresh previews and guide.

# 0.5.0-alpha.5

- Fix random neighboring block textures on moving LRT door glass. Keep sprite-local UVs within 0..16 for both two-high and saved three-high doors; share mapping across generated models, dynamic meshes, editor assets and previews.
- Route actual moving vertices through an atlas-safe emitter. Add direct headless emitter tests for 318 faces, including 24 glass faces.
- Add physical gangway floors, walls and ceilings from the same interpolated bellows shell. Supply placed-shell collision to normal Minecraft movement, and moving-shell collision to Create's passenger pass. Keep both passage ends open.
- Expand only the native passenger search bounds to include the flexing gangway outside the car body. Refresh cached shell geometry as adjacent carriage frames change and when native colliders are invalidated. Unloaded/disconnected anchors leave no extra shell.
- Test real placed walking, landing near both sides, wall/ceiling clipping and removal in all four facings. Ground standing-height living probes using Create's actual collision pass on both curved articulated builds, including searches beyond restricted body bounds.
- Pass all 58 Minecraft tests with Steam ’n’ Rails and 56 without it. Preserve existing IDs, dyes, Alpha 4 layouts and schematics; update the guide, preview and portable source.

# 0.5.0-alpha.4

- Lower the example LRT floor-to-ceiling clearance from four blocks to three and move roof equipment down one row. Keep five occupied/visible columns and the streamlined B cab.
- Fix downward-wound cab floor top faces and clipped corner triangles discarded by the model baker. Keep floor texture scale consistent across facets; put driver seats/controls at normal floor level.
- Make LRT leaves plug outward 3.2 pixels before sliding outside the vehicle. Preserve fixed frames, 32/40-tick travel, dyed accents, green/yellow center signals and piano/hiss cues. New doors are 2×2; saved 2×3 doors remain supported.
- Animate joined pantograph arms and the contact head over 40 ticks with interpolation/reversal. Support placed right-click/redstone and safe moving-train right-click/actor updates.
- Restore an articulation floor anchor with overlapping end plates and a three-high pleated gangway between adjacent same-train carriages. Keep all existing Create/S&R bogeys intact; no custom LRT bogey is registered.
- Add 16/8/16 three-car and 16/16 two-cab quick-build schematics, with two/one gangways, doors on both sides, native CRN boards and separate exported glue groups. Refresh the double-ended single-car schematic too.
- Pass 57 Minecraft tests with Steam ’n’ Rails and 55 without it, including the exact exported schematics assembled, curved, and disassembled. Validate 278 actual model bakes, upward floor quads, compact/legacy door clearance, animated linkage and existing kit regressions.
- Update the guide, tooltips, editable meshes, still/animated software previews and portable source. Make the existing cable journey test wait for its actual carriage entity before starting navigation.

# 0.5.0-alpha.3

- Replace the old LRT appearance with the selected streamlined B design and a focused single-car kit: 28 white dyeable modules, a 5×17 example and four clear interior rows.
- Build rounded cab corners and a sloped wraparound windshield from exact atlas-backed baked facets. Add matching cab floor, side-return and roof pieces; automatically propagate end profiles across the cab.
- Slim the connected window frames and use clear uniform glazing, retaining independent glass/solid render layers and plane-aware joins.
- Add continuous white cab headlights, dyeable body LED panels and inset roof ceiling lights. Lower the HVAC profile and keep a folding pantograph.
- Retain two-wide/three-high side doors, retract-then-slide travel, center signals, dyed shut lamps, flashing closing lamps and existing piano/hiss cues.
- Remove the custom LRT bogey/style/menu entry and old articulation/bellows content. Use existing Create/S&R standard-gauge bogeys. Disassemble older LRT trains and replace their compact bogeys before updating.
- Replace fake destination textures with independent cab/side housings for real CRN Advanced Display Boards. Keep CRN optional; the provided schematic requires it.
- Add a shared one-car layout, Create schematic, model-baking check, cab-profile/display-clearance regressions and a native-bogey single-car curved journey test. Cache faceted collision shapes by the properties that affect their geometry.
- Preserve the platform, fare, gondola and cable kits. Include a revised build guide, actual-model preview and editable Blockbench meshes.

# 0.5.0-alpha.2

- Fix randomly missing LRT window glass/frames: each render layer now uses an independent multipart selector. Previously the model array chose one layer at random.
- Connect both glass-corner legs to matching straight windows in all four orientations, and connect stacked corners. Restore only the exposed edge after removing or rotating a neighbor.
- Remove joined internal glass edge faces, preserve the texture scale across panes and use a periodic glass highlight texture with translucent rendering and no ambient occlusion.
- Refresh saved connection flags on load and after neighbor updates while retaining body/accent dyes.
- Add model-layer/UV/edge regressions covering all 256 glazing states and three real-world window connection tests.

# 0.5.0-alpha.1

- Add a five-block-wide modular LRT kit with four clear blocks from finished floor to ceiling, 22.5° angled connected windshield pieces and white dyeable body/seat/light parts.
- Add two-wide, three-high sliding doors for both sides, retaining the gondola travel, signal sequence and piano/hiss cues, with original clearance-adjusted LRT meshes.
- Add folding manual/redstone pantographs, flush-mounted HVAC, roof panels, display housings and underbody trim.
- Register an additional original compact two-axle standard-gauge bogey and optional Steam n Rails menu category; preserve every existing style and block.
- Add opposed floor anchors with a dynamic five-wide/four-high pleated bridge between separate Create carriages. The bridge is visual; no moving collision is added across the gap.
- Retain the full platform/fare/gondola/cable kits and old IDs. Add reproducible original asset generators, Blockbench references, an offline preview and LRT_BUILD_GUIDE.md.

# 0.4.0-alpha.8

- Add an optional Steam ’n’ Rails 0.2.1 integration: a distinct Cable Track item/material, original steel cable models for all 19 track shapes, custom curved-cable render hooks and a compact suspended Cable Grip using monorail routing.
- Create the grip automatically when a player looks up and uses Railway Casing at the first bogey position of a station in assembly mode. Glue a cabin beneath it and operate it with standard Create controls, conductors and schedules.
- Move only wide-door leaves two additional pixels toward the cabin. Preserve all fixed frames/LED models; retract three pixels before sliding so the original clear rail plane and full-cabin clearance remain intact.
- Add small moving center-seam signal lamps on both faces: green opening/open, yellow/off while closing, off once fully closed. Keep dyed closed side strips, top lamps, slow motion and existing piano/hiss cues.
- Preserve white dyeable inventory defaults and legacy 3×2 doors. Pass 42 Minecraft tests with Steam ’n’ Rails and all 40 base tests without it, including real two-station cable travel and destination disassembly preserving six seats and door dye.
- Update reproducible generators, editable assets, preview and activation guide. Graphical client launch was blocked by the workspace display connection; cable/Flywheel visuals and Complementary appearance still need in-game review.

# 0.4.0-alpha.7

- Move closed wide-door leaves one model pixel toward the cabin; keep the entire frame and LEDs fixed. Opening eases through a one-pixel back-step before sliding one block sideways; closing reverses the path. Retain 32/40-tick travel, signal colors and existing 3×2 compatibility.
- Slow the closing piano cadence from 0.28 to 0.45 seconds between notes, retaining the pitches and air hiss.
- Show one plain white base item per part in every creative tab: six platform parts, three fare parts plus two RP items, and twenty-three cabin parts. New/crafted parts use white housing and accents; tooltips explicitly describe dye and crouch-dye controls.
- Preserve registered item/block IDs, stored colors, dyeing, drops, pick-block and Create serialization for existing builds/items. Refresh door geometry, editor assets and the preview; verify the entire two-stage path and white defaults.

# 0.4.0-alpha.6

- Make wide-door side LEDs and top status lights green during opening/open, flashing yellow during closing and the stored accent color once fully shut. Keep the status lights in the header and preserve both dye channels.
- Slow wide doors to 32-tick opening and 40-tick closing, with smooth easing at both ends and continuous reversal. Retain side-only travel and the fixed clearance plane for 3×3 and saved 3×2 doors.
- Replace wide-door iron sounds with a single C5 opening chime or descending E5/C5 closing chime, each layered with an original pneumatic hiss. Broadcast one combined server cue per changed action for redstone/manual blocks, moving clicks and Create door-controller decisions.
- Add light/timing/reversal and moving client/server cue regressions; retain client publishing guards. Suppress inherited vanilla completion sounds on wide doors. Leave single-leaf doors and other transit parts unchanged.

# 0.4.0-alpha.5

- Make both three-wide gondola door leaves slide sideways only, starting immediately with eased travel instead of an initial pop-out step. Apply this to new 3×3 and saved 3×2 wide doors.
- Keep the leaves on a fixed rail plane 0.2 pixels behind the three-pixel frame throughout opening/closing so they clear stationary side panels, accent LEDs, adjacent cabin walls/windows and the inset cabin's front seats. Preserve the centered two-block opening, footprint, colors, redstone and Create interaction.
- Regenerate the wide door meshes, item and Blockbench reference; update the cabin preview. Check all travel positions at 1% intervals against fixed trim, neighboring panels and the complete inset cabin.

# 0.4.0-alpha.4

- Fix the client crash `Cannot send clientbound payloads on the client` when clicking a wide gondola door on an assembled Create train. The interaction accepts the client click so Create can forward it, and only the server publishes door states.
- Guard direct wide-door state publishing against client calls. Publish each changed cell once, invalidate colliders once, and preserve captured block-entity NBT.
- Broadcast one door sound at the clicked door from the server, including the clicking player. Reject missing/replaced interaction cells safely.
- Add regressions for every cell, facing and interaction hand of both new 3×3 and saved 3×2 wide doors. Client-side tests use a Create/Catnip level wrapper and an entity that rejects client publishing; no graphical client is involved.
- Retain the Alpha 3 inset cabin, LED junctions, models, dyes and placements.

# 0.4.0-alpha.3

- Rework the cabin kit around the concept's lower accent band, metal kick plate/crossrail, large glazing, dark door seals and beveled roof.
- Add inset outer corner posts, floor rims and floor corners for a 5×5 occupied block grid with a roughly 3×3 visible cabin. The example floor including trim is 3.375 blocks across.
- Add outer corner vertical and horizontal LED junctions with two cabin-facing lights, accent dye, matching emitter planes, and a bridge that joins stacked vertical runs into horizontal roof trims. Corners use click quadrant placement and inherit stacked corner orientation.
- Make new wide double doors 3×3: centered placement, nine-cell obstruction checking, redstone/dye/pick/break controls and wrench rotation. Retain two one-block leaves and half-block side panels. Side LEDs keep the accent; status stays in the header on both faces.
- Retain existing saved 3×2 wide doors, including their models, collisions and Create interaction, without overwriting the third row. Replacing one adopts the new height.
- Expand Create movement/interaction, block-entity color lookup and client meshes for three rows. Keep the two-high single-leaf door and Phase 1 functionality.
- Expand the Gondolas inventory to 23 parts × 16 housing colors (368 entries), with recipes, colored loot, PBR, generated collision/renderer meshes and editable Blockbench models.
- Verify 35 Minecraft GameTests, including a complete inset cabin with 105 captured Create blocks and six passenger seats. Update the actual-geometry preview and placement guide.

# 0.4.0-alpha.2

- Continue adjacent and stacked cabin-panel facing; crouch-place reverses facing. Support-face and click-height placement for standalone LED trims.
- Add centered, twin and wrapping-corner vertical LED strips, plus single, twin and wrapping-corner horizontal strips. Horizontal trims mount at top or bottom and all seven LED types use the accent dye. Add exposed back LED faces so flush face-mounted trims remain visible.
- Rebuild the beveled roof edge as shallow Roof Stairs, using vanilla stair corner, top/bottom and waterlogging behavior. Match models and collision for all five shapes in all facings and both halves; keep the flat middle roof tile.
- Add a three-block-wide, two-block-high double sliding door placed from its center: two one-block leaves and half-block fixed side panels. Preserve fixed side accent LEDs and top-only status.
- Synchronize wide-door redstone, manual interaction and dyes across all six cells; check the full placement footprint, rotate the whole assembly with Create's wrench and drop one colored item from any broken cell.
- Extend Create station-side door decisions and moving interaction to update the complete wide frame; capture/serialize the assembly as a real Create carriage.
- Refresh client-side cabin door entity state from Create movement updates so top status lights follow opening/closing on contraptions.
- Pass 32 headless Minecraft GameTests and all three asset validators.
- Expand the Gondolas tab to eighteen parts and 288 housing-color entries. Regenerate recipes, loot, editable Blockbench references, PBR maps and actual-geometry previews.

# 0.4.0-alpha.1

- Start Phase 2 with eleven independent modular cabin parts: floor, body panel, flat window, solid/glass corners, roof, beveled roof edge, accent LED pillar, structural suspension hanger, bench seat and sliding door.
- Add a Gondolas tab with sixteen housing variants for each part, separate accent/seat-upholstery dyes, persistent colored drops/pick-block and original labPBR/emissive textures.
- Connect matching flat windows horizontally/vertically and matching glass corners vertically. Use shared geometry for models, collision shapes, renderer meshes, Blockbench references and offline previews.
- Extend Create seats and sliding doors, register passenger-seat tags/movement and moving door interactions, retain station door-control behavior and paired leaves, and keep paired fixed jambs/LEDs at the outer edges.
- Move cabin door leaves out from the wall before sliding, with separate glass/metal passes and fixed accent lights plus top-only status.
- Add crouch-mirrored fare-gate placement in all facings: passage to the entrance viewer's right; keep automatic one-way sensing, independent banks, opposite-end readers, dyes and single drops intact.
- Correct the rear STOP glyph UV orientation in the fare-gate renderer.
- Keep existing fare-gate layouts and old station migration compatible. Cabin cable track/drive bogey and complete station-to-station gondola operation remain the next milestone.
- Pass 25 Minecraft GameTests, including glued cabin assembly and save/reload as a real Create train carriage, and three asset validation scripts.

# 0.3.0-alpha.6

- Rebuild each fare gate as an exact two-column repeating module: one reader post followed by one gate passage.
- Use a glass leaf hinged to its own post, so modules operate independently without an extra housing.
- Connect matching adjacent gate modules and the new one-column Decorative Fare Reader; update connections on placement and removal.
- Add sixteen decorative-reader housing variants, independent housing/accent dyes, reader detail, green approach arrows/LEDs and red rear STOP signs/LEDs.
- Keep adjacent modules intact when a gate or end reader is broken. Hold gates while the rear swing area is occupied.
- Convert saved three-column fare gates into two-column gates plus separate end readers, preserving colors/travel without duplicate drops or moving neighboring blocks.
- Add row connections in all four facings, exact item footprint, independent-lane operation, end-reader removal and old-world conversion checks.
- Update editable models and the actual-asset preview to show connected fare-gate banks.

# 0.3.0-alpha.5

- Add automatic one-way fare gates with two green approach arrows, green approach LEDs, red rear LEDs and STOP signs, paired swinging glass leaves, and a short medium-pitched sampled-piano opening chime.
- Enforce per-player entrance admission while open, retain collision against reverse traffic, hold for occupied passages, and close after a clear delay.
- Add two-block-high ticket/pass kiosks with a server-validated RP menu, free tickets/passes, dispensing animation and shared cooldown.
- Add separate housing/accent dye channels, sixteen creative housing variants, metal labPBR maps, accent emissive strips, colored item tinting and single-drop multi-block lifecycle for both families.
- Keep platform content in its existing creative tab; place the new fare content in a separate Fares tab.
- Add original pixel textures, editable Blockbench models, recipes, RP item icons, reproducible model/texture/audio generation and textured offline previews.
- Correct the platform doorway item's stale four-column tooltip.

# 0.3.0-alpha.4

- Add two-block-high full-glass doors and wall modules, each with all sixteen frame colors.
- Reserve the full row above compact doors for independent CRN Advanced Display Boards/Panels.
- Keep compact status lamps inside a slim top header; side LEDs remain fixed and accent-colored.
- Connect compact and three-block full-glass sections across shared rows, with exposed upper trim and LED segments on taller wall edges.
- Add occupied-display-row placement/preservation, standing-player clearance, mixed-height redstone and all-facing connection integration checks.

# 0.3.0-alpha.3

- Rebuild each full/half-height door as exactly two columns, with independent wall modules.
- Move door and wall geometry/collision from the block center to the facing block edge (0..3 pixels).
- Keep vertical LED strips fixed on both jambs and tint them to the accent dye; remove lights from moving leaves.
- Reserve white/green/flashing amber status behavior for top lamps only; restore top status lamps on half-height gates.
- Give fixed frames a sliding channel so leaves retract behind neighboring walls without intersecting frames.
- Convert saved alpha 1/2 four-column assemblies into a central two-column door and independent walls at the old pockets, preserving dyes and controller state.
- Add two-column placement, edge collision and conversion integration checks; validate sliding clearance across travel.

# 0.3.0-alpha.2

- Join fixed glass across matching door pockets and wall/barrier modules; remove shared borders and align repeating glass UVs.
- Add vertical emissive LED borders from the bottom accent stripe to the glass top on both heights and both sides; moving door borders follow the status lights.
- Show only doors and walls in the creative tab (all sixteen frame colors). Other blocks stay registered to preserve existing placements.
- Replace the two-tone chime with sampled upright piano G5/D5, two semitones below the old A5/E5 tones.
- Remove intersecting frame/motor-post/header/leaf geometry; omit internal glass end faces; draw metal and glass in separate passes.
- Add connection, disconnection, orientation, height mismatch and creative inventory integration checks.

# 0.3.0-alpha.1

First complete platform-door implementation: both approved heights, complete sliding door modules, matching glass wall modules, status LEDs, independent dyes, connected control bus, original closing chime, display mounts, and labPBR materials. Adds reproducible assets and headless integration tests.
