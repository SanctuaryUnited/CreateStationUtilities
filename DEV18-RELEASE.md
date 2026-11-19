# Create: Station Utilities 0.2.0-dev18

## Install on a test copy first

1. Back up the world and keep the previous mod JAR. For an existing server, stop it before replacing the JAR.
2. Use Minecraft 1.21.1, Java 21, NeoForge 21.1.248, and Create 6.0.10. Put the V18 JAR in `mods` on the server and on every client, replacing the previous Station Utilities JAR. Matching clients are required for the custom models, entity data, and packets.
3. Open a copy of the world and confirm a car can travel from its lowest to its highest enabled floor. Check both entrances on a through cabin. If rolling back after saving with V18, restore the backup alongside the old JAR.

## Controls

- **Elevator Driver → Setup → Doors** has the finish, glazing, LED dye, and independent cabin and landing arrow animation settings.
- **Elevator Driver → Setup → Motion** has the per-shaft ambient toggle and frequency, left and right queue displays, and touchscreen volume. Set volume to zero to mute. Queue displays and ambient traffic default off.
- **Hall call panel + Create wrench** opens bank setup. Select **Primary bank** at the bank whose letters you want to keep. Open the secondary bank and select **Preview secondary**, then **Confirm merge**. Finish active bank dispatches first. **Undo last merge** restores the most recently absorbed bank and its original letters.
- To correct a selector in an ambiguous lobby, select the desired bank at its hall panel, then sneak-use a Create wrench on the selector. A registered entrance for that bank must be nearby on the same floor and face the selector's side.
- Bank setup also controls destination floor names, the hall call face, bank-color or directional illumination, and hall call volume. Bank-color calls use the bank's accent dye.

## Compatibility and migration

The mod ID and registered block and entity IDs are unchanged. This build uses V17.7 as its source baseline. V17 cab geometry and V13 compatibility geometry keep their respective dimensions and collision profiles. Existing door finishes load through the prior style values. Motion schema 2 retains direction, in-flight ambient status, and the priority destination for a boarded passenger. Schema 1 motion is readable and derives direction from the saved target where possible. Existing bank data remains readable. A merger stores the original secondary bank and keeps its shaft IDs and settings.

Cylinder shells retain the existing voxel contour and front-only entrance rules. The added LEDs follow that contour, so transitions still reflect the voxel wall profile. Custom floor materials use the first upward-facing baked sprite where available, with a particle-sprite fallback. Multi-texture connected models, resource-pack-specific orientation and tint, and legacy V13 width scaling can differ from placed blocks.

## Verification scope

- Java 21 source compilation against NeoForge 21.1.248, Minecraft 1.21.1, and Create 6.0.10. Gradle could not resolve its ModDevGradle plugin in this environment. The development JAR uses newly compiled classes and the original release's unchanged resources plus the new door texture and atlas entry.
- Pure-Java tests covered direction sweeps, intermediate insertion, duplicate stops, reversal, legacy motion migration, door glazing and collision envelopes, lighting options, and cabin geometry.
- The 11-test native NeoForge elevator suite passed, including merge/unmerge and ambient passenger priority.
- The headless renderer check passed for authored glass UVs, full-bright LED emission, nine floor tiles on a 3×3 quad, and in-range UVs on clipped tiles.

Live two-client synchronization, Complementary Reimagined, visual inspection of every material and dye in Minecraft, and loading a full pre-existing player world still require manual verification. No production world was modified. Automatic selector association uses nearby entrances and explicit overrides. It does not model room boundaries or line of sight. The ambient launch throttle staggers new trips within a dimension.
