# Create: Station Utilities V17.7

Development build for Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10, and Java 21. Use V17.7 on both clients and servers.

## Cabins and landings
 
- New elevators default to a five-second automatic door-close timer. Existing elevators keep their saved delays. The doorway sensor covers a quarter-block zone at each enabled entrance.

- New cabins use a nominal 48 × 48 model-pixel footprint and stand 2.8 blocks tall. Solid, Rear Panoramic, Scenic, and Cylindrical retain their familiar shapes. The model adapts the V13 silhouettes and lighting details to the current collision system and V13 face renderer.
- Left, right, and rear wall faces sit 1 px inward. Each entrance has two 7 px cabin jambs and a 32 × 32 px clear opening. The floor, jambs, and header form an 8 px passage behind the moving cabin doors. Cabin and landing panels slide next to each other across the threshold with a small clearance. Door movement and collision use the same geometry.
- An enabled rear entrance brings its frame and sill out 1 px to the landing plane and mirrors the front controls, display, doors, and lighting. Cylindrical cabins use front access. Through cabins need a guide rail beside the shaft, clear of both doorway centerlines. Rotate the deactivated cabin in Alignment if the rail is directly behind the cabin.
- Landing frames stay anchored to their placed blocks. Standard, Flush, and Recessed use different lobby-side depths and share a fixed shaft-side edge. New landings default to Flush. The hallway header alternates between floor ID and name every 3 seconds. Arrows and short floor plaques sit on the inner jamb faces. Brighter dyeable LEDs remain on the hallway side. The shaft side is plain metal. Both enabled cabin entrances carry direction arrows on their inner jamb faces.
- The in-cabin touchscreen shows the active destination and accepts floor taps. Six floors fit per page. Aim at the panel and scroll, or tap the arrows beside the page number. Both cabin screens share the page and stop queue. The taller cabin header above the two-block opening shows the destination ID and Y level while moving and the full floor name at a stop.
- The original V13 face renderer draws the cabin, landing, and previews in either geometry mode. It emits each authored face directly with its supplied UVs and keeps separate solid and glass passes plus full-bright LEDs. Current texture lookup, brightness, and display overlays feed that renderer.

## Shared service and destination selectors

1. Activate each cabin and install its landings. Place a Floor Call between two nearby landings on the same hallway side. The panel detects the pair and queues Up and Down independently.
2. At the lowest shared floor, use a Create wrench on the panel and choose **Create bank**. Wrench the next panel and choose **Add adjacent shafts** to extend the selected bank. **Select bank** switches which bank is being extended. Upper-floor panels beside the bank detect service automatically.
3. Use the same wrench menu for shared panel dyes, arrow style, and header style. Cars are assigned letters in bank order, displayed on the landing headers.
4. Place an **Elevator Destination Selector** beside a landing of the linked bank and aim at its screen. Browse the reachable floors with the arrows or mouse wheel. Select a floor to see the assigned shaft letter. The physical screen page is shared by nearby players. Sneak + right-click opens the larger menu with individual browsing. The selected car arrives, waits for boarding, and takes the destination request.

Floor discovery, configuration, and dispatch use world geometry and saved
serviceable stops. There is no fixed mod cap on the number of floors or cars.

## Setup defaults and configuration clipboard

New elevators start with V13 Brushed Steel, light level 6, and a maximum
speed of 0.5 blocks per tick. Saved values load as before.

Use **Elevator Driver → Setup → Copy Config** to copy cabin, motion, door,
indicator, sound, and radio settings. **Paste Config** shows a confirmation
preview. Stop the target before applying, and match its rear entrance layout.
The clipboard excludes floor stops, landing links, shaft placement, and calls.

## Original V13 textures

The elevator uses V13 artwork in both existing skin slots. The former Gunmetal
slot now displays **V13 Charcoal Steel**, using the original industrial frame
texture. The former Stainless slot displays **V13 Brushed Steel**, using the
original standard frame. Cabin walls, floors, trim, door leaves, glazing, and
LEDs use original V13 tiles in both geometry modes. The tall door textures
map to the moving faces as single sheets.

## Upgrade and rollback

New cabins start in V17 geometry. Previously saved cabins keep their current geometry until the owner uses **Elevator Driver → Setup → Update Cabin to V17**. Stop and empty the cabin and doorway first, then finish any queued trips. Modern V17 cabins gain the taller shell when loaded. Existing 3 × 3 cabins keep their controller, links, settings, and position.

Choose **Setup → Revert Cabin to V13 Geometry** to restore the compact V13 shell, door frames, door positions, and collision geometry. The compact V13 mesh is centered inside the existing footprint so the controller, cabin entity, and landing blocks keep their positions and links. Current landing assemblies and indicators continue to operate. Use **Update Cabin to V17** to restore the V17 entrance and touchscreen. A saved V16 profile stays readable and can switch directly to V13 from Setup.

For old 2 × 2, 4 × 4, or 5 × 5 saves, deactivate the cabin before conversion and reinstall matching 3 × 3 landing assemblies afterward. Clear any old blocks or rear frames that intersect the new cabin envelope before retrying a blocked conversion. The compatibility path retains the old configuration until a conversion succeeds. The geometry revert uses the 3 × 3 footprint.

## Build and validation

Run `./gradlew build` with Java 21. The focused native test command is `./gradlew runGameTestServer -Pelevators30Validation -PwithoutRailways`.

See the release validation summary for the checks run on this build. Interactive client rendering, shader compatibility, and a live two-client session require separate verification.
