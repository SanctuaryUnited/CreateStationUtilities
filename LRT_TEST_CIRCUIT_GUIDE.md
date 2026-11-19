# Sanctuary Transit — 512 × 512 LRT Test Circuit

The download contains a complete double-track proving circuit with one shared center-platform station and one pair of side platforms. Both running loops have broad return turns. Standard Create track and bogeys are retained.

## Choose one schematic

| File | Included |
| --- | --- |
| `LRT-Test-Circuit-512.nbt` | Circuit, stations, a staged three-car LRT, and its CRN Advanced Display Boards. Requires Create Railways Navigator. |
| `LRT-Test-Circuit-512-NoCRN.nbt` | Same layout and staged three-car LRT, with the optional CRN boards omitted. |
| `LRT-Test-Circuit-512-Track-and-Stations.nbt` | Circuit and station facilities; place your own vehicles. |

All three are 512 blocks east–west × 512 blocks north–south × 16 blocks high, including the site boundary markers. They contain roughly 50,000 placed blocks. The center is intentionally open for additional builds.

## Load it

1. Use Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10, and the included Create Transit `0.5.0-alpha.12-platforms.2` JAR. Replace the older Create Transit JAR rather than installing both. Use matching mod versions on a multiplayer server.
2. Copy one of the circuit `.nbt` files into your Minecraft instance's `schematics` folder. Load it through Create's Schematic Table using an Empty Schematic.
3. Choose a clear, flat 512 × 512 area. Align the schematic's bottom block layer with your existing ground block layer. Track sits one block above that layer; the platforms and carriage floors meet at the same walking level.
4. For the first test, use Create's creative placement option and preserve block entities and entities. Glue and prepared schedule contents depend on the placement method retaining them. The Schematicannon is an alternative if you supply the materials.
5. Allow the track graph and signals to initialize after placing the entire circuit. The schematic contains native reciprocal curve connections, station targets, and signal targets, not manually drawn track substitutes.

## Start the staged train

The staged train is your three-car LRT: two 16-block cab cars with an 8-block middle car, two-block articulation gaps, native Create bogeys, and three separate glue groups. Its floor origin is **(70, 2, 215)** relative to the schematic corner.

At station A, use the **outer-track** station controller at **(77, 3, 217)**. Enter assembly mode, assemble the staged train, and return the station to normal operating mode. Create expects the first bogey one track block beyond the station target, which is already accounted for in this layout.

The train includes a smouldering Blaze Burner conductor and forward/backward controls, using the same arrangement as the existing LRT journey tests. Give the prepared **outer-loop schedule** to that conductor after assembly. It alternates between:

- `Sanctuary Test A - Outer`
- `Sanctuary Test B - Outer`

Each stop has a 12-second dwell, and the schedule repeats. The schedule is stored in the barrel at **(77, 3, 282)**. If your placement method stripped its contents, create the schedule manually with the two exact names above, add a 12-second delay to each, and enable repeat.

The inner-loop schedule is stored at **(446, 3, 280)** and alternates between `Sanctuary Test A - Inner` and `Sanctuary Test B - Inner`. That loop has no preassembled train; you can add one of your other LRT QuickBuilds.

If glue entities were omitted during placement, glue each car separately within these inclusive bounds:

| Car | Minimum block | Maximum block |
| --- | --- | --- |
| North cab | (70, 2, 215) | (74, 7, 230) |
| Short middle | (70, 2, 233) | (74, 7, 240) |
| South cab | (70, 2, 243) | (74, 7, 258) |

Keep the station structure outside those glue selections. The gaps between cars remain articulated.

## Station dimensions and controls

| Feature | Exported value |
| --- | --- |
| Straight track center spacing | 12 blocks |
| Car body width | 5 blocks |
| Platform walking surface | 2 full blocks above the ground surface |
| Platform length | 116 blocks |
| Center platform width | 7 blocks |
| Side platform widths | 6 and 8 blocks |
| Native station points | 4: one for each running loop at each physical station |
| Native block signals | 16 |
| Occupancy detectors | 4 |
| Native curve connections | 8 curves, each saved at both ends |
| Approximate nominal turn radii | 72 blocks outer / 60 blocks inner |

Stop/controller coordinates are relative to the northwest schematic corner:

| Stop | Controller | Track target | Assembly direction |
| --- | --- | --- | --- |
| A — Outer | (77, 3, 217) | (72, 1, 217) | South |
| A — Inner | (79, 3, 294) | (84, 1, 294) | North |
| B — Inner | (423, 3, 217) | (428, 1, 217) | South |
| B — Outer | (446, 3, 294) | (440, 1, 294) | North |

Assembly direction points into the parked consist. The operating directions are northbound on the outer west straight and southbound on the inner west straight; the opposite side of each circuit naturally faces the other direction.

Each platform has two-block-high platform screen modules aligned with the three-car door bays. A native Train Observer detects occupancy of the stopping berth and supplies redstone to its connected screen bank. Nearby levers hold the bank open manually. The observer also reacts to trains passing through its target; it is an occupancy control rather than an interlock proving that a train has stopped.

The overhead poles and iron-bar wires are **decorative**. Trains do not require electricity to move. The footbridges sit above the wire layer, with separate stair access to the platforms and the ground. Vehicle doors retain Create's station-door behavior and the current mod's status lights/chimes.

When placing another copy of the circuit in the same world, rename its station stops and update its schedules so the stop names stay distinct.

## Alpha 12 loading patch

The included JAR removes duplicate and empty platform multipart rules: 7,344 rules become 892, a reduction of 87.9%. All 3,024 reachable platform geometry combinations were compared against Alpha 12.1 and are unchanged. Existing platform and vehicle block IDs, panel customization, dyeing, and chimes are preserved.

This addresses an unnecessary model-loading cost. It does not prove the exact cause of the earlier stall without your client log. If Minecraft still stalls, provide the `logs/latest.log` from that launch.

The preview is a technical plan generated from the exported schematic, rather than an in-game screenshot. See `CIRCUIT_VALIDATION.txt` for the checks completed on this release.
