# Elevator doors and construction — 0.2.0-dev4

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21. Install the same Transit JAR on client and server, replacing the previous JAR.

## New construction workflow

1. Place the controller with the Cabin Kit and configure cabin size/orientation. The controller remains below the guide rail column, on the cabin's side.
2. Place a straight column of Guide Rails against a shaft wall. Clicking a horizontal wall face chooses the mounting direction; aligned neighbors connect vertically. Rail geometry and collision now sit at the block face instead of at its center. Existing rail IDs remain registered.
3. Put a dedicated Floor Marker Guide Rail one block above each floor block. A floor block at Y=64 has its walking surface at Y=65; put the marker at Y=65. The controller records that stop as floor row 64 and aligns the cabin walking surface to Y=65. Extend continuous rails through the cabin roof at the highest stop.
4. Place an Elevator Landing Door Kit manually at the entrance. It places a complete two-row multiblock, oriented in the direction the player faces toward the shaft. Start at the first/leftmost cell of the full-width entrance. Its reserved width follows the nearest configured cabin (default 3 if none is configured). The opening remains 1 block wide for 2x2/3x3 cabins and 2 blocks wide for 4x4/5x5 cabins. The remaining width provides the side/frame areas for sliding leaves.
5. Wrench-click the connected floor marker, then wrench-click the landing. Linking checks floor height, shaft ownership, size and entrance orientation. An unlinked entrance does not satisfy cabin activation. Each configured front/rear entrance must have a complete matching doorway. Cylindrical cabins still have no rear doors.
6. Link call buttons with the existing marker/wrench workflow. Button height is independent of marker height; explicit links retain the existing 256-block distance limit.
7. Activate the cabin and request floors from inside it or from linked landing buttons.

Break and replace old three-row model landings when upgrading their visuals; this update does not destructively convert old shaft layouts. Breaking the new multiblock removes its other members, including an old third-row member if present. Keep a world backup. The landing kit currently has a generated inventory icon reusing the original Cabin Kit texture. Its recipe uses five iron ingots and one glass block. Material sampling does not consume the sampled block.

## Styles and materials

Both cabin and landing doors share a production panel-geometry implementation with these 17 styles:
Solid, Glass, Wood Trim, Slim Window, Divided Glass, Industrial, Retro, Dark Modern, Art Deco, Clinical, Transit, Executive, Heritage, High Tech, Coastal, Minimal, Copycat.

Cabin: the fifth model-settings page selects Door style and Copycat glass layout. It also accepts installed block IDs for Primary and Secondary material. While Copycat is selected, right-click the cabin with a BlockItem to sample primary material; crouch-right-click samples secondary. This affects the cabin's matching front/rear door style.

Landing: crouch-wrench-click a fully closed doorway to cycle style. Crouch-empty-hand-click cycles solid, half-glass and full-glass panel layouts; those layout settings are used by Copycat. In Copycat, right-click with a block samples primary; crouch-right-click samples secondary. Dye still controls the primary tint and accent/LED channel independently. Material settings propagate to all members of the same entrance and persist in block entity data.

Copycat copies the installed block model's particle texture onto the fixed door geometry. It is texture sampling, not the complete external Create Copycats API: block geometry, orientation-dependent faces, connected-texture behavior, block entities, transparency and special shader materials are not reproduced. Copied material channels are not multiplied by dyes. The six existing additional chimes plus the two original cues and Off remain available.

## Dimensions, animation and indicators

The landing frame, including header, is exactly 2 blocks tall and occupies a half-block depth envelope. Door leaves sit on the exposed shaft-side surface and move sideways only. There is no retract-into-wall or outward swing phase. Cabin panels use matching style geometry in their cabin entrance; cabin body height remains unchanged. Frame and panel collision animate with the mesh.

The landing header displays the linked landing's floor ID, with a direction arrow that follows the approaching cabin, flashes near arrival/opening, and retains direction when open. The display is on the hallway face and uses the secondary color at full brightness. A matching passenger-facing cabin header shows synchronized current floor/direction. These are baked text/mesh renderers, not dynamic world-light sources. Long floor labels can exceed the small display area; short IDs such as B1, G and F2 are recommended.

## Movement and idle doors

Speed can be changed while the cabin is stopped; changing it during travel/leveling is refused. The Movement page's Idle close button cycles 0, 100, 200, 300 and 400 ticks: 0 stays open, and the other choices close after 5, 10, 15 or 20 seconds. The server setting accepts 0–1200 ticks. Obstruction resets the timer and reopens the entrance. A same-floor request reopens a cabin that closed while idle. Travel still waits for closed doors and a complete destination entrance.

## Verification and limits

All 156 required native Minecraft GameTests passed in a fresh test world. Added native fixtures cover manual kit placement and explicit marker binding, refusing activation before linking, two-row placement, independent copied-material persistence, unknown-material rejection, timed-close/stay-open controls and same-floor reopening. Existing movement, passenger, original chime and station/escalator regression tests passed. Pure geometry tests cover 204 style/layout/size combinations, persistent settings, exact landing height/depth bounds and sideways-only panel movement. Native CLIENT registration/inventory checks passed with the new Landing Kit catalog entry.

A final build also checks cleanup of legacy third-row members; the full native run preceded that cleanup-only change. No graphical Minecraft window, real connected multiplayer, actual audio output, shader/Complementary rendering or GPU FPS test was available. The production-geometry preview is a software render without in-game text, reflections or lighting. The native dependency check excluded the optional Steam 'n' Rails runtime using the existing project option. No unrelated station, escalator, travelator or vehicle asset removals were made.
