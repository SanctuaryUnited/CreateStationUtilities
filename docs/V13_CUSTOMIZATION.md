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
