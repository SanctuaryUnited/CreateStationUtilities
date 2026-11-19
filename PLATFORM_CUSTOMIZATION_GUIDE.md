# Platform customization — Alpha 12

Minecraft 1.21.1 • NeoForge 21.1.248 • Create 6.0.10

Replace the older Create Transit JAR with the Alpha 12 JAR; use only one version at a time. Existing kit block IDs and saved glass panels remain compatible.

## Panel selection

With an empty main hand and empty off hand, crouch and right-click any part of a platform doorway or wall. Each click cycles:

1. Clear glass (the original appearance).
2. Divided glass (physical vertical mullions separating the panes).
3. Solid metal panel (no glass).

The entire doorway or wall changes together. The selection survives saving, pick-block, breaking, and replacing. Doors remain two blocks wide. All styles work on half-height platform gates/barriers, two-block full-height doors/walls, and three-block full-height doors/walls. No extra color-specific items are added.

Right-click with dye to color the frame/panels. Crouch-click with dye to color the accent stripe and side LEDs; this does not change panel style.

## Automatic wall corners

Place a matching-height wall at a right angle immediately behind the existing wall, or build the pair in the reverse order. The existing wall automatically gains a return along the appropriate block edge to join the perpendicular wall. The return includes real collision and carries the shared redstone bus around the bend. Removing the neighbor restores the straight wall.

The modules remain aligned to the facing edge of their blocks. For example: a north-facing wall followed by an east-facing wall in the block immediately south creates a right-hand return. Rotate or mirror this layout for the other directions. Doors do not bend; use the matching wall module to finish a doorway bank.

Two- and three-block full-height walls can also join; the corner exists at their shared rows. Each module retains its own chosen panel style and dye colors.

## Sound and status sequence

Platform doors now use the same opening and closing chime/hiss cues as the existing LRT doors. The platform closing-warning pause is retained.

- Opening: status lights flash green.
- Fully open: status lights remain steady green.
- Closing: existing yellow warning sequence is preserved where supported.
- Fully shut: each kit retains its existing off/white/dyed resting appearance.

Opening signals are updated for platform doors, single/wide gondola doors, LRT, EMU, and HSR doors, including their existing moving-train render paths. Fare-gate direction arrows and rear stop indicators are navigation markings, so their direction colors remain unchanged.

## Validation and preview

The included preview uses the actual generated platform geometry. It is an offline render, not a Minecraft screenshot. Geometry validation checks all styles/heights, left/right corner returns, non-overlapping surfaces, and sliding clearance. Headless Minecraft GameTests cover real interactions, dye preservation, drops, corner collision, and redstone transmission. Vehicle model-loader and existing kit regression checks are also run.

A live graphics-client check with your Complementary Reimagined configuration is still needed to confirm the final shader appearance.
