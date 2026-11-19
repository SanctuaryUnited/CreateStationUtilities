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
