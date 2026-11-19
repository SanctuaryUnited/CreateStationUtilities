# Elevator Floor Markers — Alpha13.8

Replace the previous Create Transit JAR with `create-transit-0.5.0-alpha.13-elevators.8.jar`. Install only one version. Existing elevators and unlinked call buttons continue to work.

The **Elevator Floor Marker** is a stationary, dyeable floor plate in the Elevators creative tab. Place it on the landing floor beside the elevator entrance, outside the moving cabin and shaft. The marker calls the cabin to the row of the supporting floor block: if the floor block is at Y=70, the marker sits at Y=71 and its stop is Y=70. Marker floors also appear in the cabin's existing floor selection menu.

## Link one button to several elevators

Use the standard **Create wrench**:

1. Right-click the first elevator's **Drive** with the wrench. This selects that drive without assembling or disassembling it.
2. **Crouch + right-click its Floor Marker** with the wrench. The marker is now bound to that drive.
3. Stand normally and **right-click the marker** with the wrench. This selects the marker for linking.
4. **Crouch + right-click the Call Button** with the wrench. The marker is added to that button's links.
5. Repeat steps 1–4 for each other elevator, using the same call button.
6. Right-click the button normally, or power it with a **redstone rising edge**. Every linked elevator is called to its own marker's stop. A valid unassembled cabin is assembled automatically, just like an ordinary call button.

The marker initially binds to the nearest loaded drive when placed. Explicit binding is recommended for a bank of elevators so each plate targets the intended shaft. Ordinary empty-hand right-click on a marker shows its stop and drive coordinates. Wrench-clicking a button while standing shows its link count.

## Change or remove links

- Select a marker again, then crouch + wrench the same button to remove just that marker's link. Repeat the same gesture to add it again.
- **Crouch + empty-hand right-click the button** to clear all its links. It returns to the original nearest-elevator behavior.
- To rebind a marker, wrench the new drive, then crouch + wrench the marker.
- One marker may be linked to multiple buttons.
- Links and marker drive bindings persist when the world is saved and loaded. Breaking the block drops a dye-preserving item with no old links attached.

## Placement and limits

- Keep each marker within **8 blocks horizontally** of its drive and no more than **256 blocks above** the drive's base floor. Stops below the base are unsupported.
- A button may link to **32 markers**, each within **256 blocks** of that button, in the same dimension.
- Each elevator has the existing **32-stop limit**, including its home floor and any legacy call-button stops.
- One button serves one stop per elevator. Linking conflicting stop heights for the same drive is rejected.
- Markers and drives must be loaded to resolve calls. Missing, removed or unloaded linked targets are skipped. The other valid elevators are still called; configured buttons never fall back to another nearby elevator.
- Keep the existing guide rail, unobstructed shaft and landing doors. Markers define stops; they do not replace these parts.

The marker's body and LED rim use the same dye controls as the elevator kit: dye changes the body; crouch + dye changes the accent. The corrected Alpha13.7 opening chimes and silent closing behavior are retained.

## Verification

Java 21 / Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10. All **99 native GameTests passed**, including three marker integration tests: actual wrench interactions; floor discovery, persistence and missing-target behavior; and a glass/solid two-cabin bank called upward by one button and returned by redstone. The full build, timing/network checks, elevator model baking and existing vehicle model checks passed. Optional Steam 'n' Rails tests were not included. No graphical shader client was launched.
