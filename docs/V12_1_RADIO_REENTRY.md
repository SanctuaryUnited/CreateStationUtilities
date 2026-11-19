# V12.1 — speaker reverb recovery

Install `create-station-utilities-0.2.0-dev12.1.jar`, replacing the previous
Station Utilities JAR. Keep existing speaker blocks, stations, groups, dyes and
range settings. No cabin conversion or speaker rebuilding is needed.
Sound Physics Remastered remains optional and client-side for this integration;
this patch was tested against its NeoForge 1.21.1-1.5.2 build.

## Fix

Sound Physics defaults `update_moving_sounds` to false. A long-running radio
could keep the filter and reverb calculated when it started. Starting behind a
wall, at a greater distance, or while chunks were arriving could leave that
result in place after the player returned inside. Async stream startup also
used Sound Physics' global last-sound identity, which another sound could change
while the radio buffered.

The radio now recalculates on its actual playback channel after the stream
starts, using that stream's identity. When Sound Physics' global continuous
updates are off, our active radio sources refresh every five ticks, staggered
across the existing twelve-source limit. When global updates are on, Sound
Physics handles periodic refresh; the patch does not add a second periodic loop.
Your global Sound Physics settings are not changed.

This applies to both speaker and cabin radio, with or without the custom
Extended reverb toggle. Normal Sound Physics occlusion still applies through
real walls; the fix lets it recover when you move back to a clear position.
Stopped/replaced sources and world changes clear their old acoustic profiles.
The configured processing-distance override now uses the actual Double API type.

## Test

The development regression uses a real integrated Minecraft world, a stone
room, a placed speaker, an HTTP MP3 fixture, and real native OpenAL channels.
A separate test-only mixin disables the new refresh path as a negative control.
After returning through the blocked side, the test takes one blocked listener
sample on the actual audio thread, then moves inside and checks recovery.

With refresh disabled: high-frequency transmission stayed at **0.049787067**
after returning inside, and the environment update count stayed at four.
With the patch: all three exits/re-entries restored transmission to **1.0**,
direct gain to **0.68741**, and reverb-send parameter sum to **2.6461728**.
The source was stopped outside coverage and restarted each time, reusing native
source ID 1. The test left global moving-sound updates off and set the ordinary
Sound Physics processing limit to 8 blocks, below the blocked listener distance,
to also exercise the radio-specific processing-distance override.

OpenAL used its No Output device. This checks actual playback/channel lifecycle
and environment-setting calls, not audio heard by a person. Listening in your
complete mod pack and connected multiplayer remain follow-up checks.

For an in-game check: enable a station, walk out of coverage, return from behind
a wall, then enter its room. Reverb and clarity should recover without toggling
power or replacing the speaker. Keep the configured ranges and source chunks
loaded as appropriate. This patch does not remove ordinary wall muffling.
