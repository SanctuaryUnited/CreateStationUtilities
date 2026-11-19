# Create: Station Utilities — V14.1 follow-up

Target: Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10, Java 21. Internal version `0.2.0-dev14.1`; mod ID `createtransit`. This patch retains the V14 cabin/door asset range and all previous station systems.

## Changes

- Cache the state-aware multipart bake once per Transit multipart source model and resource load. The previous full-resource client run exhausted its 3 GiB heap while repeatedly compiling identical selector trees. The new cache is limited to Transit blocks using the default rotation and default texture getter. Other namespaces, custom getters and non-default rotations keep the native bake path. New resource model instances have independent caches.
- Use a concurrent selector lookup for the shared Transit baked models so parallel chunk builders can safely populate state selections.
- Resolve moving-cabin radio coordinates from the loaded client cabin immediately when server metadata arrives. An older server position can no longer briefly overwrite the live emitter position before the next sound tick or acoustic-profile publication. Volume, station and other metadata continue to update normally.

## Validation

| Check | Result |
| --- | --- |
| Gradle `build` / checks | Passed |
| Dedicated-server GameTests | All **187** required tests passed |
| Native multipart cache | **143,104** block states across **97** production multipart models preserved the same selected child output as fresh bakes |
| Cache boundaries | Custom texture getters, non-default rotations, other namespaces and fresh reload-model instances bypass the old cached object |
| Parallel chunk builders | Concurrent state-selector population and output checks passed |
| Full graphical client boot | Full production blockstates/models reached the integrated world; **97** multipart builds served **143,007** repeated root-bake requests |
| Real client journey with Sound Physics | **60-block ascent and 60-block descent**; exact client feet-to-floor alignment, unchanged Inside radio source, zero measured emitter error, server door interlocks and linked terminal panels passed |
| Old radio snapshot regression | A real snapshot **3.707033 blocks** behind the loaded cabin was applied; emitter coordinates immediately stayed on the client cabin before another sound tick |
| Local radio controls | Streamer Mode stopped the native radio channel; disabling it and resyncing restored playback |
| Existing native geometry / UI checks | **9,216** configurations / **50,368,512** emitted vertices; all **1,088** controls across **64** viewport cases passed |
| Existing audio checks | PCM buffering, MP3/Ogg decoding, 12 shared readers, ICY/jitter, reconnect and bounded gain checks passed |

The initial strict server rider-in-volume assertion was diagnostic, not a shipping gameplay failure. The passing trip recorded maximum server/client movement-timeline separation of **3.595153 blocks** at speed while the local player remained exactly on the rendered cabin floor. No rider-position forcing was added.

Logs: `V14_1_REGRESSION_VALIDATION.log`, `V14_1_JOURNEY_VALIDATION.log`. The final normal-resource JAR is checked against every compiled production class and included source resource during packaging.

## Scope

Production files: `TransitMultipartBakeMixin`, `MultipartSelectorCacheAccess`, `MultipartBakeMetrics`, `RadioClient`, mixin metadata and build version. New development checks: `MultipartBakeCheck`, `CabinJourneyValidation`, native-client launch option and integration into the existing client validation provider. No cabin collision, movement, floor discovery, door interlock, escalator, travelator, kit registration or asset geometry rewrite was required.

An initial strict server-side rider assertion failed because vanilla client entity interpolation and server player reports represent different movement times. The diagnostic run showed the real client stayed exactly on its cabin floor through both journeys. The final check therefore records and bounds the server timeline difference while independently checking actual client footing, radio continuity and server door interlocks; it does not force or teleport the rider during the journey.

## Remaining limits

The graphical run uses software OpenGL and OpenAL No Output with the actual optional Sound Physics Remastered 1.21.1-1.5.2 mod. It validates native sound sources, position, continuity and controls; it is not a listening test. Complementary Reimagined appearance, multiplayer latency beyond the integrated-server session and the complete user modpack still require gameplay testing. The reference sheets remain the V14 procedural-geometry exports, not shader screenshots.
