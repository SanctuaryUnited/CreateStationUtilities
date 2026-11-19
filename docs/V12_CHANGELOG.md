# Create: Station Utilities V12 — 0.2.0-dev12

Minecraft 1.21.1 · NeoForge 21.1.248 · Create 6.0.10.
Saved-world mod ID remains `createtransit`. Replace the previous mod JAR on both
client and server with `create-station-utilities-0.2.0-dev12.jar`.

- Four speaker models: original wall box, ceiling, slim column and outdoor horn.
  All support wall, floor and correctly inverted ceiling mounting.
- Dyeable speaker casing and grille, using the existing 16-color palette.
- Named radio groups share power, station, playback mode and master volume;
  individual speakers retain local gain and acoustic coverage.
- Separate direct range, extended reverb range and reverb level controls.
  Optional Sound Physics Remastered integration preserves its real environment
  reflections while applying independent direct/send attenuation per radio source.
  Without that client mod, playback uses ordinary direct attenuation.
- Cylindrical cabins offer a saved, optional voxel curve; smooth geometry remains
  available. Glass, floor, roof, trim and collisions follow the stepped outline.
- Cabin Alignment has a Rotate 90° button. It turns an inactive model cabin
  around its shaft center, independently of the driver/rail orientation.
  Rotation persists, previews correctly, and determines landing-door matching.
- Model-cabin assembly, geometry upgrades and vertical travel no longer reject
  static blocks in the cabin bounding volume. This accommodates wall-mounted
  guide rails in tight shafts. World blocks are not removed. Chunk bounds,
  other-cabin overlap, rail continuity, landing interlocks and doorway occupant
  checks remain enforced. Legacy block-built contraptions are unchanged.

Detailed setup: `ELEVATORS3_SIMPLE_WORKFLOW.md`.
Implementation, validation and remaining limitations: `ELEVATORS3_IMPLEMENTATION.md`.
