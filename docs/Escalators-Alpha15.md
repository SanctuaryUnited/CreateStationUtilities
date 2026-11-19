# Create Transit — Alpha 15 escalator and travelator kit

Current model revision: Alpha 15.3. See `Escalators-Inner-LED-Alpha15.3.md` for visible passage-side lighting and `Escalators-Refinement-Alpha15.2.md` for the casing, rails, steps and installation clearance.

Minecraft 1.21.1 / NeoForge. Requires Create; use the same compatible dependency versions as the previous patch. The test environment uses Create 6.0.10 and NeoForge 21.1.248.

Eight new white, dyeable blocks: lower landing, incline, flat section and upper landing, each with glass or solid sides. They have their own models, textures and PBR materials. Existing vehicle and elevator blocks remain independent. EMU and HSR kits remain removed.

## Build an escalator

Place the lower landing facing the direction the escalator rises. The placing player's facing is reversed, like the other kit blocks; check the front of the landing before extending the run.

The first incline sits one block forward at the same block height as the lower landing. Each following incline sits one block forward and one block higher. The upper landing sits one block forward and one block higher than the final incline. Four incline sections therefore connect landings whose block positions differ by four vertically and five horizontally.

Build a floor at the base of each landing: its floor top should match the landing block's bottom, with the landing tread 2 pixels above it. Leave at least two blocks of clear headroom above the belt; more space is comfortable on the slope. The incline model and collision extend above their block position.

For a travelator, use lower landing → flat sections → upper landing, all at the same height. Put matching sections alongside each other to widen either kind of belt. Sections must share facing, direction, part type and height to remove their internal sides. Glass and solid neighboring sections can join. Opposite-direction or differently oriented lanes retain their dividing rails.

## Controls

- Create wrench: reverse the entire connected belt, including all matching adjacent lanes.
- Crouch while placing: start a reverse lane. Ordinary connected placement inherits an existing section's direction.
- Redstone applied to any section: stop the connected belt. Removing power resumes it. Separate reverse lanes remain independent.
- Dye: body/frame. Crouch + dye: LED accent. Creative pick-block preserves both colors.

The internal belt drive runs automatically; no shaft or rotational power is required in this first version. All connected sections use a shared animation phase and fixed horizontal speed of 0.7 blocks/second. Standing living riders are carried with normal world collision. Flying, mounted and actively jumping players are not pulled along. Player motion is predicted on the client; mob motion runs on the server. There is a 512-section connection limit; divide larger installations into separate belts.

## QuickBuilds

Copy the included `.nbt` files into the Minecraft instance's `schematics` folder and load them with Create's Schematic and Quill / schematic tools:

- `Escalator-QuickBuild-2-Wide-Glass.nbt`: two lanes wide, four-block rise.
- `Escalator-QuickBuild-2-Wide-Solid.nbt`: the same layout with solid sides.
- `Travelator-QuickBuild-3-Wide.nbt`: three lanes wide, ten sections long.

The example accent is cyan; the items themselves stay white and dyeable. These are station installations, not moving contraptions.

## Validation and scope

See the accompanying validation report for native Minecraft movement, widening, reversal, redstone, persistence and collision checks, plus model baking checks. No graphical Minecraft client or Complementary shader session was available here: player feel, shader reflections and final appearance still need an in-game playtest.

This patch adds the kit to a temporary Escalators & Travelators creative tab. The combined Vehicle Kits / Station Utilities inventory with named section bars is the next task.
