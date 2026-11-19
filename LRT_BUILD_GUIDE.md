# Alpha 11 LRT expansion

The LRT range now contains **46 white dyeable modules**, with its existing cab,
doors, pantograph, horn and articulation preserved.

- Ceiling fixtures: strip, double strip, rectangular panel, round LED bulb and
  connecting ring. Place these in the roof row, facing the same direction as the
  surrounding panels. Ring tiles form larger outlines and close the inner
  corners of L shapes; shared edges disappear. Dye changes the housing; crouch + dye changes the LED color.
- Windows: panoramic white frame, narrow solid surround, divided glass, tinted
  glass, frosted glass and rounded corner. Compatible full-width panes connect
  across matching planes. Narrow panes retain their own surround. The rounded
  corner accepts the two appropriate outward-facing window runs.
- Skirts: Body Skirt, Bogey Skirt, Skirt End, Skirt Access and Cab Skirt. These
  replace floor-edge tiles and include their walkable floor surface. The Bogey
  Skirt extends lower around each bogey; use lighter Body Skirts elsewhere.
  Cab Skirts follow the existing sculpted cab floor profile.

The single-car and both articulated QuickBuilds now use panoramic glazing,
new ceiling fixtures and skirts. Native Create/S&R standard bogeys remain at
their previous positions; no bogey or bogey style is replaced. The old basic
Underbody Skirt remains available for existing builds. LRT's roof equipment
and vehicle height are preserved; the five-row height remodel applies to HSR.

The previews are actual mesh renders. Complementary shader reflections/bloom,
live client appearance and native CRN content still need an in-game visual check.

---

# LRT B — shorter vehicles and quick builds, 0.5.0-alpha.6

The streamlined LRT body remains five blocks wide. The shorter Alpha 4 layout keeps the cabin and roof one block lower: **three clear blocks from the finished floor to the ceiling**, with roof equipment above. New doors are two blocks wide and two blocks high, leaving a display row above. The creative tab contains **30 white dyeable modules**; use existing Create or standard-gauge Steam ’n’ Rails bogeys.

Cab floor top faces now render upward, including the clipped corner facets, with a consistent grip texture. Door frames stay fixed while the leaves plug **outward** before sliding along the outside of the vehicle. Pantographs raise and fold smoothly over two seconds. Opposed floor anchors connect adjacent carriages with a three-high flexible gangway.

## Alpha 6 additions

Dye either **Articulation Anchor** like any body module: right-click with dye for the pleated fabric, crouch + dye for narrow ridge accents. The anchors retain both colors through saving and carriage assembly. The dynamic bellows use lit fabric textures, alternating fold shading and per-segment world lighting; Complementary Reimagined can use the fabric normal/specular maps. Dye both opposing anchors alike for a continuous joint. Existing anchors remain white by default.

Place the **Roof Horn** on a glued carriage, ideally above each cab. While driving with Create controls, **hold left Shift** for a continuous Create-style train whistle; release it to stop. Create’s usual sprint-key whistle also works when the horn is present. Cab nose lamps turn white at the leading end and red at the trailing end as direction reverses; placed, unassembled cab pieces show white. Newly exported quick builds include roof horns. Existing vehicles can receive one by disassembling, placing and gluing it to the car, then assembling again.

## Alpha 5 fixes

Door glass now uses sprite-local UV coordinates, fixing the random neighboring block textures visible on the moving leaves. This applies to new two-high doors and saved three-high doors. The same UV mapping is used for generated models, Blockbench meshes and moving rendering.

Gangways now have **physical floor, side-wall and ceiling collision**, generated from the same flexing shell as the bellows. Placed gangways participate in normal Minecraft movement. Assembled gangways participate in Create's passenger collision pass and move with the carriage. Passenger search bounds include the gangway beyond the rigid body, so stretched joints stay discoverable. Passage ends remain open.

Replace the JAR on client and server. Your Alpha 4/5 builds and NBT files can stay as they are; add a horn to older cars to enable the Shift control. Keep each carriage glued separately.

## Install the patch

Replace the previous Create Transit JAR with `create-transit-0.5.0-alpha.6.jar` on client and server; keep only one version. Tested with Minecraft 1.21.1, NeoForge 21.1.248 and Create 6.0.10. Steam ’n’ Rails 1.21.1 NeoForge port 0.2.1 is optional for LRT and required for the existing cable track kit. Create Railways Navigator (CRN) is separate and supplies the actual Advanced Display Boards.

Existing saved three-high LRT doors retain their height and work with the corrected outward motion. When upgrading an Alpha 3 or earlier tall layout, existing roof blocks stay where they were placed: rebuild its roof and cab header one row lower, or use the Alpha 4 quick-build schematics. Before updating a pre-Alpha-3 train with the retired custom LRT Compact Bogeys, disassemble it and replace those bogeys with normal Create/S&R bogeys.

## Choose a quick build

Body lengths below exclude the two-block gap at each gangway. Both examples have a cab at each outer end, doors on both sides, white bodywork, blue accents, normal Create small bogeys and real CRN boards.

| Schematic | Cars and body lengths | Gangways | Total occupied grid | Doors | Bogeys | Seats | CRN boards |
|---|---|---:|---|---:|---:|---:|---:|
| `LRT-Quick-1-Three-Cars.nbt` | Cab 16 + middle 8 + cab 16 | 2 | 5 × 6 × 44 | 10 | 6 | 34 | 26 |
| `LRT-Quick-2-Two-Cabs.nbt` | Cab 16 + cab 16 | 1 | 5 × 6 × 34 | 8 | 4 | 26 | 22 |
| `LRT-B-Single-Car.nbt` | Double-ended single car 17 | 0 | 5 × 6 × 17 | 4 | 2 | 14 | 14 |

The short middle car in Quick Build 1 has **one centered doorway on each side**. Each 16-block cab car is exactly twice its eight-block body length. Seat counts include two driver seats per train. The updated schematics include a Roof Horn above each cab and separate native superglue groups for each car. They include no station, rails, conductor or schedule.

## Load the NBT in Create

1. Install CRN before using these schematics, because they contain native CRN boards.
2. Place a **Schematic Table**. Use its **Open Folder** button to find the correct Minecraft instance's `schematics` folder, then copy the chosen `.nbt` file there.
3. Return to the table, refresh the list, insert an Empty Schematic, select the build and upload it to the schematic item.
4. Hold the loaded schematic. Use **Left Alt** to choose placement, movement and rotation tools. Align the centerline over straight standard-gauge track, with rails directly below the schematic's bottom floor/bogey row.
5. In Creative, choose **Print** and right-click to place it. For Survival, use the Schematicannon and supply the required parts.

The quick builds contain real glue entities. When using a placement method that omits entities, recreate glue around **each car separately** after building. Never glue across either gangway gap: the bodies need to assemble as separate carriages to bend.

## Assemble and drive

Link a Create station to the straight track and enter assembly mode. Allow enough straight assembly area for the whole train. The six/four bogeys and all body, door, roof and display cells must be included in their respective car's glue group. Assemble Train: Quick Build 1 should have **three carriages**, and Quick Build 2 should have **two**.

Use the normal Create controls from either cab. You can choose existing standard-gauge S&R styles through its usual bogey menu; this kit registers no replacement bogey or LRT track type. Add a conductor and schedule if you want automatic operation. Create's door controls select which side opens at stations. Keep adjacent outside wall space clear for the sliding leaves.

## Build or modify the modules

Coordinates are local to a north-facing car, x=0–4. The finished floor is at the top of row 0; clear interior rows are 1–3; the roof starts at row 4; roof equipment mounts in row 5, with its base touching the roof below.

| Row / position | Modules |
|---|---|
| Row 0 | Floor Tiles; Shaped Cab Floors across the five-wide cab end; native bogeys replace their floor cells |
| Cab end, row 1 | Five Sculpted Nose modules facing outward |
| Cab end, row 2 | Five Wraparound Windshield — Lower modules; the new short cab needs no Upper row |
| Cab end, row 3 | Cab Display Bay Corners at x=0/4; three native CRN boards at x=1–3, with housings one cell inward |
| Cab return sides, rows 1–3 | Cab Side Body, Cab Side Glazing — Lower, Cab Side Header |
| Ordinary sides, row 1 | Horizontal LED Body Panels, except at door openings |
| Ordinary sides, rows 2–3 | Connected Slim Windows; native two-wide CRN boards above door openings |
| Doorways, rows 1–2 | Two-Wide Sliding Door; one item places all four cells |
| Roof, row 4 | Swept Cab Roof at the cab end; it spans two cells inward. Regular Roof Panels and outward Beveled Roof Edges elsewhere |
| Equipment, row 5 | Pantograph, modular low-profile air conditioners, and Roof Horn at x=2 near each cab |
| Driver | Seat at row 1, with native Create controls one cell ahead on a Cab Controls Base in floor row 0 |
| Interior | Seat modules at row 1; retain an aisle along the centerline |
| Inner carriage ends | Opposing Articulation Anchors in the center floor cell, facing each other across a two-block gap |

For a 16-block north cab, z=0 is the cab end; its swept roof occupies z=0–1. Bogeys are at z=3 and 12, and two-wide door openings at z=5–6 and 10–11. The driver's seat is at x=2, row 1, z=2; controls are at z=1. Reverse the rear cab's end pieces and driver controls.

For an eight-block middle car, bogeys are at z=2 and 5, and the doorway is at z=3–4. Place anchors at both ends. The matching paired floor halves overlap across the straight two-block gap; pleated sides and ceiling follow the adjacent carriage transforms on bends. Carriages must belong to the same train. The physical floor, sides and ceiling update from the same interpolated carriage frames as the rendered bellows. Create carries grounded entities with the owner carriage; normal movement provides support when the build is placed as blocks.

For west-side doors place the item at the southern bottom cell, facing west; it extends one column north. For east-side doors place it at the northern bottom cell, facing east; it extends south. Leave the full two-by-two opening clear. Cab profiles and matching glazing joins update automatically when the adjoining pieces are placed.

## CRN displays

The kit's housings are physical backing and trim. Place **actual CRN Advanced Display Boards** in the bays and configure them with CRN's wrench. Use Train Destination for exterior boards or Passenger Information for additional interior boards.

| Bay | Board size | Alignment |
|---|---|---|
| Outer cab | 3 × 1 | `z_alignment=positive`, inward half of bay |
| Above side door | 2 × 1 | `z_alignment=negative`, outer half flush with body |

For cab boards, click the housing's outward face. For side boards, click the roof underside near the outside edge while looking inward. Neighboring boards must have the same facing and alignment. A glow ink sac enables emissive text. CRN controls the live text, route data and line colors. The mod's body parts work without CRN, but the supplied schematics require it.

## Motion, lighting and dyes

- Opening plugs the leaves **3.2 model pixels outward**, then slides each leaf one block sideways. Opening takes 32 ticks; closing takes 40 ticks with reversible smooth travel. The fixed frame and its lamps stay put.
- Door side and top lamps glow green while opening/open, flash yellow during closing and return to the accent dye once shut. Center-seam lamps are green when open, flash yellow during closing and turn off when shut. Existing one-note opening/two-note closing piano cues and pneumatic hiss are retained.
- Empty-hand right-click or redstone raises/folds a placed pantograph over 40 ticks. Right-click also works on an assembled train. Its joined arms and contact head animate continuously and reverse without snapping. This is cosmetic roof equipment; it does not simulate electrical propulsion.
- All items start white. Right-click with dye changes the housing; crouch + dye changes accents, LED colors and upholstery. Both channels survive drops, saving and Create assembly.
- Cab nose lamps switch between white at the leading end and red at the trailing end on moving trains. Dyed body LEDs and inset ceiling lights remain. The fabric bellows have their own labPBR normal and specular companions for Complementary Reimagined.

## Validation and previews

All 59 Minecraft GameTests pass with Steam ’n’ Rails and all 57 without it. The tests load the actual exported glue entities and schematics, assemble one/three/two carriages through native Create stations, travel on a real curved standard-gauge route and disassemble with the correct seats, doors, gangways and dyes. Real Entity.move probes land on and cross the full-width placed gangway in all four facings, stop at side walls/ceiling, and fall normally after both anchors are removed. During both curved train journeys, Create’s real collision pass grounds standing-height living probes across the bending gangway and registers them for carriage movement, including positions outside restricted rigid-body search bounds. Headless client probes check pantograph ticks, interpolation, reversal and moving interaction. The actual NeoForge loader bakes 280 models and 17,684 quads in four orientations, including upward-facing cab floor facets. The actual dynamic vertex emitter checks all 318 moving mesh faces, including 24 glass faces, for sprite-local coordinates.

`preview/lrt-models.png` shows both quick builds and details using the same mesh definitions as the mod. `preview/lrt-animation.gif` shows the outward door and hinged pantograph paths. These are software previews. A graphical Minecraft client is unavailable here, so CRN's live text, Complementary reflections/bloom, Flywheel appearance and full-modpack visual behavior still need in-game review. CRN is not installed in the automated test runtime; native board geometry and schematic IDs are checked independently.
