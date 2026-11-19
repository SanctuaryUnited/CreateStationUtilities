# Create Transit — Escalator renderer patch (Alpha16.3)

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21.

Replace the old Create Transit JAR with `create-transit-0.5.0-alpha.16-performance.3.jar`, leaving one Create Transit JAR in the mods folder. The Mods screen should show `0.5.0-alpha.16-performance.3`. Existing escalators and travelators retain their IDs, NBT, dyes and placement; rebuilding them is unnecessary.

## What changed

The previous patch still rebuilt animation lists, boxed parts, vertex arrays and packed meshes on every new partial-tick phase. Separate belts with different clocks could repeatedly replace a shared cache entry. Most thin moving strips were rendered as six-sided boxes even though only their upper face was visible.

This patch writes animation surfaces directly into bounded reusable buffers. It retains tread tops, front risers, yellow tread edges, metal lips, handrail seams and outward-facing direction signs, while omitting hidden box sides/undersides and subpixel strip edges. In the regression set, moving vertices fall from 953,856 to 200,960 (78.93%). Static casing/glass models, both dyeable LED trims, textures, PBR maps, sound and the inventory layout are byte-identical to Alpha16.2.

When Flywheel visualization is active, six shared unit-quad models are batched by material. Each section updates reusable instance transforms and lighting rather than uploading a new copy of its mesh through its block-entity renderer. Unused surfaces are hidden; pools are bounded and deleted with their visuals. Extended frustum and light-section bounds include the upper handrail. The ordinary renderer is skipped only when the visualization backend is supported.

When Flywheel visualization is unavailable, including shader configurations that disable it, the native fallback emits the same reduced surfaces with existing atlas sprites and lighting. The patch does not require a shader setting change. Native exact collision surfaces, step speed, reverse/redstone controls, rider motion, dyes, widening and the shared motor loop are retained. Collision unions now defer intermediate shape optimization, avoiding repeated simplification of each sliced box.

## CPU/allocation comparison with Alpha16.2

Five alternating samples after warmup. The principal workload is a 12-section, two-wide escalator, 600 rendered frames or 20 collision ticks per sample. Both collision versions cache repeated requests within the same tick. Rendering consumes native emitted attributes in a VertexConsumer sink. Allocation values are decimal MB.

| Work, median | Alpha16.2 | Alpha16.3 | Reduction |
| --- | ---: | ---: | ---: |
| Fallback CPU | 40.650 ms | 17.271 ms | 57.51% |
| Fallback allocations | 78.193 MB | 0.014 MB | 99.98% |
| Collision CPU | 106.809 ms | 59.107 ms | 44.66% |
| Collision allocations | 99.631 MB | 41.702 MB | 58.14% |

Separate scenarios cover one section, a synchronized 12-section belt, and independent clocks that previously thrashed the frame cache. The one-section fallback uses about 65–66% less CPU in this run. After initial buffer allocation, animation mesh preparation creates no per-frame lists, boxed parts or vertex arrays.

These measurements are CPU microbenchmarks, not FPS results. GPU rendering, shader passes, actual draw calls, atlas lookups and audio-device playback are excluded. The instanced GPU path is not timed here. A graphical Minecraft client with Complementary Reimagined has not been run in this build environment; actual FPS must be checked in the user's game. Raw results are in `Alpha16.3-Performance-Benchmark.json`.

## Verification

- 104 required Minecraft GameTests pass on a fresh generated test world, including ascending/descending riders, multiple phases, widened travelators, obstacles, redstone, reverse, dyes and sound lifecycle.
- 576 native collision unions match the original physical shape exactly, across all four facings, side profiles and boundary phases.
- 2,304 animation configurations retain the selected visible surfaces and native vertex position, UV, color, overlay, light and scaled/rotated normals. Shared unit-quad instance transforms and inverse-transpose normals agree with the native fallback.
- 80 reduced animation frames agree with the independent Python preview contract. Fifty escalator models bake in four orientations; six unit surfaces retain atlas-safe UVs and receive instance lighting without duplicate diffuse shading.
- Client distribution loading checks inventory mixins and invokes the production visualizer registration body, verifying Flywheel API linkage and six dedicated partial-model locations. NeoForge datagen skips the normal gameplay setup event; this check does not claim a rendered gameplay client.
- Native retained LRT/elevator model checks, sound checks and release archive audits are recorded in `Alpha16.3-Validation.txt`.

## In-game comparison

Use the same viewing position, shader settings and render distance as before; let chunks finish loading. Compare F3 FPS and frame times while looking at the running escalator, after stopping it with redstone, and while riding it. Check the version shown in Mods before comparing. This isolates running-animation work from shader/static-casing cost and rider collision work if further profiling is needed.
