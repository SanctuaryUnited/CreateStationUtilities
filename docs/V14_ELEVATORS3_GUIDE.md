# Create: Station Utilities — Elevators 3.0 / V14

Minecraft 1.21.1 · NeoForge 21.1.248 · Create 6.0.10 · Java 21.

Install `create-station-utilities-0.2.0-dev14.jar` on the client and server, replacing the previous Create Transit/Station Utilities JAR. Keep one copy of this mod. The mod ID remains `createtransit`. Sound Physics Remastered is optional and belongs on clients for its acoustic effects.

## Build an elevator

1. Build a clear shaft. Supported cabin footprints are 2×2, 3×3, 4×4 and 5×5. The cabin walls occupy the perimeter. New compact cabins are 2.5 blocks high, with a small horizontal allowance for the thin guide rails.
2. Place the Elevator Driver beneath a straight vertical column of matching Elevator Guide Rails. Keep the rails in the same column with matching orientation. They mount against a wall; the compact driver uses the same one-pixel depth.
3. Use dedicated Floor Marker Guide Rails at the stopping heights. For a floor block at Y=64, its walking surface is Y=65: place the marker at Y=65. The cabin walking surface stops at the marker's bottom edge. Repeat at the other floors. Connect rails between the first and last marker; no extra extension above or below the marker range is needed.
4. Use the Cabin Refresher on the connected rail to create the default cabin. Configure the cabin size while it is inactive. The new mount positions the back of the cabin next to the rail. **Alignment → Auto Center** centers the nominal footprint on the Minecraft grid. Even-sized cabins account for their different grid center.
5. Place landing doors manually at the cabin entrance on each served floor. The Landing Kit snaps to a matching entrance and links automatically where unambiguous. The controller also retains its install/realign landing action. For a manual link, with empty hands use **Sneak + Break** on the connected marker to select it, then **Sneak + Place/Use** on the landing. The selection gesture keeps the marker intact.
6. Rescan the controller. Configure floor IDs and names independently of their heights. Enabled, reachable floors appear in the passenger list. Example: F1 / Floor 1, F2 / Floor 2, F3 / Floor 3. Requests use a deduplicated FIFO queue.
7. Place touchscreen call panels at a comfortable height. Link them to the intended marker using the same empty-hand selection/link gestures. Their physical placement height does not choose the destination when explicitly linked. Existing multiple-marker button links remain supported.
8. Activate the cabin. Doors close before travel, remain locked while moving, and open only at the aligned, permitted landing. Select **Go** inside the cabin to request a floor. Scroll to reach longer floor lists.

Keep rail paths and the controller loaded. A broken/unloaded path or missing required landing prevents normal service. Static shaft blocks are not removed by assembly; avoid placing solid world blocks through the passenger space.

## Choose a cabin and skin

**Customization → Cabin** provides four structural models:

- Standard Solid: enclosed walls; optional rear entrance.
- Rear Panoramic: rear window, optional side windows; a rear entrance replaces the rear window.
- Scenic: three glazed sides; optional rear entrance.
- Cylindrical: rounded panoramic rear, front entrance only. Voxel Curve remains optional.

The **Skin** selection is independent of the model and doors:

| Skin | Materials and accents |
| --- | --- |
| Original Brushed Steel | Silver brushed panels, metallic seams, cyan accents |
| Modern Dark | Charcoal panels, graphite door edges, upper-wall dyeable foam strips |
| Wood Trim | Selected block/plank texture on appropriate wall, door and ceiling surfaces, dark framing, amber default accents |
| Light Transit | White/silver panels, dyeable blue lower wall/door accents |

Body dye, secondary accent/foam dye and LED dye are separate controls. Each supports all 16 Minecraft dyes. Main lamps remain pure white. Modern Dark has no lower-wall foam and no rear information screen.

The Wood and Floor material pickers display inventory/hotbar blocks; Creative players can search installed block items. They do not consume or move inventory items. **Default floor** restores the new cabin checker tile. A custom floor uses the selected block's particle texture. Existing custom floor selections remain saved.

## Doors and ceilings

**Doors → Style** has exactly three choices: Full Glass (`GLASS`), Full Solid (`SOLID`), and Slim Window (`SLIM_WINDOW`). Half Glass and the old style lineup are not selectable. All three work with all four skins. The master configuration applies to linked cabin and landing doors. Normal sliding remains the default; two-speed is optional. Center/left/right mechanisms and existing door timings remain available.

**Cabin → Ceiling customization** contains:

1. Standard recessed lights (`RECESSED`).
2. Perimeter strips (`STRIPS`).
3. Parallel linear LEDs (`LINEAR`).
4. Grid lighting (`GRID`).
5. Central illuminated panel (`PANEL`).

Ceiling materials follow the skin. Cylindrical layouts are constrained to the curved shell. The primary ceiling-light toggle is independent of LED accents. Fully enclosed cabins retain default level-15 illumination while primary lighting is on. Turning it off lets temporary cabin light blocks expire.

## Alignment and indicators

Deactivate an empty cabin before changing its footprint, entrance geometry or alignment. Alignment contains Auto Center, reset to saved center, 90° rotation, X/Z half-block controls, and X/Z 1/16-block controls. Adjustments are saved per elevator. Preview before activating and realign/reposition landings if the entrance moved.

Old offsets are preserved automatically. Auto Center is an explicit action; it can change where an old cabin's landings need to be placed.

The merged header shows a centered floor ID and direction arrows. Mirror swaps the header arrow positions. Inside jambs have an up arrow above a down arrow on both sides, without duplicate floor numbers. Up is lime; down is red. During travel the matching side arrow lights. On arrival, up flashes once or down twice; terminal arrivals use the available departure direction. After the arrival signal, both side arrows return to gray while stationary.

The touchscreen panel is physically compact at a terminal: Up-only at the lowest enabled floor, Up/Down at intermediate floors, Down-only at the highest. Disabled and fault states are shown. Click the upper or lower half of an intermediate panel for that direction's request indication. Requests currently summon the same car to the floor; there is no multi-car directional dispatch algorithm.

## Audio and client preferences

Cabin customization retains the chime presets, Motor Hum toggle, Door Hiss toggle and motor-volume control. The motor now uses a stronger, seamless loop while moving. Relevant chime/hiss ranges are roughly 18 blocks at the source setting, and the motor uses a 20-block attenuation distance. Walls and Sound Physics settings can change perceived volume.

Music retains its per-emitter/group 0–300% setting. Volume controls have −/+ buttons in percentage units. Direct music range and reverb range use block units and −/+ buttons; their existing 4–256 range remains available.

These local commands require no operator permissions:

- `/createstationutilities clientsettings`
- `/csu clientsettings`
- `/csu settings`
- `/csu radiosync`

The settings menu offers elevator audio volume, radio volume, Streamer Mode, automatic radio sync, Sync Now, menu size from 70% to 150%, all 16 dye themes, and elevator PBR quality Off/Low/Medium/High. Local audio percentages multiply the world's emitter settings. Blue is the default theme. Preferences persist in `config/create-station-utilities-client.json`.

**Streamer Mode mutes radio playback only.** Elevator chimes, motors, door sounds and other non-radio effects are unaffected.

A live station's local decoder now keeps advancing while temporarily out of range. Readers share one clock per URL; automatic sync corrects substantial reader lag. Sync Now restarts nearby radio channels locally without changing the server or another player's audio. Unused connections expire after 30 seconds; returning later reconnects to the live source. Separate clients may retain different network/buffering latency. A finite MP3 download is not a live broadcast, and unsupported AAC/HLS/playlist URLs remain unsupported.

## Migration and limits

Schema-1 cabin settings migrate to schema 2. Removed styles map to the closest current door model and skin. Half-glass Copycat settings map to Slim Window. Old materials, floors, links, motion state and alignment data remain saved. Existing legacy wood-body cabins become the Solid model with Wood Trim skin. Older RING/BULBS ceilings remain readable; the menu offers the five current layouts.

The custom material system samples block textures. It does not reproduce arbitrary block geometry, connected-texture logic, or another mod's Copycat block-entity material data. Shader reflections/transparency need a check with your actual Complementary Reimagined setup. PBR quality changes the elevator material maps, not shader-pack-wide settings.
