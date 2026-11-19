# Create: Transit — Elevator System 2.0 implementation report

**Release:** 0.1.0-alpha.3 • **Date:** 11 October 2026

## Delivered implementation

This is an update to the supplied Create: Transit source project. The existing independent elevator controller and native Create contraption integration were extended. It is not a UI mockup or a teleport-based moving platform.

The release provides dedicated stopping rails, automatic floor discovery, an actual Minecraft controller screen, server-owned configuration, a passenger request queue, enclosed moving cabins and automatic interlocked doors. Glass and solid three-floor QuickBuilds are included. The mod ID remains `createtransit`; the requested download name remains `CREATE Transit Alpha 0.1.0.jar`, with embedded version `0.1.0-alpha.3` following the project's alpha patch convention.

## Verified project dependencies

| Component | Source project / build dependency |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.248 |
| Create | 6.0.10+mc1.21.1; required runtime metadata range [6.0.10,6.1) |
| Java | 21 |
| Gradle wrapper | 9.2.1 |
| NeoForge ModDev plugin | 2.0.148 |
| Steam ’n’ Rails | Existing pinned Modrinth compile-only API `L3Jv0QZI:HOPX5oro`; optional separate runtime mod |
| Create Railways Navigator | Existing optional metadata preserved |

Create's installed classes were inspected before integration, including `ControlledContraptionEntity`, `TranslatingContraption`, `IControlContraption`, the movement interaction/collider APIs and `AllGuiTextures`. Networking uses actual NeoForge `CustomPacketPayload`, `StreamCodec`, `RegisterPayloadHandlersEvent`, `PacketDistributor` and menu-opening APIs. No invented Create interfaces or methods were introduced.

## Features and behavior

| Area | Implemented behavior |
| --- | --- |
| Standard rails | Existing `elevator_guide_rail` ID retained; vertical up/down connections, neighbor orientation inheritance, end caps and loaded-column ownership. Existing mounting/profile retained. |
| Dedicated marker rails | New registered `elevator_floor_marker_rail`, item, block entity support, recognizable illuminated marking, model, blockstate, loot, recipe, pickaxe tag and creative inventory entry. No rail-conversion interaction. |
| Discovery | Scans upward on the controller's exact X/Z column; records actual positions and sorts by physical Y. Does not cross gaps, another drive, orientation changes or unloaded chunks. Maximum 32 stops and 256 blocks of travel. |
| Labels and service | Default F1/F2/F3 example labels; arbitrary unique IDs and names, independent of height. New labels avoid existing IDs. Marker configuration persists server-side. Disabled/inaccessible stops reject ordinary requests. |
| Automatic updates | Rail/marker changes invalidate their shaft; loaded controllers also rescan periodically. Added/removed markers update the list and prune invalid pending requests. Negative-Y gap detection is supported. |
| Controller GUI | Real `ElevatorMenu` and `ElevatorScreen`; Create schedule-card/brass textures, elevator name/UUID, current/target floor, operating state, rail status, queue, descending paginated stop rows, floor editing, enable toggles, delay settings, Rescan, Assemble, Retry and Return/edit. |
| Client synchronization | Bounded action payloads and complete controller snapshots. Mutations validate the active menu, owning player, permissions and distance/cabin presence; passenger menus cannot edit configuration. |
| Requests | Bounded FIFO queue with duplicate coalescing. No scheduled floor cycling. Repeat requests for the current stop reopen its appropriate doors. |
| Cabin assembly | A complete rectangular Elevator Floor anchors the assembly. Face-connected ordinary blocks and compatible elevator walls, glass, ceilings, doors, lighting and decoration are captured within its footprint. Entire assembly moves as a native Create controlled contraption. |
| Movement | Cubic easing supplies acceleration/deceleration; precise offset alignment and an explicit LEVELING state. Real contraption motion/collision carries standing riders. The swept cabin volume and rails are checked before/during travel. |
| Automatic doors | Existing animated glass/solid cabin and landing doors reused. Complete adjacent door pairs must face opposite directions. Floor-specific front/rear matching; other sides stay closed. Doors close and confirm closed before movement, remain locked while moving/leveling, and open only with an aligned cabin. |
| Obstruction handling | Living-entity doorway detection holds/reopens doors while preserving the queued request. Broken rails, invalid destinations, interrupted door interlocks and blocked movement enter FAULT. Loaded open landing doors fail closed without an aligned cabin. |
| Delays | Server-saved opening and closing delays (0–200 ticks) and dwell (0–600 ticks); 40-tick existing door animation, minimum five-tick closed-door leveling interval. AUTO is the default/current mode. |
| Call controls | Existing wall-mounted call button supports click/redstone requests and an amber pending indicator. Unique-shaft automatic association or explicit wrench links. One button can target one marker per elevator across a bank; invalid explicit links never fall back to another shaft. |
| Cabin controls | Existing moving control panel opens a passenger destination menu for its exact owning controller rather than the nearest shaft. |
| Persistence | Saved controller UUID/name, discovered stops, marker settings, delays, queue, phase, offset, easing progress, destination and native cabin UUID. Unloaded owner pauses cabin motion; loaded controller rebinds its native cabin. |
| Maintenance | Return/edit travels to the assembly floor and restores the cabin blocks. Maintenance return remains available when the home floor is disabled. Retry is restricted to FAULT and cannot interrupt a healthy trip. |
| Existing builds | Existing elevator registry IDs, older floor plates/call linking, dyes, arrival chimes, connected walls and ceiling variants retained. Dedicated-marker mode becomes sticky once selected, preventing a missing marker network from silently turning into legacy nearest-floor behavior. |

Floor alignment uses **marker Y minus one = cabin floor-block Y**. The highest usable stop accounts for the actual assembled cabin roof height; it is not a fixed-height platform offset. The drive remains at the lowest assembly/served floor.

## New source files

All production paths below are relative to `src/main/java/com/sanctuarygames/createtransit/elevator/`:

| File | Responsibility |
| --- | --- |
| `ElevatorFloor.java` | Immutable stop data and NBT serialization. |
| `ElevatorGuideRailBlock.java` | Standard/compatible rail connections, placement and shaft lookup. |
| `ElevatorFloorMarkerRailBlock.java` | Dedicated stopping rail interaction and controller access. |
| `ElevatorRailDiscovery.java` | Bounded single-column floor discovery and warnings. |
| `ElevatorPackets.java` | Authoritative controller action/snapshot networking. |

Also added `Elevator2GameTests.java`, a native test structure, two Elevator2 NBT/JSON QuickBuilds, `tools/generate_elevator2_assets.py`, `tools/export_elevator2_demo.py`, the marker/end-cap resources and this release's documentation.

## Existing classes modified

| Classes | Main change |
| --- | --- |
| `ElevatorDriveEntity` | Discovery/configuration, request queue, state machine, automatic doors, movement validation, persistence and snapshots. |
| `ElevatorDriveBlock` | Automatic facing when attached to a floor, controller/assembly interaction. |
| `ElevatorContraption`, `ElevatorCabinEntity` | Enclosed/generic block capture, useful assembly errors, restored modules, exact controller ownership and unloaded-owner pause. |
| `ElevatorPartBlock`, `ElevatorPartEntity` | Dedicated rail kind, persistent marker data, door-closed check and call indication / landing safety ticks. |
| `ElevatorCallBlock`, `ElevatorLinking`, `ElevatorNetwork` | Physical discovered-stop requests, exact marker/controller links, shaft isolation and geometrical landings. |
| `ElevatorDoorBlock` | Landing interaction respects aligned-cabin interlocks. |
| `ElevatorMenu`, `client/ElevatorScreen` | Real configuration/passenger UI with authority checks and snapshot synchronization. |
| `ElevatorSetup`, `client/ElevatorRenderer`, `ElevatorRegistry` | Moving panel ownership, call indicator rendering and marker/payload registration. |

The build version, guide-rail blockstate, model manifest, language additions and pickaxe tag were updated. Existing elevator/model and inventory tests were adjusted only for the added registration count; existing behavior checks were retained. The new client validation checks the production screen registration and installed Create texture resources.

## Tests actually performed

**Final result: BUILD SUCCESSFUL. All 136 required native Minecraft GameTests passed.** This includes the original 121 required tests available without the optional Steam ’n’ Rails runtime and 15 new Elevator2 tests.

| Validation | Result / scope |
| --- | --- |
| Gradle `build` / `transitCheck` | Passed; Java 21 production code and resources compile and package. Existing reversible travel, warnings, network changes/disconnection and bounded failure checks pass. |
| Native `runGameTestServer` | All 136 required tests pass on Minecraft 1.21.1 + NeoForge 21.1.248 + Create 6.0.10, in a fresh generated test world. |
| Glass and solid three-floor travel | Actual 42-block enclosed contraptions assemble, follow F3→F2→F1 FIFO requests, coalesce duplicates, execute normal states and align at exact stops. |
| Riders | Native Create standing-rider collision probes with ArmorStand entities pass in both directions. This is server-side rider physics, not a graphical human-player play session. |
| Discovery/configuration | Actual physical positions/order, labels, enabled flags, saved marker NBT, duplicate-ID rejection, automatic additions/removals, negative-Y gaps, wrong orientation, isolated neighboring shafts and 32-stop limit pass. |
| Doors/calls | Alternating front/rear landings, missing upper half, absent-cabin lock, saved-open landing recovery, blocked doorway/request retention, configured delays and amber request state pass. |
| Fault/recovery | Removing a rail during motion stops at the last valid position, without a destination teleport; repair/retry continues. Maintenance return reaches disabled home, and Retry cannot interrupt normal travel. |
| Save/rebind | Serializing/replacing the controller during motion preserves identity/name, exact offset, state, target, configuration and queue; the existing native cabin rebinds and finishes its queued trip. |
| Generic decoration | Ordinary quartz/glass/stair decoration captures and restores; unsupported chest block entities reject assembly with a meaningful error. |
| Payload/menu protocol | Real action/snapshot codecs round-trip; opening data decodes; server edits persist; disabled requests, remote edits and passenger configuration edits are rejected. |
| Native `elevatorModelCheck` | 67 models / 5,976 native baked quads; 32 moving boxes / 768 emitted vertices; four orientations, textures, dye channels, glass AO, UVs and 24 distinct recipes pass. |
| Native CLIENT datagen validation | Actual production screen factory registers; Create schedule/brass texture resources are present; inventory mixins load; Station Utilities immediately follows Vehicle Kits; Flywheel registration links; 3,072 block tint and 3,072 flat-icon tint combinations pass. No rendered client window was used. |
| Existing escalator checks | Native model bake: 148 models / 57,528 quads; 320 moving configurations / 107,520 vertices; fixed flat ends, clean landings and 400 tread/collision combinations pass. Existing escalator/travelator QuickBuild NBT parses. |
| Escalator sound/performance/incline | Sound event/resource/range checks pass; 576 collision unions and 2,304 surface configurations pass; CPU/allocation benchmark runs; 320 incline volumes and 3,840 native selection ray casts pass. No graphical FPS measurement. |
| Release archive/resource audit | All 196 compiled main classes match release bytes. 895 packaged models and 765 distinct direct local model references are validated. ZIP/JAR CRC and duplicate-entry checks pass. No test classes or schematic test files enter the mod JAR. |
| Preservation audit | All 3,750 baseline source files remain present; 25 existing files changed intentionally, including the validation entry point. All unrelated production class bytes and unrelated packaged resource bytes match the prior JAR. Old language keys/tool tags remain. The obsolete anonymous `ElevatorMenu$1` helper is replaced by the new menu implementation. |

The escalator checks ran on the same final escalator implementation; a subsequent native run verified the final elevator placement/maintenance changes. The release audit confirms escalator and other unrelated production files remained unchanged.

Build/test commands for a normal Java 21 environment:

```sh
./gradlew build
./gradlew -PwithoutRailways runGameTestServer
./gradlew -PwithoutRailways -PinventoryValidation elevatorModelCheck runInventoryData
./gradlew -PwithoutRailways escalatorModelCheck escalatorSoundCheck escalatorPerformanceCheck escalatorInclineCheck
```

`-PwithoutRailways` excludes the optional Steam ’n’ Rails runtime and its two optional cable-track GameTests; it does not remove the existing compile-only API dependency or production integrations. Move aside the generated `run/world` between full GameTest runs: existing train tests retain train/station world data across runs. Never overwrite a player's world for testing.

This environment needed a private headless asset setup for server/client-datagen launch and a private NeoForm runtime workaround because its sandbox does not expose the Java executable through `ProcessHandle.Info.command()`. These affect the build environment only and are not changes bundled into the mod or source. Standard Gradle/JDK environments use the supplied wrapper and normal dependency download process.

## Limitations and required in-game follow-up

- **Graphical testing remains required:** controller layout/input at different GUI scales, visible cabin/door animation, lighting/shadows, Complementary Reimagined and hardware FPS were not certified. Native client datagen verifies linking/registration/resources; it does not render the screen or ride with a human player.
- **Actual multiplayer clients:** the production packet codecs/menu authority and server state are validated, but multi-client latency, disconnect/reconnect while riding and live packet transport need a play test.
- **Chunk stress:** serialized motion/rebind and loaded-path safeguards are tested. Repeated real world/chunk unload, server restart and players traveling across chunk boundaries should be stress-tested. There is no automatic chunk force-loading.
- **Cabin bounds:** rectangular dedicated floor only; maximum 512 captured blocks, eight-block horizontal reach from the anchor, six rows above the floor. Only face-connected blocks within that rectangle are captured. Movement reserves the full rectangular volume conservatively.
- **Block entities:** non-elevator block entities and immovable blocks are explicitly unsupported. Ordinary block decoration and the elevator kit are supported.
- **Guide network:** one vertical column starts above the lowest drive. Stops below the drive, alternative mount geometries, branching rails and horizontal movement are not implemented.
- **Capacity:** 32 stops / 256 vertical blocks, not a three-floor hardcode. World build height and cabin roof clearance may reduce the usable range.
- **Existing integrations:** unchanged Steam ’n’ Rails / CRN production code was compiled/preserved; their optional runtime behavior was not newly exercised in this release's base test launch.
- **Future features:** shared-bank calls currently summon all explicitly linked elevators. Intelligent dispatch, rotational power, alternate elevator shapes and illuminated destination display blocks remain future work. Existing redstone call input and arrival/opening sounds are retained.

These are functional limits or unperformed tests, not substitutes for implemented travel. The three-floor QuickBuilds run the actual assembled-cabin, discovery, request and automatic-door implementation in native server tests.

## Files and integrity

- `CREATE Transit Alpha 0.1.0.jar` — compiled mod; install this file, replacing the previous version.
- `CREATE-Transit-Alpha-0.1.0-Source.zip` — complete source, wrapper, assets, tools, all existing schematics, new QuickBuilds and release documentation. Generated worlds, caches and private toolchains are excluded.
- `CREATE-Transit-Elevator2-QuickBuilds.zip` — the two new NBT/JSON demonstrations plus building guide.
- `CREATE-Transit-Elevator2-Building-Guide.md` — construction, controller, linking, doors and troubleshooting.
- `CREATE-Transit-Elevator2-Implementation-Report.md` — this report.

JAR size: **1747800 bytes**.

JAR SHA-256:

```text
79b3ffb207a0d63e9dc61f063cc139c1741e50d01e1d2ce452161761fd2069c1
```
