# V15 validation

See `docs/V15_HALLWAY_UPDATE.md`, `docs/V15_VALIDATION.log` and `docs/V15_VISUAL_VALIDATION.log` for current checks, CPU/vertex measurements and full-resource native captures. All 187 required server tests passed; two actual crosshair hover states and six styles were captured and inspected. No shader-pack FPS claim.

# V14.1 validation

`docs/V14_1_REGRESSION_VALIDATION.log`: build/check, all 187 required dedicated-server GameTests, 143,104 native multipart state comparisons over 97 production models, cache lifetime/bypass/concurrency checks, production geometry, UI and audio regressions passed.

`docs/V14_1_JOURNEY_VALIDATION.log`: full-resource graphical client with Sound Physics Remastered, real rider over 60 blocks up and 60 down, continuous Inside radio, stale snapshot position handling, native motor playback, terminal panels and local mute/resync validation. See `docs/V14_1_IMPLEMENTATION.md` for exact scope and remaining gameplay checks. No listening or Complementary shader certification is claimed.

Historical checks follow.

# Current: 0.2.0-dev4

Build and native CLIENT registration/inventory checks passed. All 156 required Minecraft GameTests passed. Geometry tests cover 204 style/layout/size combinations; original 144 cabin settings and nine chimes also pass core checks. See docs/ELEVATORS3_DEV4.md for scope, cleanup-only follow-up and manual runtime limits.

# Elevators 3.0 chime addition: 0.2.0-dev3

Full Gradle build and native CLIENT registration/inventory checks passed. **All 153 required Minecraft GameTests passed** in a fresh world, including the new chime registration/configuration/NBT fixture and the retained movement/door/passenger tests. Core tests verify all nine saved chime selections. External ffmpeg/ffprobe validation passed for all six mono Vorbis clips; provenance hashes match the selected pack and the original Otis/Schindler audio matches dev2 byte for byte. This remains headless validation: audible playback and graphical selector interaction still need an in-game client check. See ELEVATORS3_CHIMES_DEV3_VALIDATION.txt.

Audio validation passed: all six new mono 48 kHz Vorbis recordings decode and match their CC0-pack hashes; each has a sound definition, subtitle and selector label. Original Otis/Schindler bytes match dev2 exactly. The new native fixture covers eight distinct registered cues plus Off, backward-compatible original event IDs, server-side selection and controller NBT persistence.

Graphical GUI interaction and actual in-game sound playback/listening still require a real client test. Some source recordings include doors or background sound. Legacy assembled elevators keep their existing cues. See [chime guide](docs/ELEVATORS3_CHIMES_DEV3.md) and [implementation report](docs/ELEVATORS3_IMPLEMENTATION.md).

---

# Elevators 3.0 model cabins: 0.2.0-dev2

Full Gradle build, core/geometry checks and native CLIENT registration/inventory validation passed. **All 152 required Minecraft GameTests passed in the final fresh-world run**, including five new model-elevator tests. The independent focused run also passed all five elevator fixtures.

Coverage includes all 20 actual cabin entities, native floor/door collision in four mount directions, real entity-data packet encoding, moving controller NBT rebinding to the same UUID, two simulated server-player riders on upward/downward trips, per-floor rear-only door service, and broken-rail fault/repair/resume. These are headless native checks, not a graphical Minecraft client or real connected multiplayer certification.

Actual asset export: 20 primary meshes, 184 distinct door/glazing configurations, 19 finish examples and 27 rendered PNGs. Existing-resource comparison: 3,377 identical files, three deliberate existing-file edits, 46 removals exclusively retired LRT recipes. Release ZIP/JAR byte and checksum audits are performed by `tools/package_elevator3_release.py`.

Earlier runs intermittently timed out in the retained LRT circuit, even with new elevator fixtures excluded; the final full run passed it. New elevator recovery failures found during development were fixed before this release. Shader transparency/PBR, real multiplayer, full restart/chunk lifecycle and hardware FPS still require in-game validation. See [implementation report](docs/ELEVATORS3_IMPLEMENTATION.md), [evidence](docs/ELEVATORS3_VALIDATION.txt) and [building guide](docs/ELEVATORS3_BUILDING_GUIDE.md).

---

Historical results below describe previous releases.

# Elevators 3.0 foundation checkpoint: 0.2.0-dev1

Full Gradle `build` passed, including the new core regression task and the existing transitCheck. Native CLIENT data generation passed, including Flywheel visualizer registration and the gondola-only catalog assertions. This is registration/logic validation, not a rendered window. No new model-based cabin entity is in-world yet. No new multiplayer, passenger, shader or hardware performance tests were run. The previous 147 GameTests were not rerun for this checkpoint. Private proxy/truststore and empty-headless-assets build settings are not packaged.

[Implementation and missing work](docs/ELEVATORS3_IMPLEMENTATION.md)

# Raised elevator call buttons: 0.1.0-alpha.6

Compilation passed. Native Minecraft GameTests: all 147 passed. The new height/link persistence test and updated raised-button travel/obstruction/indicator and shared-bank tests passed. All connected-dye tests also passed.

Only ElevatorCallBlock, ElevatorNetwork and version metadata differ in the JAR from alpha.5. Existing models and other code are unchanged. Graphical placement appearance and live multiplayer interaction have not been manually tested. Tests used the existing optional Steam ’n’ Rails exclusion and private headless environment workarounds; these workarounds are not shipped.

See [patch guide](docs/ELEVATOR_CALL_HEIGHT_ALPHA6.md).

# Connected primary and secondary dye: 0.1.0-alpha.5

Build and client data-generation passed; all 10 dye tests passed. Full suite: 145/146, with an unchanged LRT circuit timeout on both attempts. See [patch guide](docs/CONNECTED_DYE_ALPHA5.md) and [validation evidence](docs/CONNECTED_DYE_ALPHA5_VALIDATION.txt).

# Connected secondary dye: 0.1.0-alpha.4

All 144 required native Minecraft GameTests passed. See [patch guide and limitations](docs/CONNECTED_SECONDARY_DYE_ALPHA4.md).

# Elevator System 2.0: 0.1.0-alpha.3

All 136 required native Minecraft GameTests passed. [Implementation report](docs/ELEVATOR_SYSTEM_2_IMPLEMENTATION_REPORT.md), [building guide](docs/ELEVATOR_SYSTEM_2_BUILDING_GUIDE.md), and [validation evidence](docs/ELEVATOR_SYSTEM_2_VALIDATION.txt). Graphical and live multiplayer checks remain.

# Current validation: Alpha16.5

[Release checks and limitations](docs/Alpha16.5-Validation.txt).

Alpha 11 release verification

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10.
All 77 required headless Minecraft GameTests passed.
Actual model baker: 1420 models / 100184 quads in four orientations.
Dynamic emitters: LRT 318 faces, EMU 225 faces, HSR 108 faces, atlas-safe UVs.
Asset validation: 46 LRT modules / 768 glazing states; 26 HSR modules / 252 models / 844 visual state combinations.
HSR exported CRN/NoCRN coach: 5 x 5 x 31, 513 blocks, 33 native seats, four doors, two standard bogeys at z7 and25 and one glue group.
Exact NoCRN HSR NBT assembled into one native Create carriage; player mounts an outer wall-seat.
Three geometric blocks of interior air; full floor at row0, ceiling at row4. AC and raised pantograph stay within row4.
Native wheel meshes extend below the bogey block; the five-row limit is measured from that block to the roof.
Native seat attachments fit below the ceiling on full-block pedestals.
LRT updated NBT: single319 blocks; three-car726 blocks; two-cab586 blocks. Native bogeys and independent articulation glue groups preserved.
New gameplay checks cover rectangular and L-shaped connected rings, rounded corner/mixed window connections, narrow surrounds and walkable skirt floor tiles.
Native standard bogey horizontal swept radius27.96 pixels; skirt clearance37.6 pixels. Deep covers span two tiles on either side of each bogey.
Original opening two-tone cue and quiet separated closing beeps decoded and checked.
Transparent glass layers, dye retention, no competing floor/wall faces and packaged source-resource bytes validated.
No live graphical client/CRN route-content/Complementary shader certification. Previews use actual shipped mesh definitions.
