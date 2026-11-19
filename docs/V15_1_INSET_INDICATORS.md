# V15.1 — raised inner-frame indicators

Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10, Java 21. Version `0.2.0-dev15.1`, unchanged mod ID `createtransit`.

The arrow modules now use the original black housings on the inner jamb faces. The extra V15 modules on the hallway-facing trim have been removed. Each plate is mounted 1/32 block (half a model pixel) beyond its housing face, toward the opening. Arrow artwork and plaque lettering sit a further finite distance forward to prevent being hidden by the backing.

Both cabin and landing doors have stacked direction arrows. Fixed floor-ID plaques are drawn only by landing doors; the cabin renderer supplies no landing ID and cannot request a plaque. Plaques sit directly beneath the arrows and use the same raised mounting. Header displays stay above both entrances. Their artwork is now fitted to the original black display bounds rather than leaving a surplus black band exposed below it.

All six styles, independent selectors, mirrored header options, lime/red/gray direction signals, per-arrow call-button hover and V15 geometry/material caches remain included. No floor links, shaft layout, collision, door movement or saved cabin settings changed. Existing V15 elevators need no rebuilding.

## Validation

- Gradle build/check passed.
- Native CLIENT mounting assertions cover both jambs, all four cabin sizes and all four frame depths: 32 mounting cases. Plates are beyond the inner housing surface, face the doorway and remain centered in frame depth.
- Explicit plaque-presence assertions reject the cabin's null landing ID and accept G/F2 landing IDs.
- Existing 36 style/NBT combinations, exact split landing geometry, terminal hover selection, 9,216 production mesh configurations, 143,104 multipart states, GUI scales, radio and creative inventory checks passed.
- V15's 187 server GameTests remain prior regression evidence; this client artwork/placement patch does not change movement or server operation. They were not rerun for V15.1.
- The renderer matrix retains 25,184,256 submitted vertices; the curved-body sample remains 7,456 versus the old 12,544. No new hardware FPS claim.

The full-resource native client completed six style views, both call-arrow hover views, and two additional oblique inset close-ups. Inspection confirmed raised inner-jamb arrows and a readable landing F1 plaque, with no plaque below the nearer cabin arrow module. The cabin close-up also sees the separate landing frame farther away; its F1 plaque belongs to that landing. Front views confirm the obsolete front-trim modules and surplus header band are absent. See `V15_1_VISUAL_VALIDATION.log`. Complementary Reimagined and the complete user modpack remain outside the graphical test environment.

Production changes: `DoorIndicatorRenderer`, the landing-only plaque in `StationModelPreview`, and new engine-neutral `DoorIndicatorLayout`; build version. Development changes: placement assertions and two close-up camera views in the full-resource graphical fixture.
