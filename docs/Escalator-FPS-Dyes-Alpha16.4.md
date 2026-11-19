# Create Transit — Escalator FPS workload and dye refresh (Alpha16.4)

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21.

## Installation

Close Minecraft and replace the previous Create Transit JAR with `create-transit-0.5.0-alpha.16-performance.4.jar`. Keep only one Create Transit JAR installed. Update the server copy too when using a multiplayer server. Existing escalators, travelators and their saved data retain their IDs; rebuilding them is unnecessary.

## Dye controls

| Target | Control |
| --- | --- |
| Frame / metal casing | Right-click the section with dye |
| Both inner and outer LED trims | Crouch + right-click the section with dye |

Colors apply to the clicked section. Frames and LEDs have independent color channels. Survival consumes one dye only when the chosen color changes; creative mode retains the dye. Pick block and world reload retain both colors.

## Changes

The previous incline path repeatedly merged stepped collision volumes and then merged them again to rotate east/west/south-facing sections. Flat travelators already used a fixed shared collision shape. Inclines now fill one native voxel grid directly, with the same occupied volume and moving tread heights, and rotate input boxes before filling it. Each immutable phase shape also caches its ray-cast boxes and selection-outline edges so the grid is not enumerated repeatedly each frame. Sections with the same phase and rail profile share the result, without rounding animation time.

The existing dye interaction and crouch dispatch were already registered. The missing refresh was in escalator color packet handling: stored paint/accent values changed, but baked casing and LED meshes were not marked for a redraw. The receiver now requests fresh model data and a native client block render update after applying the packet.

The Alpha16.3 moving mesh, shader-compatible fallback, Flywheel registration, glass, both LED trims, sound, dyes, rider movement and sectioned inventory are retained.

## Verification scope

Native shape and ray-cast tests verify collision and selection equivalence. Native gameplay tests exercise real dye interactions as well as rider travel, reversal, widening, redstone and sound. A client-distribution test exercises native frame/LED tint registration against received data.

CPU/allocation benchmarks compare the exact previously delivered Alpha16.3 collision path with this version. They include moving collision construction and repeated selection ray/edge work. They do not measure GPU rendering, shaders, actual draw calls or in-game FPS. A graphical Minecraft/Complementary session is unavailable in this environment. Check actual frame rate in the same scene in your game after replacing the JAR.

## Measured CPU workload

Median of three alternating warmed samples, eight game ticks per sample. Each tick includes collision construction and four selection ray/outline queries per section (an 80 FPS / 20 TPS synthetic workload). Timing covers CPU work only. Twelve-section cases deliberately issue selection queries for every section; a real camera only outlines its targeted section.

| Scenario | Alpha16.3 (ms) | Alpha16.4 (ms) | CPU reduction | Allocation reduction |
| --- | ---: | ---: | ---: | ---: |
| 1 sections north shared | 38.182 | 8.824 | 76.9% | 86.0% |
| 1 sections east shared | 227.825 | 8.417 | 96.3% | 99.8% |
| 12 sections north shared | 380.573 | 16.399 | 95.7% | 90.0% |
| 12 sections north independent | 349.074 | 75.560 | 78.4% | 81.1% |
| 12 sections east shared | 1051.262 | 12.946 | 98.8% | 99.8% |
| 12 sections east independent | 1041.737 | 76.518 | 92.7% | 99.7% |

All 108 required native gameplay tests passed in the final fresh-world run, including 4 new dye/phase-sharing tests. Native client tint checks covered 2,048 received paint/accent combinations. A previous run hit the retained rail-circuit station-binding failure; the final fresh-world run passed that test without changing any rail code or fixture. See `Alpha16.4-Validation.txt`, `Alpha16.4-Native-Validation.log` and `Alpha16.4-Incline-Benchmark.json`.
