# Create: Transit — connected primary and secondary dye

**Version:** 0.1.0-alpha.5  
Minecraft 1.21.1 · NeoForge 21.1.248 · Create 6.0.10 · Java 21

## Install

Extract `CREATE Transit Alpha 0.1.0.jar` from the patch ZIP and replace your previous Create: Transit JAR in `mods`. Keep only one Transit JAR installed. Update both clients and server for multiplayer. This build includes Elevator System 2.0 and the previous secondary-dye feature.

## Controls

| Interaction while holding dye | Result |
| --- | --- |
| Right-click once normally | Existing local primary/body dye behavior |
| Right-click the same block twice normally | Primary/body dye spreads to connected Transit blocks |
| Crouch-right-click once | Existing local secondary/accent dye behavior |
| Crouch-right-click the same block twice | Secondary/accent dye spreads to connected Transit blocks |

Use the same dye, hand, block and crouch mode for both clicks, within 10 game ticks (about half a second at normal server speed). Release the use button between clicks; holding it is filtered on the client. Changing crouch mode starts a new gesture.

**Primary bulk dye preserves each block's secondary color. Secondary bulk dye preserves each block's primary color.** All currently dyeable kits participate, including gondola, LRT, station equipment, platform screens, elevators, escalators and travelators.

The fill follows face-to-face connections in all six directions, across different Transit kits. It does not cross air, diagonal contact or blocks belonging to Minecraft/other mods. Transit blocks without dye channels can bridge connections but retain their existing appearance.

Use on placed blocks; disassemble moving vehicles/elevator cabins first.

## Cost and safeguards

The normal first click keeps its existing cost. A successful bulk operation uses one additional dye in Survival; no-change fills and Creative use consume none. Up to 4,096 connected blocks are supported. Oversized components, unloaded boundaries and failed permission checks cancel the bulk operation before it changes anything or consumes its dye. The ordinary first click may already have changed the clicked piece.

Build permission, Minecraft interaction/item-use restrictions and cancelled/denied origin interactions are respected. Third-party region protections that only intercept the clicked position may need separate integration for other positions in a connected component. No chunks are force-loaded.

## Implementation and validation

The existing `ConnectedAccentDye` implementation now records the selected channel with the gesture and uses that channel for matching/applying colors. `ConnectedDyeInput` filters held-use repeats for both primary and secondary dyeing. Two new server tests cover actual primary double-clicks across all kits, preserved per-block accents, Survival/Creative/no-change dye costs, mode switching and continued secondary behavior. Existing limits, protection handling and persistence are reused.

Compilation/JAR build and native CLIENT registration/data-generation checks passed. All ten connected-dye GameTests passed on both full-suite runs. The full suite passed 145 of 146 required tests on each run; the unchanged `full_size_circuit_assembles_and_roundtrips_two_native_stations` timed out with `No sequences finished`, including a fresh-world retry. That existing train-circuit failure remains unresolved; the full suite is not green.

JAR integrity and source/class agreement passed. Compared with alpha.4, only the two connected-dye implementation classes, the gesture record, English messages and version metadata changed. All other JAR entries, including train code and assets, are identical.

The native CLIENT data-generation run checks client registration and existing tint/inventory integrations. It does not render a Minecraft window. Physical input feel, custom input mods and live multiplayer latency still require an in-game check. Optional Steam ’n’ Rails runtime tests use the existing `withoutRailways` exclusion; its production integration is preserved.

The private headless-assets and NeoForm executable-discovery environment workarounds used for previous builds are not bundled into the project or mod.

JAR SHA-256: `89fad5a16e686dd8f08b414287096cc52afcdb28374a8050a82b20083d521674`
