# Create Transit Alpha16.6 — shared escalator and travelator family

For Minecraft 1.21.1, NeoForge 21.1.248+, and Create 6.0.10.

## Shared placement

The Station Utilities tab now contains four family items: glass, solid, redstone glass, and redstone solid. The same item builds both a flat travelator and an escalator. One item is consumed for each placed section.

- A solitary section and sections placed in a level row stay flat. Ordinary surrounding blocks do not create steps.
- Building matching sections one block forward and one block higher creates an incline. The immediately adjacent flat sections become its lower and upper landings.
- Removing the elevated section turns the affected sections back into flat travelators. Their frame dye, trim dye and block entities are retained.
- Place matching sections side by side to widen the run. Interior handrails disappear and exposed outside handrails remain.
- Where a flat approach continues into a staircase, landings use straight, continuous handrails with matching mitered slope joints. Removing the approach restores the rounded terminal rail.
- Face uphill when starting a run. Clicking an existing section inherits its facing. Crouching starts a reversed lane; the Create wrench reverses the connected run.

For a north-facing escalator with three incline sections, place at `(0,0,0)`, `(0,0,-1)`, `(0,1,-2)`, `(0,2,-3)` and `(0,3,-4)`. The first and last positions become landings. Additional sections behind the first landing at the same height or beyond the top landing at the top height remain travelators.

## Redstone and LED trim

| Family item | No redstone signal | Signal at any connected section |
| --- | --- | --- |
| Regular glass or solid | Runs | Stops |
| Redstone glass or solid | Stops; inside and outside LED trim off | Runs; inside and outside LED trim on |

Power applies to every connected matching section, including flat tiles, slopes, landings and widened lanes. The unpowered trim has a dark, normally shaded material with zero native fullbright and zero PBR emission. Its dye color is saved and returns when the run is powered. Direction/stop indicators retain their existing status behavior.

Regular and redstone-powered runs have separate control networks. Opposite-direction lanes retain their separating rails. A gap separates runs and stops power propagation.

Click with a dye to color the frame. Crouch and dye-click to color the trim. Moving motor sound follows the existing run/stop behavior.

## Updating an existing world

Replace the previous Create Transit JAR with `create-transit-0.5.0-alpha.16-escalators.6.jar`. Install only one Create Transit version.

All eight original fixed-section IDs remain registered, including both original travelator blocks. Existing builds and QuickBuild schematics still load. Those old items are hidden from the creative catalog; pick-block and drops now return the shared item. An old section item already in an inventory can be converted into the matching regular family item using a one-item crafting recipe.

The shared collision caches, selection caches, moving-mesh implementation and Flywheel renderer from the successful FPS fix are retained. Placement searches run on server topology updates, not in render frames.

## Verification scope

Validation covers native Minecraft placement in both orders and all facings, both skins and control modes, widened level belts, elevation creation/removal, preservation of dyes and clocks, combined flat/staircase networks, rider start/pause, power removal and network splits, legacy loot/pick behavior, the creative inventory, baked lightmaps, exact joint edges and shader texture emission values. A graphical Complementary session and hardware FPS measurement are not part of these checks.
