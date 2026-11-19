# Create: Station Utilities — Elevators 3.0 / V15.1

Minecraft 1.21.1 · NeoForge 21.1.248 · Create 6.0.10 · Java 21.

Install `create-station-utilities-0.2.0-dev15.1.jar` on the client and server, replacing the previous Create Transit/Station Utilities JAR. Keep one copy of this mod. The mod ID remains `createtransit`. Sound Physics Remastered is optional and belongs on clients for its acoustic effects.

V14.1 reduces repeated multipart-model baking during loading and keeps moving-cabin radio positions on the live client cabin when server metadata arrives. Existing V14 cabins, floors and settings remain compatible; no rebuild is required for these two fixes.

V15 adds six independent header and side/call styles, fixed landing-floor plaques and per-arrow hover feedback. In Doors customization, select **Indicator style** and **Side / call style**; scroll to reach the controls. Landing plaques show their own floor, while headers follow the car. Terminal call arrows are now rendered upright. Existing elevators require no rebuild.

V15.1 places direction modules in the original inner-frame recesses, raised half a pixel beyond the housing. Both door types have arrows; only landing doors have the fixed floor-ID plaque below them. Header displays stay above the doors. Existing elevators require no rebuilding.

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


---

## Earlier release guides

# Create: Station Utilities V13 — cabin, controls and radio

Install `create-station-utilities-0.2.0-dev13.jar` in place of the previous mod
JAR on client and server. Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10.
The saved mod ID stays `createtransit`. Existing stations, groups, floors and
materials are retained. Sound Physics remains an optional client installation.

## Doors and indicators

Open the controller's **Doors** tab and select **Indicator style**:
Pixel Matrix, Slimline, Classic LED or Recessed Glass. This setting applies to
linked landing doors and the cabin, is saved independently of the door style,
and can be changed without rebuilding the shaft.

The old split indicator holes are replaced with a single black header spanning
the opening. Floor text stays centered, auto-sized to the available width.
Header arrows sit on its sides; the existing mirror setting swaps them.
Both inner frame sides contain stacked arrows without numbers: lime up, red
down, and gray/dim when inactive. Only the direction actually being traveled
lights; both return to gray when the cabin stops. Indicator Off hides text.

Regular two-leaf sliding is the default again. **Two speed** explicitly selects
four telescoping leaves. The compact shell clips hidden leaf overhang at its
pocket boundary, preventing panels being drawn through the shaft. The frame
stays in place; cabin panels are nearer the outer doorway edge. Rear panel
collision now uses the same plane as the visible leaf. Change structural door
settings with an empty, deactivated cabin.

## Cabin customization

The **Cabin** tab now groups lighting, material selection, voxel cylinder,
chimes, motor hum, door hiss and motor volume. Scroll to reach lower controls.
Ceiling fixtures start beyond the front door pocket; rear entrances also have
clearance reserved for their pocket. The voxel toggle is immediately below the
model selector instead of sharing a row with the rear-door control.

Select CYLINDRICAL, enable Curved glass and Voxel cylinder while deactivated,
then activate. Existing smooth cylinders stay smooth until selected.

The motor is a positioned continuous loop following the moving cabin, fading
in/out instead of periodic quiet overlapping pulses. Default motor level is
100%, versus the previous 25% source level. Motor volume supports 0–100% and
its loop has a 24-block attenuation distance. It uses the static sound pool so
it does not consume radio streaming channels. Door hiss remains separately
switchable. Chimes are labeled by their translated names in the Cabin selector.

## Material picker

Click the Primary/Secondary material box or its button in Doors, or the Wood /
Floor material button in Cabin. The new picker shows block icons from hotbar
and inventory. Creative players also get a searchable registry block list.
The current material icon/name is shown at the top. Search names or block IDs,
use wheel/Next/Prev to change pages, select a block, or Back/Esc to cancel.
Selection sends the normal validated controller setting; no item is moved from
inventory. These are block materials, not arbitrary uploaded image textures.

## Thin rail placement

Guide rails, dedicated floor-marker rails and compact drivers use a one-pixel
wall profile with matching visible geometry and collision. New placed drivers
and Refresher-generated drivers default to the thin variant; old explicitly
full-size legacy drivers retain their state for compatibility.

Build a straight rail column and markers, leaving one cell below the column
for the generated driver. Right-click a rail with the Refresher. New cabins
face away from the mounting wall, with their rear snug to the rail and a small
clearance. The new snug mount scales compact horizontal dimensions equally;
the rail/driver do not penetrate the shell. Rails still end at the first/last
marker without extension blocks.

Old saved mount positions are not silently rotated or moved. To opt in: stop,
exit and deactivate, use **Alignment → Snap cabin to rails**, then reposition
or explicitly realign landing doors at each floor before activating. Rotation
and half-block alignment remain available. Cylinders do not support rear doors.

## Volume and radio recovery

Music local and group/master controls now support 0–300%; the product of group
and local gain is capped at 300%. Every volume control has −/+ buttons, displayed
percentages and 1% steps. Music and reverb distance have −/+ one-block steps and
show blocks rather than percentages. Reverb level also has percentage buttons.

Above 100%, actual per-reader PCM amplification supplies the boost; Minecraft's
normal source-volume clamp no longer makes the larger values ineffective.
Samples are clamped to 16-bit bounds, so loud tracks can distort at high gain.
The shared decoder history is not modified by each listener's boost.

Inside radio eligibility now follows the locally interpolated cabin, tolerates
small vertical rider/cabin timing differences, and grants a brief exit grace.
It does not use a two-second-old source position to range-cull a passenger.
It still stops after leaving the cabin. Exhausted shared decoders are retired
so a subsequent open reconnects instead of replaying a dead cache entry. This
improves automatic recovery; real network outages can still interrupt audio.

## Validation limits

See the implementation report for build, server, GUI and audio test results.
The full client mod pack, shaders, moving-player Inside audio and subjective
motor loudness still require an in-game check. No audible listening claim is
made for automated tests using OpenAL No Output.

---

# V12.1 — speaker reverb recovery patch

Replace the previous Station Utilities JAR with
`create-station-utilities-0.2.0-dev12.1.jar` and restart Minecraft. Keep your
existing speaker blocks, radio groups, stations, colors and range settings.
No speaker rebuilding or cabin conversion is needed.

Radio acoustics now refresh when a stream actually starts and while it plays,
including when Sound Physics' global moving-sound updates are disabled. Leaving
coverage and returning should no longer leave the old muffled/low-reverb result
stuck on the speaker. Cabin radio uses the same fix. The patch handles ordinary
Sound Physics radio playback as well as the Extended reverb option. Real walls
still muffle sound normally; clarity recovers when you return to a clear path.

Sound Physics remains client-side for this integration. Use the tested
NeoForge 1.21.1-1.5.2 version. No global Sound Physics settings need changing.
The existing V12 setup instructions below still apply.

Validation: real native speaker playback, source stop/restart and three repeated
exit/re-entry cycles recovered both filter clarity and reverb. OpenAL used No
Output; audible listening and connected multiplayer still need testing in your
pack. See the implementation report and `V12_1_RADIO_REENTRY.md` for details.

---

# V12 — speakers, radio groups, voxel cabins and rotation

Install `create-station-utilities-0.2.0-dev12.jar` on both client and server,
replacing the older Station Utilities/Create Transit JAR. Keep your existing
Create and NeoForge dependencies. Saved cabins and speaker settings retain
backward defaults: smooth cylinder, no shared group, extended reverb off.

## Rotate and align a cabin

1. Finish the elevator's requests at a floor, then leave the cabin and doorway.
2. Open the controller and press **Deactivate**.
3. Select **Alignment → Rotate 90°**. Each press turns clockwise around the
   cabin center. The driver and guide rails stay in place.
4. Hold the Cabin Refresher and look at the driver or connected rails to see
   the cabin preview and highlighted entrance. Use the existing half-block
   North/South/East/West buttons to adjust its position if needed.
5. Place the landing doors to face the new entrance. The controller's
   **Realign landing at current floor** can reinstall its owned landing at that
   floor where the required cells are available. Repeat for each served floor.
6. Activate the cabin and test each stop.

Rotation is saved independently of alignment. Reset alignment resets the
horizontal nudge, preserving your selected rotation. Four turns restore the
original orientation. Rotation is disabled while a cabin is active.

Static block obstruction checks no longer prevent model-cabin assembly or
travel. A guide rail sharing the cabin's bounding cell is allowed. This does
not carve out shaft walls or remove blocks; solid blocks still have their
normal Minecraft collision with players. Matching closed landing doors,
continuous rails, loaded chunks and separation from other cabins are still
required for travel. Landing installation still refuses to overwrite unrelated
blocks. This change applies to the generated model cabins, including cylindrical
cabins, rather than the legacy block-contraption elevator.

## Choose the voxel glass cylinder

1. Stop, exit and deactivate the cabin.
2. Open **Cabin** and select **CYLINDRICAL** as the model.
3. Turn **Curved glass** on and **Voxel curve** on. Scroll if necessary.
4. Activate again. The curved rear becomes a stepped, axis-aligned glass shell;
   its floor, roof, LED trim and collision follow the same outline.

Turn Voxel curve off to restore the smooth model. Existing cylinders remain
smooth until you select the option. Cylinder models do not support rear doors.

## Place and color speakers

Station Utilities contains the original Station Speaker plus Ceiling Speaker,
Column Speaker and Horn Speaker. Click a wall, floor or underside of a ceiling
to mount one. Ceiling-mounted speakers attach to the underside and face down.
All four models use the same radio settings.

Right-click a speaker to configure it. Use the upper color-swatch row for the
casing and the lower row for the grille. Alternatively, use a dye on the block;
sneak-use dyes the secondary/grille color. A changed physical dye consumes one
item outside Creative mode. The existing double-click connected-block primary
dye and sneak-double-click secondary dye also include all speaker variants.
The horn is a visual model variant; its range is
radial, like the other speakers.

## Set direct sound and extended reverb

Ordinary playback needs no extra mod. For the independent extended reverb path,
install and enable **Sound Physics Remastered** on the client. This update was
checked with its **NeoForge 1.21.1 version 1.5.2**. Install it separately from
[the official project](https://modrinth.com/mod/sound-physics-remastered).
It is optional; do not install a Fabric or incompatible Minecraft build.

1. Select/add a direct MP3 or Ogg live-stream station and enable playback.
2. Set **Direct range**, for example **32 blocks**.
3. Enable **Extended reverb**.
4. Set **Reverb range**, for example **128 blocks**, and **Reverb level**, for
   example **30%**.
5. Test in the actual station/cabin environment. Direct sound fades out at its
   own radius; reflected audio fades over the longer range. Both sliders support
   4–256 blocks. Real reverb depends on Sound Physics' reflected paths and the
   surrounding materials; an open outdoor space may produce little reverb.

Without enabled Sound Physics, the saved reverb controls remain available but
playback uses the direct range only. These controls also appear in the cabin
Radio menu. Existing playback modes remain available. Extended reverb respects
INSIDE mode: it does not bypass the requirement to be inside that cabin.

## Create a radio group

1. Open the first speaker, configure its station, and scroll to the group field.
2. Enter a name such as `Sanctuary Central`; press **Join / create group**.
3. Enter exactly the same name on each additional speaker and join it.
4. The top Radio power, station and Playback controls now operate on the group.
   Use **Group volume** for master volume, with the ordinary Volume slider as
   each speaker's local level. Effective gain is local × master.
5. Direct range, reverb enable/range/level and colors remain per speaker.
6. **Leave group** restores that speaker's independent local settings.

A new group copies the first speaker's power/station/mode and starts at 100%
master volume. Group names are case-sensitive, 1–32 printable characters; up to
64 groups per world. Group data persists with the world and is shared across dimensions. POWERED and
UNPOWERED modes evaluate redstone at each speaker's own block. **Use local
station** copies a speaker's retained independent station into the group.

Speakers using the same URL share the existing client decoder and buffered
stream. The existing 12-source limit remains. This reduces repeated connections;
it does not guarantee sample-perfect timing between separate players' computers.

## Validation and testing limits

See the V12 implementation report for the exact automated and native-client
checks. Listening to the reverb, shader appearance, third-party mod-pack behavior
and connected multiplayer still require testing in your game.

---

# V11.1 — Update Cabin and color strips

Install `create-station-utilities-0.2.0-dev11.1.jar` on both client and server,
replacing the older Station Utilities/Create Transit JAR.

## Upgrade your existing model cabin

1. Let the elevator finish its queued trips and stop at a floor.
2. Step out of the cabin and doorway.
3. Open its controller and select **Setup → Update Cabin**. At GUI scale 4,
   scroll down in Setup to reach the button.
4. The same cabin updates to the 2.5-block height and half-block-deep door
   pockets. This includes the wide “3×3 entrance” option. The button then
   reads **Cabin up to date**.

Your controller, cabin identity/position, colors, style, chimes, lighting,
speed, floor configuration and landing/call links stay in place. You do not
need to remove the driver or relink the elevator. This changes cabin geometry;
it does not relocate an existing rail column or controller. Assembled block
contraptions from the much older elevator system are not converted by this button.

Updates are refused while occupied, moving, servicing a request, faulted,
unloaded, or when the proposed geometry is obstructed. A landing alignment
that would move a connection is refused without applying the upgrade.

## Choose colors visually

**Body dye** and **LED dye** now show colored strips with names rather than
numeric IDs. Click either strip, then scroll through Minecraft's 16 dye colors
and choose a color. Names also appear in tooltips; selected-strip text uses
contrasting ink. Existing saved colors are preserved.

---

# Create: Station Utilities — V11 compact elevator update

Install `create-station-utilities-0.2.0-dev11.1.jar` on both client and server.
Remove the previous Create Transit/Station Utilities JAR; the mod ID is still
`createtransit`, so keep existing worlds and configuration. Back up the world
before updating. Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10.

## New elevator: rail first, one-click cabin

1. Leave **3×3 clear blocks inside the shaft walls**. The walls are outside that
   space. Leave the desired landing entrances accessible.
2. Mount a straight guide-rail column on an inside shaft wall, along the middle
   of that wall. Keep all rails facing the same way.
3. Use dedicated Floor Marker Guide Rails at each stop. The marker occupies
   the block **one above the landing floor**. A marker at Y=65 means the cabin
   walking surface aligns with the top of the floor block at Y=64 (Y=65).
   No extensions are required above the highest marker or below the lowest
   marker. Leave the one block immediately below the rail column clear for
   the automatically placed driver. It is inside the shaft, under the rail.
4. Hold **Elevator Refresher** (existing Cabin Kit item ID) and right-click a
   rail. A marker click spawns at that floor; a normal rail chooses the nearest
   enabled marker. It creates a solid white 3×3-footprint cabin immediately, 2.5 blocks
   from its underside to roof top. A slim driver is created below the column.
   Rails share the shaft's wall-side strip; no hole in the perimeter wall is
   needed. The cabin's clear interior is smaller than 3×3.
5. Place **Landing Kits manually** at each doorway. Click the bottom doorway
   position while facing the entrance direction. Kits snap and auto-link to
   matching cabins/markers. Remove ordinary wall blocks from the doorway
   before placing the kit. Refresher spawning never adds landing doors.
6. Click the cabin to open floor selection; scroll through all enabled floors
   and press **Go**. Missing/incorrect landing doors prevent travel.
7. Use the Refresher on the rail/driver to open configuration. Return and
   deactivate an empty cabin before changing footprint or entrances. Change
   style, glass, dye colors, ceiling lights, chimes and indicators there.
8. Place call buttons at any height and use the existing marker-link workflow.
   Floor names, enable settings and manual door links work as before.

The new cabin entrances have half-block-deep pocket frames. Panels move only
sideways. Wide compact center doors use four telescoping leaves to fit their
side pockets. Front and rear entrances remain inside the cabin footprint.
Old saved cabins retain their old geometry/mounting; they are not moved or
shrunk on load. Use **Setup → Update Cabin** to upgrade an existing model cabin in place.
The update keeps its existing mounting position; relocating rails still requires
a separate rebuild.

## Motion, menus, lights and radio

- Speed is a **maximum** in blocks/tick (20 ticks/second). New compact cabins
  default to 0.6 blocks/tick, with acceleration 0.01. Short trips reach lower
  peak speeds; long trips accelerate toward the maximum and brake for the
  next requested floor. Starts ramp acceleration over about one second.
- Idle door timeout 0 means stay open; positive tick values close doors after
  that delay. Safety interlocks and obstruction reopening remain in place.
- Fine horizontal alignment remains **half-block** increments. Leave the
  cabin before deactivating and adjusting alignment.
- Focus a text field to type E normally. Esc still closes the menu. Floor
  selection, controller and speaker panels scroll smoothly; scrollbars can
  be dragged. GUI scales 1–4 remain supported.
- Cabin lights are white. Invisible, non-colliding **level-15** light sources
  are maintained inside cabins and expire after the cabin leaves/removes.
  They do not replace solid blocks or fluids. Cabin rendering also has a
  block-light floor of 15; shaders can still affect exposure and materials.
- Up to **12 nearby audible emitters** play per client. The nearest advertised
  emitters have priority. Up to 12 distinct URLs are retained; speakers on
  the same URL share one decoder/network connection with independent PCM
  readers. This avoids opening 12 identical station downloads.
- Eligible sources advertise 32 blocks before their audible radius. Inside
  radio can prewarm outside its cabin without playing there. Connections
  stay warm for 30 seconds after use; re-entry can reuse buffered PCM.
  First-time connection still needs the six-second anti-chop startup buffer.
- Radius still supports 4–256 blocks. Source chunks must stay loaded. Native
  sound falls off toward silence at the selected radius; no chunk loading
  is forced. Sound Physics reverb is controlled by that mod.

## Validation limits

See the implementation report and V11 validation logs for actual check results.
Complementary Reimagined/Sound Physics audible and shader appearance checks,
real connected multiplayer, and subjective scrolling/ride comfort still need
an in-game check with your pack. Headless native tests do not certify those.

---

## Previous release reference (V10.2 and older)

The material below describes the older two-click/controller-first workflow.
Use the V11 steps above for new rail-first compact cabins.

# Create: Station Utilities — V10.2 elevator guide

Minecraft 1.21.1 • NeoForge 21.1.248 • Create 6.0.10 • Java 21.
Install `create-transit-0.2.0-dev10.2.jar` on clients and server, replacing the older
JAR. The internal `createtransit` mod ID and existing registry names are unchanged.

## Build a new elevator

1. **Preview.** Use the Model Elevator Cabin Kit on a clear placement spot.
   The translucent cabin shows its real footprint; gold marks the entrance.
   Crouch-use rotates the preview. Use the same spot again to confirm.
2. **Confirm the blue mount.** V10 generates the controller and home Floor Marker
   Guide Rail in the blue outline, two blocks inward from the old corner mount.
   The cabin stays exactly where the ghost preview was. Both blue blocks must be
   replaceable. The default cabin is 3×3 with the new 3×3 entrance.
3. **Extend the shaft.** Run a straight, continuous guide-rail column upward
   from the generated home marker. Replace the rail at each stopping floor with
   a dedicated Floor Marker Guide Rail. A floor block at Y=64 uses a marker at
   Y=65, so the cabin's walking surface aligns with the top of that floor block.
   **Stop the rails at the highest marker. No roof-extension rails are needed.**
   Leave actual space for the cabin and roof throughout the shaft.
4. **Place landings manually.** Face the shaft and use a Landing Door Kit at
   each entrance. Clicking a bottom frame cell near the preview opening snaps
   the kit to that opening. Doors automatically link to a unique matching cabin.
   New entrances occupy three rows with a 2×2 opening. Existing two-high entrances
   remain supported. Standard depth is 0.5 block; Flush depth is 1 block.
5. **Configure.** Open the controller. Setup contains Size, Floors, Speed and
   the door timer. Floors discover as F1, F2, F3, etc.; they are not limited to
   three. In Floors, use the pencil to edit IDs/names and On/Off for service.
   This implementation supports up to 32 detected stops and a 256-block shaft.
6. **Activate and ride.** Activate after installing matching landings at the
   enabled floors. Enter and empty-hand use the cabin to open floor selection.
   Press **Go**. Doors close, travel is interlocked, and the cabin levels and opens
   at the requested marker. Multiple different requests use the existing FIFO queue.

## The new menus

All elevator screens use Create: Station Utilities branding with navy panels,
cyan accents, custom tab icons, dropdown choices and sliders. Scroll inside the
central panel to reveal more controls; footer buttons stay fixed. GUI scales 1–4
use the same responsive layout. Minecraft may reduce the effective scale at a
small physical resolution to preserve its minimum logical window size.

| Controller tab | Controls |
| --- | --- |
| Setup | Size, floor list, speed, stay-open/timed close, alignment and cabin shortcuts |
| Doors | Frame depth, Center/Left/Right slide, single/two-speed panels, 17 styles, glass/Copycat layout, mirrored indicators, materials, production mesh preview |
| Cabin | Size, five variants, dyes, windows, rear entrance, four ceiling lights, handrails, wood/floor materials, rear door appearance, production mesh preview |
| Motion | Speed, acceleration, opening/closing delays, idle close, chimes, motor/door sounds, controller name |
| Alignment | 0.5-block horizontal moves, baseline Reset, explicit landing realignment |
| Radio | Empty-by-default custom station list, HTTP(S) URLs, playback, volume and radius |
| Advanced | Maintenance return, retry, floor service and shared door settings |

Existing speed and acceleration defaults remain 0.2 blocks/tick and 0.01
blocks/tick². Speed must be above zero through 1; acceleration above zero through
0.1. Door delays use ticks (20 ticks = 1 second). The idle door button cycles
stay-open, 5, 10, 15 and 20 seconds. Doors can always reopen for a same-floor call.

To open the shared **landing door configuration**, hold the Landing Door Kit and
use an existing linked door. It opens the controller's Doors tab at that landing.
Normal empty-hand use still calls the elevator. Empty-hand sneak-use retains the
manual-link gesture below. Door settings are shared with the cabin, not independently
incompatible landing mechanisms.

## Door styles and indicators

Select Center, Left or Right slide and single-speed or two-speed telescoping
panels. All panel movement remains sideways. Telescoping leaves occupy separate
thin depth lanes. Allow adjacent side clearance for fully open single-speed doors.
Frame, mechanism and footprint changes require an empty, deactivated cabin.

The floor label is in one recessed transom screen, with a direction arrow at the
opposite end. Both sides are finished. **Indicators mirrored** swaps their sides;
**Floor display** hides the live text. Existing floor-label editing and arrival
flashing remain supported. The display stays above the moving panels when open.

New kits use the three-high frame. Existing saves retain their two-high layout.
A size-3-or-larger cabin can opt into **3×3 entrance** in Doors before replacing
its landings. A size-2 compact cabin keeps the legacy smaller opening. Larger
cabins retain wall infill around the central entrance. Changing the entrance
layout requires replacing its landings before activation; it does not cut blocks
or silently rebuild old shafts. Rear doors remain unavailable on cylindrical cabins.

## Half-block alignment

Exit the cabin, deactivate it, then open Alignment. Each West/East/North/South
click moves the cabin by **0.5 block**. Two clicks are one block. There is no
quarter-block movement cap. Reset restores the original mount baseline, including
V10's compact preset. Older saved sixteenth-block offsets retain their precise
position; new buttons use half blocks without rounding existing saves.

Hold the Cabin Kit and look at the placed controller to inspect the adjusted
preview. Reposition landing kits to match, or deliberately press **Realign landing
at current floor** while deactivated. This replaces that floor's owned landing
using current settings, after clearance/permission checks. Newly installed or
linked landing geometry and collision match the cabin's fractional alignment.
The controller and rail blocks do not move when you adjust the cabin. Vertical
stopping height is still controlled by floor markers.

The compact mount is generated only for newly confirmed Cabin Kits. Existing
controllers/shafts retain their saved positions and offsets.

## Call buttons and manual links

Place a call button near a complete linked landing at any convenient height.
Automatic detection finds the nearest physical landing (six horizontal blocks,
up to 256 vertical blocks); ties between shafts do not guess. Normal use of a
linked landing door also summons the cabin to that floor.

Manual overrides support unusual positions and elevator banks. Empty both hands:

| Normal Minecraft action | Result |
| --- | --- |
| Sneak + Break a floor marker | Select it without mining it |
| Sneak + Place/use a landing door | Link the entrance to that marker |
| Sneak + Place/use a call button | Add that marker link; repeated use is idempotent |
| Sneak + Break a call button | Clear manual links and restore automatic detection |

These honor remapped Minecraft actions. No custom linking key or mandatory wrench.
Holding dye, a block or a tool preserves its normal interaction. Existing optional
wrench shortcuts and saved manual links remain supported.

## Radio and speakers

1. Open Radio and enter a station name and a **direct HTTP(S) MP3 or Ogg Vorbis
   audio-stream URL**. Add station, select it, then turn the emitter on.
2. Set volume, radius and playback mode. Cabin modes are Nearby or Inside.
3. Place a Station Speaker against a wall or ceiling. Use it to open the same
   radio controls. It reads the world's shared station list automatically.
4. Speaker modes are Nearby (always), Powered, or Unpowered. An unconfigured,
   disabled or missing-station speaker stays silent. Radius is 4–256 blocks.

Compatible direct MP3/Vorbis mounts from AzuraCast, Icecast and SHOUTcast work
through the same decoder path. Use the actual stream URL, not the administration
page, browser player, playlist or `.m3u8` address. AAC/HLS, playlist parsing,
authenticated streams and Now Playing metadata are not implemented. The default
list is empty; no Sanctuary station or any other music is bundled. Up to 16
stations are saved per world, shared across dimensions.

V10.2 adds a **256-block maximum and default radio range**. Existing emitters
saved at the old 64-block maximum upgrade to 256 once. Smaller custom ranges
stay unchanged; after this update, explicitly choosing 64 saves it as 64. Set
Radius to 256 and choose **Nearby** playback to hear cabin radio from outside.
Changing the radius restarts that stream so Minecraft applies the new falloff;
allow the startup buffer to fill again. The sound fades toward silence at its
configured radius. The server retains a small handoff margin beyond that radius.

Sources must remain loaded on the server. This does not force-load shaft/speaker
chunks, increase entity tracking, or change Sound Physics' own reverb settings.
If a cabin's visual entity is not tracked, Nearby radio uses the server's source
position rather than stopping solely because the model is out of view. Inside
mode still requires the player to be inside the cabin.

V10.1 buffers six seconds of decoded audio before starting a live stream, with
an eight-second bounded reserve. Allow a few seconds of **Buffering…** when
starting a station. Available audio chunks are no longer padded to one second
with silence. ICY song-title metadata is removed before decoding. A short silent
fallback remains only for an actual empty-buffer network interruption.

Audio downloads/decoding run on clients with bounded buffers and at most four
nearby streams per client. Minecraft's Blocks volume controls playback. Speakers
use normal positioned Minecraft sound instances, which exposes them to compatible
sound-processing mods; Sound Physics reverb still needs testing with that mod.
The status line reports the client's audio mixer status, not a remote server probe.

## Troubleshooting

- **Missing rail / broken shaft:** every cell between markers must be a compatible
  rail with matching facing. Roof-extension rails are unnecessary. Use Rescan.
- **No floors:** the first marker belongs directly above the controller, and every
  higher marker must be connected. Labels are independent of height.
- **Activation refused:** clear the actual cabin and roof volume, load the shaft
  chunks, and install matching linked landings. Three-high and legacy entrances
  must match the chosen layout.
- **Alignment changed:** replace or explicitly realign the landing; check the
  preview. Do not remove cabin/world collision checks to force movement.
- **Can't change a structural setting:** exit and deactivate first.
- **Radio connection fails:** use a direct MP3/Vorbis URL and verify the stream is
  reachable from the player machine. Errors appear in chat with duplicate suppression.
- **Scale 4:** scroll over the middle panel. Tabs and footer stay on screen.

See `ELEVATORS3_V10_VALIDATION.txt` and the implementation report for exact test
results and runtime limitations.
