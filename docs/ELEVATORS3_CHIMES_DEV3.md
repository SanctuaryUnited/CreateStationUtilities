# Eight elevator chime presets — dev3

Install create-transit-0.2.0-dev3.jar on client and server, replacing the prior Transit JAR. Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10. All dev2 model-cabin features remain available.

Use the Cabin Kit on your elevator controller or guide rail. In model settings, go to the doors/audio page (the third settings page). Click **Chime** to cycle through:

| Preset | Audio |
| --- | --- |
| Otis-style | Existing original cue; unchanged |
| Schindler HT-style | Existing original cue; unchanged |
| Arrival ding | lmbubec — Elevator Ding.wav |
| Elevator bell | NachtmahrTV — Elevator Bell |
| Japan arrival | gregconquest — Japanese elevator arrival recording |
| Classic bell + doors | cognito perceptu — elevator bell doors.wav |
| FM bell | CosmicD — FM Elevator bell |
| Synth ding | fvcalderan — Elevator ding |
| Off | No arrival/opening chime |

The choice is saved on the server and applies to that model cabin. Existing worlds keep their previous selection. The cue plays when the doors begin opening from fully closed; opening both service sides together produces one cue. No separate closing chime is added. Door hiss and motor hum retain their own switches, and the Blocks audio category controls playback volume.

The two original presets are preserved, not replaced by web recordings. The six new choices use the exact mono conversions from the delivered CC0 audio pack; some include door movement or room ambience. They are full clips, not newly isolated or denoised samples. New choices are available on model-based cabins; the legacy assembled-block system retains its existing style-based cues.

All six additional sources list CC0-1.0. Five use public Freesound HQ preview MP3 derivatives, while Synth ding uses the original OpenGameArt OGG attachment. All packaged copies are mono 48 kHz Vorbis. Manufacturer identity for these six is unverified; Japan arrival is not labeled as an official Mitsubishi recording. Exact sources, credits, license reference and SHA-256 hashes are inside the JAR under META-INF/createtransit-elevator-chimes-cc0.json and META-INF/createtransit-elevator-chimes-credits.txt. These CC0 assets retain their own dedication independently of the mod license.

Validation: tools/validate_elevator_chimes.py decodes the six files and verifies provenance hashes/resources/labels. Supplying the dev2 JAR as its argument also checks that the original two cue files have identical bytes. The native GameTest fixture verifies sound registration, original event continuity, server acceptance and NBT persistence. Audible playback on a graphical client remains a manual test.
