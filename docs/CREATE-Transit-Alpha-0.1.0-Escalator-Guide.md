# CREATE Transit Alpha 0.1.0 — deck, icon and inventory fixes

For Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10 and Java 21.

## This patch

Moving treads and yellow edge strips stop at the fixed terminal plate instead of rendering underneath it. This removes the nearly coplanar surfaces that produced the overlapping pattern in flat landings. Internal walkway-to-escalator connections no longer have an extra fixed end plate. The moving belt and exposed plates cover the full section without gaps.

All twelve current and legacy escalator/travelator items use flat pixel icons. Separate texture layers retain frame dye and LED dye, while glass, rubber and the powered variant's redstone badge keep their original colours. The Station Utilities tab now follows Vehicle Kits; their native NeoForge order is checked during client validation.

Flat travelators now have the same rounded handrail returns and terminal housings as escalators. The entrance and exit are selected automatically from neighbouring sections. A lone section has a return at each end. Extending or splitting a run moves the end pieces to its exposed ends.

When a level travelator leads into an escalator, the connecting landing uses continuous handrails, glass and inner/outer LED trim. Internal connections omit terminal housings and overlapping glass end faces. The moving handrail grips and direction indicators follow the selected end shapes.

These changes apply to glass, solid, regular and redstone-powered sections, including older fixed-section blocks. They keep their existing dye colours and remain physically flat where placed on level ground.

## Placement

Station Utilities contains four shared items: glass, solid, redstone glass and redstone solid. Each builds travelators and escalators; one item places one section.

- Place a level row to make a travelator. Rounded returns appear at its exposed entrance and exit.
- Build forward and one block higher to make an incline. The adjoining flat sections become lower and upper landings.
- Add flat sections before or after the stairs to extend the walkway. Internal returns disappear and the handrails connect.
- Place matching sections beside each other to widen the run. Interior rails disappear.
- Click an existing section to inherit its facing. Crouching starts a reversed lane; a Create wrench reverses a connected run.

Sections must have matching facing, direction and control mode to share a connection. A gap separates the runs.

## Power and dyes

| Section type | No redstone signal | Signal at any connected section |
| --- | --- | --- |
| Regular glass or solid | Runs | Stops |
| Redstone glass or solid | Stops, inner and outer LED trim off | Runs, inner and outer LED trim on |

Right-click with dye to colour the frame; crouch and dye-click to colour the LED trim. Unpowered redstone trim retains its saved dye, uses normal shading and has zero PBR emission. Direction and stop indicators continue to show the lane state. The existing motor sound follows movement.

## Installation

Replace the previous Create Transit JAR with **CREATE Transit Alpha 0.1.0.jar**. Keep one Create Transit JAR in the mods folder.

Existing sections refresh their connections when their chunks load; rebuilding them is unnecessary. Existing QuickBuilds and original fixed-section registrations remain supported. Pick-block and drops use the shared family item. Old fixed-section inventory items can be converted with the existing one-item recipe.

The internal mod version is `0.1.0-alpha.2` to distinguish this patch from the preceding build. The download filename remains the requested Alpha 0.1.0 name.

## Validation

121 required native Minecraft GameTests passed in a fresh world, including survival placement, all facings, both construction orders, widening, power controls, splitting and rebuilding level runs. Native CLIENT validation passed for the inventory mixins, Flywheel registration and 3,072 frame/LED dye combinations.

148 static models baked in four orientations. 320 moving configurations matched the preview geometry; 2,048 visual states and 36 handrail joint edge sets passed. 6,336 deck animation states have complete, nonoverlapping coverage. Native moving-geometry checks also verify the end-plate boundaries. Item tint validation covers all 3,072 frame/LED combinations and the untinted detail layer.

Collision and reusable-buffer performance checks passed. Collision, rider-motion and sound implementations retain the preceding patch's bytecode. The renderer still reuses bounded mesh storage and Flywheel unit surfaces while omitting the hidden end-plate overlaps.

The preview uses the actual generated models. Hardware FPS and the graphical appearance under Complementary Reimagined were not measured.
