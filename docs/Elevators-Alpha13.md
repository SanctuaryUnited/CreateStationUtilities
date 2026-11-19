# Create Transit — Modular Elevators, Alpha13.6

Minecraft 1.21.1 • NeoForge 21.1.248 • Create 6.0.10+mc1.21.1 • Java 21

Replace your previous Create Transit JAR; keep only one version installed. Create and NeoForge are separate dependencies. Steam ’n’ Rails is optional for existing cable tracks; elevators do not require it.

## QuickBuilds

Copy `Elevator-Glass-3x3-Three-Floors.nbt` or `Elevator-Solid-3x3-Three-Floors.nbt` into Minecraft’s `schematics` folder. Use Create’s schematic table to load it, then place or print it with a schematicannon. Each demo fits 9 × 32 × 10 blocks. The cabin’s 3 × 3 footprint has a full-block floor, three clear interior rows and a thin roof in the next row. There are 42 cabin modules.

Platform floor rows are 2, 14 and 26 relative to the schematic. The drive is behind the cabin at (3, 2, 1). Empty-hand right-click the drive to assemble. Click a floor-call button, the drive again, or the inside floor panel to select a destination. Stops queue in order; the cabin eases into and out of motion. Redstone rising edges on floor calls also request stops.

Crouch-click the drive to return home and disassemble. Breaking it restores the cabin at its current rounded block position; use the normal return command when possible.

## Your own cabin

Build with the dedicated Elevator creative tab. Cabin modules must join face to face and have a complete rectangular floor. Place the drive behind a floor module, facing into it. Guide rails go vertically above the drive through the highest cabin roof row. Calls and landing doors go one block above their platform floor row. Add a floor-call block on every served level; these register the stops.

The drive must be at the lowest stop. Calls and landing doors bind to the nearest loaded drive within 8 horizontal blocks and 256 vertical blocks. Avoid overlapping drive detection areas. The panel lists up to 32 stops. Cabin assembly accepts up to 512 modules, within 8 blocks of its starting floor module on each horizontal axis and up to 6 blocks above it. Keep the cabin’s whole rectangular swept shaft volume clear and loaded.

The independent drive needs no pulley, rotational power or superglue. It moves a native Create contraption with Create rider collision. Landing doors close during travel and open only at an aligned stop. Missing rails or an obstruction stop travel; clear the problem and request a stop again.

## Appearance

Glass and solid cabins have dedicated models and materials. Glass panels join at their edges. Doors are one block wide and two high, with two sliding leaves behind a fixed frame. Opening and closing each take two seconds. Side LEDs match the dyed accent; the top lamp flashes green while opening, stays green fully open, flashes yellow while closing and goes dark fully closed. Solid doors use an original classic Otis-inspired opening bell, and glass doors use an original lower-pitched early Schindler HT-inspired opening chime. Both include a quiet air release. The existing Transit light-rail closing warning remains.

Items default to white. Apply dye to change the housing; crouch and apply dye to change accents and LEDs. Dye before assembly; colors survive movement and restoration. The kit also includes a ceiling light, vertical LED strip, header, control panel, guide rail, drive and call button. Rounded sculpted cabins are for a later update.

Normal and LabPBR specular maps are included. The preview renders actual model geometry in software, not an in-game screenshot. Complementary Reimagined reflections, lighting and animated client appearance still need an in-game visual check.


## Alpha13.3: faster travel and front/rear landings

Travel now takes approximately half the former time, with the same smooth starts and stops. A 12-block trip takes 93 movement ticks (about 4.65 seconds at 20 TPS), plus the existing door opening/closing intervals.

Put cabin doors on opposite walls facing outward. Put each landing-door pair immediately outside its matching cabin door, facing inward, with their bottom halves at the same height. Only doorways with complete, correctly facing landing doors open at that stop. Unserved sides stay closed. If both sides have matching landings at the same height, both open.

For front/rear entry, place the drive against a side floor block and stack its rails above it so the drive does not obstruct the rear doorway. The new Glass-Through and Solid-Through QuickBuilds demonstrate front entry at the bottom/top stops and rear entry at the middle. They use a side-mounted drive at (1, 2, 3). Install Alpha13.3 or newer before using these examples; Alpha13.2 opens both cabin doors together.


## Ceiling lighting variants

Five dedicated Elevator roof-light blocks are available in the Elevator creative tab: Ceiling Strip, Ceiling Double Strip, Ceiling Panel, Ceiling Bulb and Ceiling Ring. They replace roof blocks directly and use the same thin roof profile. Rotate strip blocks when placing them to choose their direction. The existing warm Ceiling Light remains available.

Apply dye normally to change the housing; crouch and apply dye to change the new light surfaces. New fixtures emit block light at level 12 and render their light faces at full brightness, including on the moving cabin. Vanilla block-light propagation and shader dynamic lighting are separate from the visible emissive faces.

Adjacent Ceiling Rings on the same roof layer form a larger perimeter. Two rings make a long rectangular fixture; four in a 2x2 arrangement form a larger square. L-shaped arrangements retain their inner corner. Internal borders disappear; removing a ring restores the exposed border. Ring blocks only connect to other elevator rings, and orientation does not prevent connection. Color each participating block to match. The roof panels remain solid; these are ceiling light patterns, not openings through the roof.

## Connected solid cabin walls

Solid walls and corner blocks now form continuous metal panels. Vertical neighbors hide the internal LED trims: only the bottom wall row carries the floor LED band, and only the top wall row carries the roof LED band. Adjacent blocks carry the trim to their edges, including around corners. Metal uses a dedicated seamless wall material and LabPBR maps. Control panels join a matching solid wall column; a door header connects to the doorway below and carries only its roof trim. Place vertical neighbors facing the same direction. Normal dye changes the metal; crouch + dye changes the LEDs.

Existing stationary solid cabins refresh their connection states when loaded. Updated QuickBuilds store the connected states too. The moving cabin retains both connectivity and dye colors. White inventory items, recipes and identifiers are unchanged, so existing cabin blocks remain compatible.

## Alpha13.6: mounting, chimes, dye and loading

Place a floor-call button by clicking a vertical wall face. Its housing and collision sit at the edge of the new block against the supporting block, like a regular wall-mounted button. All four horizontal faces work. The button still registers a floor one block below its own row. It can be used on a freestanding panel for modular builds; it does not require a full support block after placement. Existing buttons retain their saved position/facing, so re-place them on the intended wall face if necessary. The updated QuickBuilds include supporting blocks behind their calls.

The opening sound follows the selected cabin door variant, including moving cabins and standalone/landing doors: solid = original classic bell; glass = original early HT-style electronic chime. No manufacturer recording is bundled. One cue plays per cabin opening transition; unchanged idle checks do not repeat it. If matching glass and solid doorways open together, the solid opening cue takes precedence. The closing cue remains the LRT set.

Every elevator block supports housing dye and accent dye. The floor surface now follows housing dye, guide rails have a tintable dedicated material, and the original ceiling lamp accepts accent dye while keeping a warm appearance with white dye. Both rows of cabin and landing doors now synchronize frame and accent colors. Clear glass, display graphics and functional green/yellow status lights keep their roles. Dye before assembly.

Release loading optimization keeps all 178 block definitions and all items. It excludes 2,332 unused or redundant model files and resolves 707 exact duplicate reachable block models within their own kits. The release contains 1,189 models instead of 3,521 source models, reducing packaged model JSON by 9,064,296 bytes. Every blockstate condition, model transform, texture, tint, render type and complete mesh is preserved. Optional cable partials remain included. This reduces resource loading work; total Minecraft startup seconds have not been measured on your full modpack.

To regenerate: run `python tools/add_elevator_lighting_assets.py`, `python tools/generate_elevator_chimes.py`, `python tools/export_elevator_kit.py`, then `python tools/optimize_loading_assets.py` after any other asset generators. `tools/release-models.txt` controls packaging; update it whenever resources change.

## Validation limitation

95/96 native Minecraft checks passed, including all elevator checks. The existing long LRT circuit roundtrip test timed out in fresh worlds; tracing showed navigation canceled before travel, so that route is not fully verified by this release. Model geometry, references and the other vehicle tests passed. No test assertion was weakened to hide the failure.
