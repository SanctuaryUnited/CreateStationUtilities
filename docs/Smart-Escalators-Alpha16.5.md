# Create Transit Alpha16.5 — smart escalators

For Minecraft 1.21.1, NeoForge 21.1.248 or newer in the 21.1 line, and Create 6.0.10. Steam 'n' Rails remains optional.

## What changed

- Closed the handrail gaps at both landing/incline joints with matching mitered edge coordinates.
- Removed overlapping interior faces from the rounded rubber handrails.
- Added automatically shaped glass and solid escalators, each with a regular and a redstone-powered version.
- Each style uses one inventory item for its lower landing, incline and upper landing. Shape changes preserve the same block entity, frame dye, LED dye and motion clock.
- Neighbouring matching lanes widen the escalator and remove the inside rails. Partially built wider rows use the complete lane's height profile.
- Retained the confirmed Alpha16.4 collision, selection-edge caching, Flywheel instancing, fallback rendering, dye packet refresh and motor audio.

## Build with one item

1. Choose **Escalator (Glass)** or **Escalator (Solid)** in **Station Utilities → Escalators / Travelators**. The redstone-powered styles are beside them.
2. Face uphill and place the first section at the foot of the escalator.
3. Place the next section one block forward at the **same height**. These form the lower landing and the first incline.
4. Add each following section one block forward and one block higher. The last section becomes the upper landing; extending it upward turns the former landing into another incline.
5. Repeat beside an existing lane to widen it. Clicking an existing section's side inherits its uphill orientation and travel direction.

Example: for a north-facing run with three inclines, place sections at `(0,0,0)`, `(0,0,-1)`, `(0,1,-2)`, `(0,2,-3)` and `(0,3,-4)`. The first and last sections become landings.

One item is consumed per placed section. A single click does not spawn the entire staircase. Automatic selection replaces the three separate shape items.

## Power and customization

| Style | No redstone signal | Redstone signal at any connected section |
| --- | --- | --- |
| Regular | Runs | Stops |
| Redstone-powered | Stops | Runs |

A lever, button, redstone dust circuit or other normal Minecraft signal can control the powered run. Keep a signal on for continuous motion. Power reaches every connected matching lane; regular and powered belts have separate networks and keep a dividing rail between them.

Use a Create wrench to reverse the connected run. Crouching during placement starts a reverse lane. Dye-click changes the frame; crouch + dye-click changes the LED accent. Glass, inner and outer LED trim, direction indicators and the moving motor sound remain available.

## Existing builds

The eight original section IDs remain registered. Existing escalators and travelator QuickBuilds load with their original layout; their landing joints select the repaired geometry automatically. The three old escalator shape items are hidden from the creative catalog, and picking or breaking an old escalator piece returns the matching new smart item. The two travelator items remain available.

Install only the new Create Transit JAR in place of the older Create Transit JAR. Keep Create and your other dependencies installed.

## Validation scope

Native Minecraft server tests exercise placement, widening, reversing, power transitions, collision, persistent dyes and the other retained kits. Native CLIENT datagen verifies the creative inventory, mixins, Flywheel registration and both tint channels. Geometry checks bake the generated meshes and compare exact landing joint coordinates. The included preview is a software render of the actual model JSON. Complementary shader appearance and hardware FPS require a graphical game session; neither is measured by these checks.
