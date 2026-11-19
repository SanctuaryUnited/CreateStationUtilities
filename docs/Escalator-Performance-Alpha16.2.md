# Create Transit — Escalator performance (Alpha16.2)

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21.

## What changed

Repeated collision requests used to rebuild the entire shape from many small sliced boxes, including all static side rails. Inclines now reuse their shape until the actual tick phase or block state changes. Static casings and complete flat/landing shapes are shared across sections. Incline geometry still follows the moving steps at the original phase precision; the original underside and collision union are preserved.

The renderer now packs the exact animation geometry once for synchronized sections with the same kind, exposed sides, status and phase. It caches six texture-atlas sprites and refreshes them after a model/texture reload. Native position/normal matrix destinations are reused, and the solid vertex buffer is obtained once per section. The fixed mesh cache retains no worlds or block entities.

Normal camera-frustum culling is enabled. The cached bounds cover the complete moving geometry, including incline handrail seams extending above one block. Client-side rider checks directly inspect the local player; each section no longer searches all nearby living entities. Server-side NPC physics is retained.

All static assets, glass rendering layers, dyeable inner/outer LED trim, moving geometry, motion speed, arrows/stop indicators, redstone control, sound and the Alpha16.1 inventory rework remain available. No visual level-of-detail reduction or coarse animation phase rounding is used.

## Measured CPU and allocation work

The native Java benchmark compares the former algorithms with this patch for a 12-section, two-wide escalator. It uses five alternating-order samples after warmup. The rendering case prepares/emits 600 frames into a consuming native VertexConsumer interface; the collision case performs four requests per section per tick for 20 ticks.

| Measured work (median) | Previous | Optimized | Reduction |
| --- | ---: | ---: | ---: |
| Render preparation and vertex emission | 115.34 ms | 95.36 ms | 17.3% |
| Rendering allocations | 136.94 MB | 78.19 MB | 42.9% |
| Collision calculations | 2,600.40 ms | 132.91 ms | 94.9% |
| Collision allocations | 1,781.04 MB | 99.63 MB | 94.4% |

These are CPU microbenchmark totals, not frame rates. Atlas lookups, GPU rendering, shader passes and real sound-device playback are excluded. The cache's first incline shape build remains included; subsequent identical requests are reused. Real FPS improvement depends on the build, graphics settings, shaders, hardware and other mods. Raw measurements are in `Alpha16.2-Performance-Benchmark.json`.

## Validation

The native comparison checks 576 exact collision unions against the old algorithm, including every kit kind, side profile and facing, with tread-boundary and near-boundary phases. It also compares 2,304 animated configurations and 953,856 emitted vertices, including position, color, UV, overlay, light and normals under rotation and nonuniform scaling. Mesh sharing and fractional phase invalidation are checked separately.

Additional native GameTests check real block-entity collision cache sharing/invalidation, live tread height in all four facings and render bounds containing every moving vertex. The existing rider, widening, redstone, reverse, dye, sound and other-kit tests are retained. See `Alpha16.2-Validation.txt` for the completed release checks and their scope.

## Installation

Replace the previous Create Transit JAR in your `mods` folder with `create-transit-0.5.0-alpha.16-performance.2.jar`. Keep one Create Transit JAR installed. Existing escalators and travelators use the optimized code immediately; their block IDs and NBT are unchanged.

For an in-game comparison, stand in the same place facing the same escalator before and after the update, with the same shader and render-distance settings. Compare F3's FPS and the frame-time chart after chunks finish loading, then ride the escalator and test redstone stop/resume and reversal. A graphical Minecraft FPS test was not performed in the build environment.
