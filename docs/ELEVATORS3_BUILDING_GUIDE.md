# Elevators 3.0 — building and operation guide

For `create-transit-0.2.0-dev3.jar`, Minecraft 1.21.1 / NeoForge / Create 6.0.10. Install the same Transit build on client and server and keep only one Transit JAR. Back up an existing world before upgrading this development release.

## 1. Place the controller and plan the shaft

Get **Elevator Cabin Kit** from Station Utilities. Its recipe is eight iron ingots around an Elevator Controller/Drive. Right-click the ground with the kit to place a controller and open model setup. Use the kit on an existing drive or its guide rails to open the same setup.

Choose an external footprint: 2×2, 3×3, 4×4 or 5×5. The guide rail mounts on a side, outside the cabin. The drive's mounting direction points from the guide into the cabin; the cabin entrance is perpendicular to that direction. Hold the kit and look at the drive to inspect the actual footprint: green means the loaded placement volume is clear, red means blocked/unloaded. Rotate/reposition the controller before activation if the entrance faces the wrong way.

The controller's Y is the cabin floor grid row. The standing floor surface is at Y+1, with three blocks of clear interior air and the ceiling above. Clear the whole cabin shaft volume. Do not place individual cabin floors, walls or ceilings for the new model system; the cabin entity supplies them.

## 2. Build the guide rail column and marker floors

Run a straight, compatible vertical Elevator Guide Rail column above the controller. Replace the guide rail at each stop with the **dedicated Elevator Floor Marker Guide Rail**, not a separate marker beside the shaft.

A stop whose floor grid row is Y needs its marker at Y+1 in the guide column. For a three-floor example, put markers at controllerY+1, +13 and +25: standing surfaces are controllerY+1, +13 and +25, and the floors are 12 blocks apart. Extend continuous guide rails through the upper cabin roof; reaching at least five blocks above the highest floor grid row is a simple safe rule.

The inherited discovery limits are 32 floors and a 256-block scan/travel range, within the world's build limits. This is not a hardcoded three-floor elevator. Each floor entrance needs at least three blocks between served floor grid rows.

## 3. Configure cabin appearance and floor service

Open the controller. Select the cabin style, front/rear solid or glass doors, colors, side/curved windows, wood/floor block textures, ceiling lighting and handrails. Structural changes (size, style, rear entrance) require deactivation. Cylindrical cabins have front service only. Enabling rear doors replaces the panoramic rear window.

The floor list comes from the connected rail column and is ordered by height. Use Edit to set independent IDs/names such as F1 / Floor 1, F2 / Floor 2 and F3 / Floor 3. Enable the floors that should accept passenger requests.

For each floor, choose front, rear or both service sides, and whether each landing uses matching cabin, solid or glass doors. Floors may face different service sides. Leave rear service off unless the cabin rear entrance is enabled.

Press **Install/update landing doors** at every served floor. This creates an owned entrance N blocks wide and three blocks high, where N is the cabin width. Leave that doorway volume clear and accessible to the builder. Installation refuses obstacles, protected cells and another shaft's landing. Reinstalling removes old owned parts at that floor, including parts left by a previous larger size.

Install the home-floor entrance before activation. Keep all destination entrances installed before requesting those destinations. The 2×2 and 3×3 cabins have a one-block doorway; the 4×4 and 5×5 cabins have a two-block doorway.

## 4. Activate and ride

Press **Activate**. The controller checks the home marker, matching landing, loaded footprint and clearance, then creates the complete model cabin. No superglue or block contraption assembly is required.

Enter the cabin. Empty-hand right-click the cabin body to open its passenger floor selection. Choose an enabled destination; requests are queued and duplicates coalesced. Doors close and confirm before movement. At the destination the cabin levels accurately and opens only the configured front/rear sides. Standing passengers move continuously with its floor.

Configure movement speed (greater than 0 and at most 1 block/tick; default 0.2), opening/closing delays (0–200 ticks), chime and door/motor audio in the settings pages. The chime button cycles through the two original Otis-style and Schindler HT-style cues, six CC0 additions (Arrival ding, Elevator bell, Japan arrival, Classic bell + doors, FM bell and Synth ding), and Off. See ELEVATORS3_CHIMES_DEV3.md for credits and controls. The Blocks sound category controls playback. Door delays are additional to animation time.

## 5. Call buttons and dye

Use the existing Elevator Call Button at a landing. Automatic association supports buttons one or two blocks above its marker. For other heights, use the existing wrench linking workflow to bind a marker/controller explicitly. Existing raised-button and bank links remain compatible; they are not reset to ground height by this update.

Requests illuminate the existing call indication. A landing door also requests its owning elevator when used. Disabled floors are unavailable for normal passenger service.

Dye the cabin body normally for its primary channel; crouch with dye for the secondary channel. Landing dye applies to every member of its entrance and preserves the other channel. Connected block-dye features remain available for normal Transit blocks; the model cabin is one entity and is colored as one cabin.

## 6. Reconfigure or replace

Use Return / edit to request the controller's home floor. Keep that floor enabled. Exit the cabin and clear the doorway; deactivation refuses occupants or an obstruction. You can also deactivate an empty, aligned, stationary cabin, then change its structural settings and reinstall affected landings. An active controller cannot simply be broken.

For an old assembled-block elevator, first return and disassemble it with its legacy controller so its blocks are restored. Remove its old physical cabin body before activating a model cabin in the same space. There is no automatic conversion of old moving contraptions.

## Troubleshooting

| Message/symptom | Check |
| --- | --- |
| No connected markers / broken rail | Use a single continuous compatible guide column directly above the drive; dedicated marker rails belong inside it. Rescan. |
| Footprint obstructed | Remove ordinary blocks from the full cabin sweep, check mount orientation and load the nearby chunks. Other cabin entities also block placement/travel. |
| Missing matching landing | Install/update the selected floor's configured entrance sides. Check their size and orientation. |
| Doors do not confirm closed | Clear the doorway and restore any missing landing cells. Confirm both served sides have complete entrances. |
| Travel fault after rail break | Repair the rail path, then Retry. Retry rescans floors before resuming the saved destination; it does not teleport the cabin. |
| Load saved cabin chunk to resume | Load the chunk containing the existing entity. The controller does not spawn a duplicate cabin. |
| Cannot choose cylindrical | Disable rear service at every floor and rear entrance first. |
| Material is not installed | Use a real block registry ID. Pale Oak is not vanilla in Minecraft 1.21.1; install a compatible backport or select another wood. |
| GUI asks for more room | Reduce Minecraft GUI scale until at least 280 pixels of content height are available. |
| Return / edit does not deactivate | Enable the home floor, leave the cabin and clear the doorway. |

The new cabin is a model entity: arbitrary placed blocks and block entities are not carried as interior decorations. Its built-in finishes provide decoration. Full-bright LED trim does not emit dynamic world light. Graphical shader appearance, actual multiplayer/network latency, full server restart and chunk lifecycle still require manual in-game certification; consult the implementation report for exact validation.
