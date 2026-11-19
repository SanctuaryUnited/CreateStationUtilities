# Create Transit — Escalator sound patch (Alpha15.4)

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21.

## Sound and controls

- An original, quiet electric motor hum with soft rolling tread and chain noise.
- Continuous 8-second mono Vorbis loop, positioned at the nearest moving deck section.
- One loop per connected escalator/travelator belt, including wide glass/solid builds.
- Opposite-direction lanes have independent sound sources.
- Runs automatically with the steps in either direction; resumes after redstone power is removed.
- Redstone stop fades the loop out over 4 ticks (0.2 seconds); startup fades in over 8 ticks (0.4 seconds).
- Audible locally, fading with distance over a 12-block range. Adjust Minecraft's Blocks volume slider to change loudness.
- Block removal, source-chunk unloading, world changes, logout, and sound-engine reload clean up playback.

The client checks already-loaded nearby chunk entities twice per second. Playback uses synchronized block states and does not send repeated sound packets or force chunks to load.

All eight escalator section IDs and Alpha15.3 inner/outer LED models are included. Existing escalator and travelator QuickBuilds work with this patch.

## Installation

Replace the previous Create Transit JAR in the `mods` folder with `create-transit-0.5.0-alpha.15-escalators.4.jar`. Keep only one Create Transit JAR installed.

## Source and asset

`tools/generate_escalator_audio.py` creates the sound from periodic synthesis and seeded noise; it uses no recordings. The in-game file is `assets/createtransit/sounds/escalator_running.ogg`. The subtitle is “Escalator motor and moving steps”.

See `Alpha15.4-Validation.txt` for the checks actually completed and their limits.
