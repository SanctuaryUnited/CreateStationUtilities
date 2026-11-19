# Create: Transit — connected secondary dye

**Patch:** 0.1.0-alpha.4  
**Minecraft:** 1.21.1 · **NeoForge:** 21.1.248 · **Create:** 6.0.10 · **Java:** 21

## Install

If the direct JAR download fails, download `CREATE-Transit-Alpha4-Connected-Dye.zip` and extract it. Put **CREATE Transit Alpha 0.1.0.jar** into your Minecraft instance's `mods` folder, replacing the previous Create: Transit JAR. Keep only one version of Create: Transit installed. Install this update on both the server and clients for multiplayer.

This build includes the preceding Elevator System 2.0 update.

## Use the feature

1. Hold the dye you want for the secondary/accent color.
2. Hold crouch.
3. **Right-click the same Create: Transit block twice within about half a second.** Release the use button between clicks.

The first click retains the normal single-piece/multiblock dye behavior. The second applies the secondary color across the connected component. It follows face-to-face block connections in all six directions, across different Transit kits. Diagonal contact, air and blocks from Minecraft or other mods do not connect the fill.

Bulk coloring changes only the secondary/accent channel. Each affected block keeps its own primary/body color. All currently dyeable Transit families participate: platform doors and walls, fare equipment, gondola, LRT, elevators, escalators and travelators. Legacy Transit pieces without a dye channel can connect neighboring pieces but do not acquire a new color channel.

Use this on **placed blocks**. Disassemble moving trains, gondolas or elevator cabins before dyeing their connected blocks.

## Dye use and bounds

- In Survival, the first click keeps its existing dye cost. The bulk operation consumes **one additional dye** if it changes anything, regardless of how many connected parts change.
- Creative consumes no dye. Applying an already matching color consumes no dye.
- The bulk scan has a **4,096 connected block limit**. Larger components are rejected with a message; split them into smaller sections.
- Keep the chunks around the build loaded. An unloaded boundary cancels the bulk operation rather than forcing chunks to load.
- Scan/permission failures cause no partial bulk recolor and no bulk dye charge. The ordinary first click may already have recolored the clicked piece.
- The server validates crouching, dye, same target/hand/color, timing and build permission. Cancelled/denied interactions clear the pending gesture. The scan also respects Minecraft's interaction/item-use restrictions at each position.
- Third-party protection plugins that only cancel an interaction at the clicked position may require their own integration for protected regions elsewhere in a connected build.
- Holding the use button is filtered on the client; it should not count as repeated dye clicks. This supports ordinary mouse/keyboard use bindings. Custom controller/input mods need an in-game check.

## Implementation

`ConnectedAccentDye` performs a bounded server-side breadth-first scan before applying changes. Existing block entity color setters save and synchronize the results. A per-player, short-lived gesture record prevents different players, blocks, hands or colors from forming one pair. It uses weak player keys and dimension identifiers, avoiding retained world references.

`ConnectedDyeInput` filters held-use repeats and resets on mouse/key release, client disconnect, menu opening or released use binding. The existing interaction listener runs at lowest priority and receives cancelled events so it can discard a pending gesture without overriding cancellation.

`StationBlockEntity` and `TransitBlockEntity` now implement the existing `CabinColors` interface using their unchanged color getters/setter. Their animations, geometry, networking and saved fields are preserved. Existing single-click handlers remain intact.

No new blocks, recipes, models, textures or creative tab changes are introduced. The mod ID remains `createtransit`. The filename retains the Alpha 0.1.0 convention, while embedded metadata identifies this patch as 0.1.0-alpha.4.

## Validation

**All 144 required native Minecraft GameTests passed**, including eight new connected-dye tests. Gradle compilation/package checks and the native CLIENT data-generation validation passed. The JAR preservation audit confirms that elevator, escalator, vehicle animation, model and inventory files are unchanged; only the interaction hook, two color-interface declarations, added dye handlers, language strings and version metadata differ.

The checks cover mixed-family propagation, preserved primary colors, gaps and diagonal separation, non-dyeable Transit connectors, Survival/Creative/no-change costs, real server double-click interactions, expired/different-player/different-color/different-target gestures, cancelled events, missing build permission, multipart elevator doors, persistent colors, oversized components and unloaded chunk boundaries.

The native client data-generation launch validates production client registration and existing inventory/tint integration. It does not open a graphical Minecraft window. Physical mouse/keyboard double-click feel, custom input mods, multiplayer latency and visual appearance still need an in-game check. Optional Steam ’n’ Rails runtime tests are excluded by the project's existing `withoutRailways` test setting; its production integration remains unchanged.

The build environment used the same private headless-assets setup and NeoForm Java-executable fallback needed for the previous release. Those environment workarounds are not bundled into the mod or source project.

JAR SHA-256: `8a5e5e3cea73613de3887149a85c0f5bd04cfa131c5e81ad8988a858264f8e68`
