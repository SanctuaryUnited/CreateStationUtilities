# Create Transit — visible inner LED trim, Alpha 15.3

Release: `0.5.0-alpha.15-escalators.3` for Minecraft 1.21.1 / NeoForge. Replace the previous Create Transit JAR and keep only one version installed.

Both sides of the passage now have a visible LED strip above the moving tread edges, mounted in a slim metal channel. It follows lower landing → incline or flat section → upper landing. The change applies to all eight glass and solid escalator/travelator pieces. The outer LED trim remains, and both strips use the same dyeable accent channel: crouch and use a dye.

The previous inner strip sat below the tread surface and was mostly obscured. This revision raises it to 6.45–7.55 model pixels above the diagonal casing baseline, above the moving steps at every animation phase. The housing fits inside the existing side-rail collision. Matching adjacent lanes remove the inner divider and its LEDs; the strips stay on the two outside rails of the widened passage.

The included QuickBuilds use the same block IDs and layouts as Alpha 15.2. Copy them into your Minecraft instance's `schematics` folder and load them with Create's schematic tools. Existing placed pieces receive the revised model automatically.

This is an asset-only patch, packaged with the exact 162 compiled classes from the Alpha 15.2 JAR. Those classes passed 93 native GameTests in Alpha 15.2; the native tests were not rerun for this visual update. This patch checks the new LED placement, face normals, dye channels, model references and bytecode identity, and renders close-ups of the actual new assets. No graphical client or Complementary Reimagined shader playtest was performed.
