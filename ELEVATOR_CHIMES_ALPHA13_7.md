# Create Transit Alpha13.7 — Elevator Chimes

Replace your previous Create Transit JAR with `create-transit-0.5.0-alpha.13-elevators.7.jar`. Do not install both versions.

- Solid cabin: same original bell twice, strikes 0.65 seconds apart. The second ring lasts longer.
- Glass cabin: deeper high-to-low electronic pair (659.25 Hz then 523.25 Hz), starts 0.32 seconds apart.
- Elevator cabin and landing doors close silently. Other vehicles retain their existing sounds.
- No changes to elevator blocks, dyes, animations, QuickBuild layouts or placement.

Sound previews are decoded from the exact OGG files included in the JAR. These are original manufacturer-inspired cues.

Focused playback, audio and archive checks passed. Full native Minecraft tests were not rerun for this sound patch. The prior release had one unresolved LRT circuit test timeout; all elevator tests passed. See the validation report for details.
