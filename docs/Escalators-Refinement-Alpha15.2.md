# Create Transit — Escalator refinement, Alpha 15.2

Release: `0.5.0-alpha.15-escalators.2`. Minecraft 1.21.1 / NeoForge; requires Create. The validation setup uses NeoForge 21.1.248 and Create 6.0.10.

The eight existing escalator/travelator blocks have been remodeled against the approved Create Transit concept sheet. Their IDs remain the same, so existing installations and the three QuickBuilds receive the new appearance when the JAR is replaced.

The revision adds an enclosed diagonal underside, deeper white metal side casing, rounded black handrail returns at both landings, and LED strips along the outside of the casing. The glass variant has a metal base and thin blue glass; the solid variant has a continuous white panel. Entry posts show a green upward arrow or a red no-entry sign according to belt direction and whether it is stopped.

The tread belt now uses four larger steps per block, ribbed gray surfaces, yellow marks along the outside edges, and stationary landing comb plates. Animated grip seams run along the top of the handrails. Collision follows the larger moving steps and the wider side housings.

The kit keeps its established modular slope: each incline advances one block and rises one block. This is steeper than the perspective concept image. The casing extends five model pixels below its block, so allow clearance underneath the run. Flat sections create a travelator; matching adjacent sections widen the belt and remove internal rails.

## Installation and QuickBuilds

Replace the previous Create Transit JAR; keep only one version installed. Copy the included `.nbt` files to your Minecraft instance's `schematics` folder and load them with Create's schematic tools.

- `Escalator-QuickBuild-2-Wide-Glass.nbt`: two lanes wide, four-block rise.
- `Escalator-QuickBuild-2-Wide-Solid.nbt`: the same layout with solid sides.
- `Travelator-QuickBuild-3-Wide.nbt`: three lanes wide, ten sections long.

Place a lower landing facing uphill. Put the first incline one block forward at the same block height; each following incline goes one forward and one up. Put the upper landing one forward and one up after the final incline. A four-incline run has landings four blocks apart vertically and five horizontally. Leave at least two blocks of clear headroom above the belt.

## Controls

- Create wrench: reverse the connected belt.
- Crouch while placing: start a reverse lane; ordinary connected placement inherits the existing direction.
- Redstone on any section: stop the connected belt; removing power resumes it.
- Dye: body color. Crouch + dye: LED accent.

The internal drive runs automatically at 0.7 blocks per second horizontally. The 512-section connection limit remains. These are stationary installations, not moving contraptions. No inventory redesign is included in this patch; that remains the next update. EMU and HSR kits remain removed.

## Preview and verification

The preview PNG and animation render the actual model JSON, moving geometry and QuickBuild structures. Minecraft lighting and shaders can change the result. The native model check compares 80 moving configurations with the preview and checks face normals, UV coordinates, materials, four orientations, and tread coverage. The existing native GameTests cover carrying, ascent, descent, collision, widening, reversal, redstone and dye persistence. See the release validation file for the results of this build.

No graphical Minecraft client, multiplayer player-motion session or Complementary Reimagined shader playtest was available here.
