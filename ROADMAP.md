# Fix Roadmap

Findings from the dev18.4 code review (2026-11-19), verified against NeoForge 21.1.267.
Each item lists the SemVer bump it needs. Tick items off as they land.

## Phase 1 — Security and stability

- [x] **1. Radio station URL trust** (`SpeakerMenu.action`, `ElevatorMenu.action` op 12/14, `RadioAudio.open`)
  - [x] Refuse loopback, private, link-local, multicast, CGNAT and IPv6 ULA hosts on the client, re-checked after every redirect. *PATCH*
  - [x] Refuse localhost names and private IP literals server-side when a station is added; stop advertising saved stations that fail the rules. *PATCH*
  - [x] Server config `radio.allowedHosts` (empty = any public host). *MINOR*
  - [x] Server config `radio.stationEditors`, default `OPERATORS` (op level 2 or single-player owner). *Breaking: builders could previously edit stations.*
  - [ ] Optional: skip warm-up connections to out-of-range sources (trades instant start for fewer connections). *PATCH*
  - Known limit: the client checks DNS before connecting, but `HttpURLConnection` resolves again, so a DNS-rebinding host could still slip through. The operators-only default and the allow-list reduce this risk.
- [x] **2. Client/server thread race on `ElevatorNetwork` maps** (`ElevatorPartEntity.onLoad`/`setRemoved`). The outer per-level maps are now synchronized. They were not gated on `!level.isClientSide`, because shared code such as `ModelLandingItem.useOn` uses these lookups on the client to predict a click's result. `StationSpeakerEntity.setRemoved` is now server-only, matching `onLoad`. *PATCH*
- [x] **3. Radio group overflow can kick a player** (`RadioStations.effective` → `group()` throws at 64). Added a read-only `peek()`; `effective()` and the speaker snapshot use it, so reads never create a group or throw. Only joining a group or editing one creates it. *PATCH*

## Phase 2 — Data growth / leaks

- [ ] **4. `ElevatorBanks` saved data never pruned.** Drop cars, membership, `selectorOverrides` and pending jobs when a drive or panel is permanently removed (not on chunk unload); expire jobs for missing banked panels; expire unassigned jobs with no eligible car. *PATCH*
- [ ] **5. Radio groups can never be deleted.** Prune groups with no members and/or add a delete action. *PATCH (prune) / MINOR (new action)*
- [ ] **6. Direct-buffer churn in radio streaming** (`RadioPcmBuffer.read`, `Mp3.read`, `MonoOgg.read`, Hub reader). Use heap or reused buffers internally; only the buffer handed to OpenAL must be direct. *PATCH*

## Phase 3 — Correctness and performance

- [ ] **7. Copied drives share a UUID** (`ElevatorDriveEntity.loadAdditional`). Regenerate `Identifier` when another loaded drive at a different position already owns it. *PATCH*
- [ ] **8. `ModelCabinController.probe()` constructs a throwaway entity on every landing lookup.** Compute landing positions without an entity, or cache per config/floor. *PATCH*
- [ ] **9. `ensureFloors()` rescans every call for non-rail drives.** Cache legacy floor results per tick. *PATCH*
- [ ] **10. `StationSpeakerBlock.CODEC` loses the style** (always `"box"`). *PATCH*
- [ ] **11. `RadioAudio.Mp3.read` stereo down-mix can index past the buffer** on an odd sample count. *PATCH*
- [x] **11a. Oversized and off-centre inventory icons** (found in 0.3.0 client testing). 26 item models used `display.gui.scale` 0.9 and one used 1.2, so parts overflowed neighbouring slots, and edge-mounted parts drew as bars along the slot edge. Fixed with `tools/fit_gui_icons.py` (run it after the asset generators; `--check` reports drift): it keeps each icon's rotation, sizes it to a vanilla block icon's on-screen footprint, and centres it from the element bounds. 62 icons refitted. Only `display.gui` changed, so placed blocks and held items are unaffected. *PATCH*
- [x] **11b. Radio and elevator motor silent after a sound-engine restart** (found in 0.3.0 client testing). F3+T and Windows audio-device changes restart Minecraft's sound engine, which drops channels without stopping the sound instances. `RadioClient` and `CabinMotorClient` now treat a sound the engine no longer tracks as stopped, and recreate it. Escalator loops already did. *PATCH*

## Phase 4 — Build and tests

- [ ] **12. Gametests don't compile**: `EscalatorGameTests`, `EscalatorSoundGameTests`, `SmartEscalatorGameTests` call the removed `EscalatorNetwork.reverse`. Switch them to `EscalatorNetwork.configure`. *No release*
- [ ] **13. Client tests don't compile**: `RadioAcousticsCheck`, `RadioAudioCheck` use old `RadioAcoustics.Profile`/`fade` and `RadioGainStream` signatures. *No release*
- [x] **14. Bump the dev NeoForge from 21.1.248 to 21.1.267** in `build.gradle`. Keep `neoforge.mods.toml` at `[21.1.248,)`; raising the floor would be MAJOR. *No release*
- [x] **15. Verify the client mixins on 21.1.267.** Checked with in-world testing of 0.3.0: creative tab, cable track and sound engine all clean. Note: `runClient` compiles gametests, so launch it with `-Pelevators30Validation` until item 12 is fixed.
- [ ] **16. Delete the unused `src/main/templates/META-INF/neoforge.mods.toml`** (it wrongly marks CRN as required).
- [ ] **17. Replace the deprecated `EventBusSubscriber.Bus`** before NeoForge removes it (confirm 21.1.248 still routes mod-bus events first).

## Future — Possible features (radio)

Sanctuary Radio runs on AzuraCast and currently uses the MP3 mount
`https://radio.sanctuaryunited.net/listen/sanctuary_radio/radio.mp3` (192 kbps). Station URLs must point at
a stream mount like this, not the `/public/...` player page, which is HTML.

- [ ] **F1. Now-playing titles.** Request `Icy-MetaData: 1` and parse `StreamTitle='…'` from the ICY blocks
  `RadioAudio.IcyMetadataInput` currently discards. Show the title in the speaker screen and cabin displays.
  Works with any Icecast/Shoutcast/AzuraCast mount, with no extra connection. *MINOR*
  - Alternative: AzuraCast now-playing API or WebSocket for album art and up-next. AzuraCast-only, a second
    connection per station, and needs the same host checks as streams.
- [ ] **F2. Stereo, non-positional listening mode** (e.g. inside elevator cabins or an "ambient" option). Keep
  both channels in the decoders for this mode instead of downmixing to mono. This is the only case where a
  higher-quality source becomes audible. *MINOR*
- [ ] **F3. Higher-quality source.** No code needed for Ogg Vorbis: add an OGG mount (160–192 kbps) in
  AzuraCast; the mod already decodes Vorbis. Only worth doing alongside F2. *None*
- [ ] **F4. AAC decoding** (for the 320 kbps `radio-ultrahigh.aac` mount). Needs a bundled decoder (JAAD is
  unmaintained; FFmpeg bindings are large). Little audible benefit while playback is mono and positional,
  and it raises bandwidth per listener from about 192 to 320 kbps. Low priority; reconsider after F2. *MINOR*
- Not planned: WebSocket audio. AzuraCast's WebSocket carries now-playing metadata, not audio, and HTTP
  streaming is already the right way to deliver it.
