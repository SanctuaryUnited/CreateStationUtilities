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

- [x] **4. `ElevatorBanks` saved data never pruned.** `ElevatorBanks.prune()` runs every 10 s and only judges positions in loaded chunks, so out-of-range state is never touched:
  - Cars whose drive is gone are removed from their bank and from nested merged banks, so undoing a merge can't restore them.
  - Banks left with no cars are deleted.
  - Selector overrides pointing at a missing panel or a deleted bank are dropped.
  - Calls whose bank is gone are dropped. A call no car can serve for 60 s, while its panel and cars are loaded, expires and its riders are told.
  - It checks the world instead of relying on break events, so data from older versions is cleaned too. Banked calls are tied to the landing rather than the panel that placed them, so breaking one of several panels keeps the call.
  - Note: car letters are assigned by position in the bank, so removing a car re-letters the cars after it. *PATCH*
- [x] **5. Radio groups can never be deleted.** `RadioStations` now saves group members (`Members`: dimension@pos → group). They are recorded on join or leave and when a speaker loads, and `prune()` runs every 10 s:
  - It drops a member only when its chunk is loaded and no speaker there is in that group.
  - It deletes a group after `GROUP_GRACE_TICKS` (24000) with no members. The delay covers speakers carried on contraptions.
  - Legacy groups, saved before `Members` existed, are flagged `Untracked` and never auto-deleted, since their unloaded speakers were never recorded. At most 64 can remain.
  - A manual delete action is still possible as a *MINOR*. *PATCH*
- [x] **6. Direct-buffer churn in radio streaming.** `RadioPcmBuffer.read`, `Mp3.read` and `MonoOgg.read` now return heap buffers. The Hub reader still returns a direct buffer, because it goes to OpenAL through `RadioGainStream`. `Hub.append` and the reader copy in bulk around the ring instead of byte by byte. The raw buffer from vanilla `JOrbisAudioStream` is left as is. *PATCH*

## Phase 3 — Correctness and performance

- [ ] **7. Copied drives share a UUID** (`ElevatorDriveEntity.loadAdditional`). Regenerate `Identifier` when another loaded drive at a different position already owns it. *PATCH*
- [ ] **8. `ModelCabinController.probe()` constructs a throwaway entity on every landing lookup.** Compute landing positions without an entity, or cache per config/floor. *PATCH*
- [ ] **9. `ensureFloors()` rescans every call for non-rail drives.** Cache legacy floor results per tick. *PATCH*
- [ ] **10. `StationSpeakerBlock.CODEC` loses the style** (always `"box"`). *PATCH*
- [x] **11. `RadioAudio.Mp3.read` stereo down-mix can index past the buffer** on an odd sample count. Fixed alongside item 6. *PATCH*
- [x] **11a. Oversized and off-centre inventory icons** (found in 0.3.0 client testing). 26 item models used `display.gui.scale` 0.9 and one used 1.2, so parts overflowed neighbouring slots, and edge-mounted parts drew as bars along the slot edge. Fixed with `tools/fit_gui_icons.py` (run it after the asset generators; `--check` reports drift): it keeps each icon's rotation, sizes it to a vanilla block icon's on-screen footprint, and centres it from the element bounds. 62 icons refitted. Only `display.gui` changed, so placed blocks and held items are unaffected. *PATCH*
- [x] **11b. Radio and elevator motor silent after a sound-engine restart** (found in 0.3.0 client testing). F3+T and Windows audio-device changes restart Minecraft's sound engine, which drops channels without stopping the sound instances. `RadioClient` and `CabinMotorClient` now treat a sound the engine no longer tracks as stopped, and recreate it. Escalator loops already did. *PATCH*

## Phase 4 — Build and tests

- [x] **12. Gametests don't compile.** The three escalator suites now flip direction through `escalator.EscalatorTestAccess`, which calls `EscalatorNetwork.configure` the way the settings menu does. `runClient` works again without `-Pelevators30Validation`.
  - `SpeakerUpdateGameTests` now expects one saved group, because item 3 stopped reads from creating groups.
  - The scratch tests from 0.3.0–0.4.0 are now permanent: `RadioSecurity`, `RadioGroup`, `RadioGroupPrune`, `BankPrune` and `DefaultStations`. *No release*
- [x] **13. Client tests don't compile.** `RadioAcousticsCheck` and `RadioAudioCheck` were updated to the current `Profile`, `fade` and `RadioGainStream` signatures. Added `radioStreamCheck`, which checks heap internal PCM, direct OpenAL output and byte order across the ring wrap. Six of the seven standalone client checks pass. *No release*
- [x] **14. Bump the dev NeoForge from 21.1.248 to 21.1.267** in `build.gradle`. Keep `neoforge.mods.toml` at `[21.1.248,)`; raising the floor would be MAJOR. *No release*
- [x] **15. Verify the client mixins on 21.1.267.** Checked with in-world testing of 0.3.0: creative tab, cable track and sound engine all clean. Item 12 fixed the gametest compile, so plain `runClient` works.
- [x] **18. 26 gametests and `elevatorModelCheck` already failed in the dev18.4 import.** They fail identically on the untouched `v0.2.0-dev18.4` source, and hadn't run because the suite didn't compile. Each was diagnosed with actual values and updated only where a release note (DEV16, DEV17 or DEV18.x), the current `Elevators30GameTests` or an explicit code rule documents the change:
  - 2.8-tall V17 shell, while compact legacy shells stay 2.5.
  - 3×3 only for new cabins, and V17 nudges move in whole blocks.
  - Three-row landings, and the door mechanism and two-speed options are removed.
  - Static blocks now block assembly, and a cabin can't face its own guide rail.
  - No 32-stop cap.
  - The Destination Selector raises the parts count to 25 and the Station tab to 26.
  - The default light level is 6, landing facing follows the cabin, and `IDLE` replaces "doors open while closed".
  - Update Cabin now starts from V13 geometry.

  Narrowed checks keep their intent: sizes 2/4/5 still load, and four-way rotation still runs, with the 180° turn expected to be refused. `runGameTestServer` now uses its own `run-gametest/` folder and starts from a fresh world. 193 of 194 pass; the remaining failure is item 19. *No release*
- [x] **19. Starter kits facing east or south can't install their home-floor landing.** The computed landing position overlaps the kit's own floor-marker rail ("Landing space obstructed"), while north and west work. Found by `kit_preview_rotates_without_placing_or_consuming_then_confirms`.
  - Cause: setting the size re-applies the rail mount, and its quarter turn turned every kit cabin away from its previewed entrance. East and south kits then could not install a home landing, and west kits had the controller in front of the doorway.
  - Fix: the kit's compact mount resets that rotation, so the cabin matches the preview. The preview and the gametests now share `ModelCabinItem.previewCabin`.
  - `KitPlacementGameTests` checks all four facings: footprint, gold entrance, landing install, activation and a clear doorway. Fixed in 0.4.1. *PATCH*
- [x] **20. Clearer refusal when a rotated cabin's entrance would face its guide rail.** Activation now reports when the cabin would cut into its own drive or rail column. Fixed in 0.4.1. *PATCH*
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
- [x] **F5. Default stations from server config** (shipped in 0.4.0). `radio.defaultStations` takes `"Name|URL"`
  entries. Each URL is offered to a world once, on server start or config reload, and tracked in
  `SeededDefaults`, so an operator's removal sticks. A URL already in the list counts as added. Entries go
  through the same checks as player-added stations: public host, `allowedHosts` and the 16-station cap.
  Refused entries are logged and retried later. The default is empty, so the mod still ships with no
  stations. *MINOR*
- Not planned: WebSocket audio. AzuraCast's WebSocket carries now-playing metadata, not audio, and HTTP
  streaming is already the right way to deliver it.
