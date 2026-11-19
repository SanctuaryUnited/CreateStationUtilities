# Create: Transit Alpha.6 — raised elevator call buttons

Replace the old Transit JAR in your mods folder with the JAR in this ZIP. Keep only one Transit JAR. Update both clients and server.

## Mount buttons at a useful height

For Elevator System 2.0 marker-rail shafts, place the call button against a wall at the marker rail's height, one block above it, or two blocks above it. It calls the nearest physical floor below it within that range. The cabin still stops at its floor marker; raising the button does not move the stop. Existing floor-level buttons keep working.

For an arbitrary mounting height, or adjacent shafts:
1. Right-click the desired Floor Marker Guide Rail with Create's Wrench (not crouching).
2. Crouch-right-click the call button with the Wrench.
3. Right-click the button empty-handed to summon the cabin.

Explicit marker links are saved and ignore the button's height. Repeat with a marker on another shaft for a shared button. Each elevator gets one destination. Repeating a marker link removes it; crouch-right-click the button empty-handed to clear links. Missing explicit targets never redirect to another shaft.

A directly controller-linked button also supports the three automatic mounting rows. Automatic buttons still refuse ambiguous adjacent shafts. Disabled floors do not redirect requests to another floor. Redstone requests and the pending indicator use the same destination resolver. Older elevators without dedicated marker rails retain their original height-based behavior; explicit marker links remain available.

## Implementation

ElevatorNetwork.callFloor resolves physical floor height separately from button placement. ElevatorCallBlock shares it for passenger/redstone requests and indicator tracking. Models, mounting faces, existing links, animations, doors, dyes and unrelated features are preserved. Internal version: 0.1.0-alpha.6.

Compilation passed. Native Minecraft GameTests: all 147 passed. The new height/link persistence test and updated raised-button travel/obstruction/indicator and shared-bank tests passed. All connected-dye tests also passed.

Only ElevatorCallBlock, ElevatorNetwork and version metadata differ in the JAR from alpha.5. Existing models and other code are unchanged. Graphical placement appearance and live multiplayer interaction have not been manually tested. Tests used the existing optional Steam ’n’ Rails exclusion and private headless environment workarounds; these workarounds are not shipped.

JAR SHA-256: `986ec1f0da346fb560ffd9d1d5b042f9c0eb4da7e3bfefea7e20b516dd78de07`
