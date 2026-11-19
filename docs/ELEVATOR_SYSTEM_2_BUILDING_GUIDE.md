# Create: Transit — Elevator System 2.0 building guide

Release: **0.1.0-alpha.3** • Minecraft **1.21.1** • NeoForge **21.1.248** • Create **6.0.10** • Java **21**

Install `CREATE Transit Alpha 0.1.0.jar` in the instance's `mods` folder, replacing the previous Create: Transit JAR. Keep Create installed separately. The mod ID remains `createtransit`. Steam ’n’ Rails and Create Railways Navigator remain optional integrations; they are not needed for elevators. Back up an existing world before updating.

## Try the supplied three-floor builds first

The QuickBuild ZIP contains two Create schematics:

- `Elevator2-Glass-F1-F2-F3.nbt`
- `Elevator2-Solid-F1-F2-F3.nbt`

Copy the NBT files into your Minecraft instance's `schematics` folder. Use Create's Schematic Table, Schematic and Quill/Schematic workflow, and Schematicannon to place one in a clear area. The two JSON files are coordinate references, not additional schematics to place.

Each build includes a 3×3 enclosed cabin, continuous guide rails, three enabled dedicated marker rails, cabin controls, call buttons and landing doors. Floor spacing is 12 blocks. The landings alternate **front → rear → front**, demonstrating that floors can open on different sides. The complete schematic occupies **9×32×10 blocks**.

Right-click the Elevator Drive with an empty hand. It assembles the cabin and opens the controller. Confirm F1, F2 and F3 are present, then choose **Go** for a destination. Inside the cabin, right-click the Elevator Control Panel to open the passenger destination screen.

## Build a shaft and cabin yourself

### 1. Build the cabin floor and attach the drive

Use **Elevator Floor** blocks from **Station Utilities → Elevators** to make a complete, solid rectangle. The floor must have no holes. A 3×3 footprint is a convenient starter size; larger rectangles are supported within the limits below.

Place an **Elevator Drive** directly beside one floor block, at the same Y coordinate. Click the side of the floor block when placing the drive: it automatically faces into that block. The block directly in front of the drive is the cabin's assembly anchor.

The drive stays in the shaft. It is not a cabin decoration. No rope pulley, rotational power or superglue is required by this controller.

### 2. Place one continuous guide rail column

Place **Elevator Guide Rail** blocks vertically **directly above the drive**, using the drive's X and Z coordinates. Keep the same mounting orientation throughout. Adjacent rails inherit their neighbor's orientation and connect above and below automatically.

Replace the rail at every stopping floor with the separate **Elevator Floor Marker Guide Rail** block. This is a dedicated block/item, not a right-click conversion. Its recipe is one regular Elevator Guide Rail, one redstone dust and one paper in a shapeless crafting recipe.

A marker sits at the cabin's **first air/door threshold row**, one block above its floor blocks:

| Stop | Platform/cabin floor block Y | Marker rail Y |
| --- | ---: | ---: |
| F1 | 64 | 65 |
| F2 | 76 | 77 |
| F3 | 88 | 89 |

For this example, put the drive at Y=64 and keep rails continuous from Y=65 through Y=92. A standard five-row cabin has its roof blocks four rows above its floor, so the rails must extend through that roof row at the highest stop. Taller cabins need correspondingly more rail above their highest marker.

The drive is installed at the lowest served floor. This implementation scans upward from it. Basement labels such as B2 or B1 are supported, but their physical stops must still be at or above the drive.

### 3. Enclose the cabin

Build walls, glass, corners, door headers, roof panels, ceiling lights and a control panel on the floor. The supplied glass and solid elevator kits are compatible, including the connected solid-wall trim and ceiling lighting variants.

Ordinary Minecraft walls, glass and decoration blocks can also join the assembly when they are face-connected to the floor/walls and remain inside the floor's rectangular footprint. A detached lamp or decoration must be connected to the captured assembly.

A useful layout has floor blocks at Y, three interior air rows at Y+1 through Y+3, and the ceiling/roof row at Y+4. Keep exterior decorations inside the footprint. The moving assembly cannot capture blocks below the floor.

Only compatible elevator block entities can be carried. Chests, furnaces and unrelated machines with block entities are currently rejected with an assembly error. Bedrock and other immovable blocks are also rejected.

### 4. Install cabin and landing doors

Use the existing **Glass/Solid Elevator Cabin Door** and the matching **Glass/Solid Elevator Landing Door** blocks. Placing a door creates its upper half automatically; leave two rows clear.

- Put cabin doors within the floor footprint, starting at floor Y+1.
- A cabin door faces outward from the cabin.
- Put the landing door exactly one block outward from that cabin doorway, at the same horizontal position relative to the cabin and the same two vertical rows at each stop.
- The landing door faces inward, opposite the cabin door.
- Both halves of each door must exist and face the same direction.

Place cabin doors while looking inward from outside the cabin; place landing doors while looking outward from the cabin/shaft side. Verify the faces before assembling.

A cabin can have front and rear doors. A floor needs at least one complete matching doorway; only the matching side opens there. The other cabin doors stay closed. A door-equipped cabin marks a stop inaccessible if there is no complete matching landing door. Freight platforms built without cabin doors do not require landing doors.

Leave the whole moving cabin volume clear throughout the shaft. Platforms and shaft walls belong outside this volume. Collision checking conservatively reserves the cabin's complete rectangular volume, including interior air.

## Assemble and configure

Right-click the drive with an **empty hand**. The controller attempts assembly and opens its actual Minecraft configuration screen. If assembly fails, the screen displays the reason. Fix it and use **Assemble**.

The screen shows the elevator name and identifier, current floor or “Between floors,” destination, operating state, rail status, request queue and detected stops. The floor list is displayed highest first and paginated when needed.

Each row includes **floor ID, floor name, service enabled, Edit and Go**. Select **Edit**, change the ID/name or service toggle, and save. Labels are independent of height: F1 can be renamed G, B1, Mezzanine or another short ID. IDs must be unique, 1–12 characters; names use 1–32 characters.

Open **Settings** to change the elevator name and automatic door delays. Values use Minecraft ticks; **20 ticks = 1 second**.

| Setting | Default | Accepted range |
| --- | ---: | ---: |
| Opening delay after leveling | 5 ticks | 0–200 ticks |
| Closing delay after door animation | 5 ticks | 0–200 ticks |
| Door-open dwell before the next queued trip | 40 ticks | 0–600 ticks |

Door mode is **AUTO**. The existing opening/closing animation lasts 40 ticks. Leveling keeps doors closed for at least five ticks, even if the configured opening delay is shorter. Existing glass and solid arrival/opening chimes are retained.

Floor markers are rescanned automatically on changes and periodically while loaded. **Rescan** updates the list immediately. You can also right-click a connected marker rail with an empty hand to open its controller at that floor.

A disabled floor remains visible for configuration but cannot be selected for ordinary passenger service. Disabled or inaccessible requests are removed from the pending queue.

## Call buttons and passenger controls

Place an **Elevator Floor Call Button** against a wall at floor Y+1, Y+2 or Y+3 (the marker rail row or either of the two rows above it). Right-click it to summon the elevator; a rising redstone signal also makes a request. An amber indicator shows a pending request and clears when service begins opening the doors.

For one nearby shaft, an unlinked button automatically finds the unique elevator that serves the nearest floor below the button within that three-row mounting range. A call button does not create a floor: the corresponding connected marker rail must exist.

For close neighboring shafts, link buttons explicitly:

1. Right-click the desired **marker rail with Create's Wrench**, without crouching, to select it.
2. **Crouch-right-click the call button with the Wrench** to add that marker link.
3. Repeat with a marker belonging to another elevator to make a shared call button. It summons each explicitly linked elevator; intelligent dispatch is a future feature.
4. Repeat the same marker link to remove it. A button accepts only one stop per elevator.

For any other button height, use an explicit marker link as above: the linked marker chooses the destination, independently of the button’s height.

Dedicated marker rails bind to their own connected drive automatically. For a direct controller link instead, select the drive with the Wrench, then crouch-wrench the button; the nearest floor below it within the three-row mounting range selects the stop. Selecting a drive clears the previous marker selection.

**Crouch-right-click a linked call button with an empty hand** to clear its links. Links are saved in the world. Explicit missing, disconnected or unloaded targets never fall back to an unrelated shaft.

Inside the moving cabin, right-click the **Elevator Control Panel**. Its passenger screen shows the controller's current enabled destinations and sends requests to the exact owning controller. Passenger panels do not permit configuration changes.

Requests use a FIFO queue. Repeating the same pending destination does not add another trip. After arrival, doors remain open until another request is ready; elevators do not continually cycle like scheduled trains.

## Operation and editing

The normal server-side states are:

**IDLE → DOORS_CLOSING → MOVING → LEVELING → DOORS_OPENING → DOORS_OPEN → IDLE**

Before traveling, the controller closes cabin and landing doors and waits for the closing animation plus its configured delay. Living entities obstructing a doorway hold/reopen the doors and preserve the queued request. All managed doors stay locked during movement and leveling. At arrival, only complete, correctly aligned door pairs open. Landing doors cannot stay open when no aligned cabin exists.

Use **Return / edit** in the controller, or crouch-right-click the drive with an empty hand, to travel back to the assembly floor and restore the cabin blocks for editing. Maintenance return can reach a disabled home floor; ordinary passenger requests cannot.

Dye supported elevator blocks before assembly: normal dye interaction changes the body and crouch-dye changes the accent/LED channel. Existing glass, metal, ceiling and door dye behavior is preserved.

Do not remove the drive during travel. Keep the complete shaft and controller chunks loaded. If the owning controller is unloaded, cabin motion pauses; saved movement resumes when the controller/cabin are loaded and rebound. Broken rails, obstructions and unloaded paths stop/reject travel with a useful fault rather than teleporting the cabin to its destination.

## Troubleshooting

| Message or symptom | Check / fix |
| --- | --- |
| No connected marker rails | Place the dedicated marker rail in the drive's exact vertical column. Keep every rail between it and the drive present. |
| Gap / different mounting orientation | Repair the first broken rail or facing mismatch. Regular and marker rails must share orientation. |
| Extend rails through the roof | Continue rails above the highest marker through the cabin's roof height. |
| Drive must face an Elevator Floor | Put the drive beside the floor at the same Y; place it against the floor's side. |
| Cabin floor has a hole | Fill the entire rectangular floor with Elevator Floor blocks. |
| No complete matching landing doorway | Check position, facing and both halves of at least one landing door at that floor. |
| Floor is disabled / duplicate ID | Enable service or assign unique floor IDs through Edit. |
| Doorway obstructed | Clear passengers/mobs from the doorway; the queued request is retained. |
| Shaft obstructed | Remove blocks from the complete cabin sweep, including roof and interior clearance. |
| Unloaded shaft/controller/cabin | Load all relevant chunks before requesting travel or retrying. The system does not force-load them. |
| Button cannot find an elevator | Check the marker's physical height and enabled service; wrench-link ambiguous neighboring shafts. |
| FAULT after repair | Use Rescan, then Retry. Select a destination again. Retry is only available in FAULT. |
| Unsupported block entity | Remove chests, furnaces or unrelated machines from the cabin; use supported elevator modules and ordinary decoration blocks. |

## Current limits and compatibility

- Up to **32 discovered stops** and **256 blocks of vertical travel**, subject to world height and loaded chunks.
- Cabin floor must be rectangular. Assembly reach is **8 blocks horizontally from the anchor** in either X/Z direction, with **512 captured blocks maximum** and **six block rows maximum above the floor**.
- One continuous rail column belongs to one stationary drive. Separate columns keep separate floor lists.
- Old elevator block IDs, mounting, dye channels, sounds and four earlier elevator schematics remain available. Elevators that have never used a dedicated marker rail retain the earlier floor-marker/call-height behavior. Once converted to dedicated rail mode, the controller requires rail markers and will not silently switch back.
- Escalator/travelator models, dynamic placement, redstone behavior, inventory icons and creative tabs are preserved.

This release was compiled and tested with native Minecraft server GameTests and client resource/registration validation. A graphical player session is still required to check the visible controller layout, subjective animation appearance, shaders and real client riding/network behavior on your setup. See the implementation report for the exact checks.
