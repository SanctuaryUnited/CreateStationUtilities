# Create Transit — Inventory rework (Alpha16.1)

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21.

## Creative tabs

| Tab | Named sections | Base item count |
| --- | --- | ---: |
| Vehicle Kits | Gondola, LRT | 69 |
| Station Utilities | Fare Equipment, Platform Screen Doors, Elevators, Escalators & Travelators | 42 |

Gondola has 23 parts; LRT has 46. Station Utilities contains 5 fare items, 6 platform items, 23 elevator parts and 8 escalator/travelator parts. A compatible optional Steam 'n' Rails installation adds the cable track to the Gondola section.

Each section starts with a full-width named cyan separation bar. Items follow in the existing order within each kit. Headings scroll with the normal nine-column, five-row creative grid. Blank header and padding cells do not highlight or accept clicks, so clicking a divider while carrying an item leaves the carried stack intact. The player's hotbar and other creative tabs keep normal slot behavior.

The two registered tabs contain real items only; UI rows are inserted locally when either tab is selected. Creative search therefore contains no separator items or blanks. Parts added by another mod to either tab are retained in an Additional Parts section. Every dyeable part remains one default-white entry; there are no separate color items. Ticket and pass remain the existing RP items.

The six earlier Create Transit creative tabs are replaced by these two. Existing placed blocks, recipes, saved structures and inventories use their existing block/item IDs. The previously deleted EMU and HSR kits remain absent. All model geometry, blockstates, textures and audio from the Alpha15.4 sound patch are retained.

## Installation

Replace the earlier Create Transit JAR in your `mods` folder with `create-transit-0.5.0-alpha.16-inventory.1.jar`. Keep one Create Transit JAR installed. Open the creative inventory and find **Create Transit — Vehicle Kits** and **Create Transit — Station Utilities**; use the normal tab-page arrows if other mods add more tabs.

## Preview and verification

`Create-Transit-Inventory-Preview.png` is an expanded software render of the native registered catalog and shipped item models. It shows all rows at once; Minecraft displays five scrolling rows. It is not a graphical Minecraft screenshot.

`runInventoryData -PinventoryValidation` loads NeoForge's client distribution and checks actual mixin transformation of the native creative screen, nested picker menu and slots before exporting the catalog. The GameTest suite checks the exact tab registrations, counts, white defaults, search contents, hidden legacy parts, complete heading rows, scrolling reachability and preservation of integration-added items.

See `Alpha16.1-Validation.txt` for completed checks and their scope.
