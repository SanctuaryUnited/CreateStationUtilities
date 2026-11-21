# Create: Station Utilities

A Create add-on for building modern transit stations: elevators, escalators, fare equipment, platform screen doors, station radio, and Gondola and LRT vehicle kits.

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 or newer |
| Create | 6.0.10 (up to 6.1) |
| Java | 21 |

Install the jar on both the client and the server. The mod ID is `createtransit`.

Optional:
- **Steam 'n' Rails** adds the cable track to the Gondola kit.
- **Create Railways Navigator** integration.

## Features

- **Elevators:** moving cabins on guide rails with floor markers, call buttons, landing doors, banks with dispatch, a cabin touchscreen, chimes, several cabin models (Solid, Rear Panoramic, Scenic, Rounded, Wood) and dyeable LEDs.
- **Escalators and travelators:** glass and solid, with dyeable frames and LEDs and a motor sound.
- **Station equipment:** fare gates, readers and kiosks, and platform screen doors.
- **Radio:** speakers and cabin radios that play live MP3 and Ogg streams. Server operators control who can add stations. See [Server config](#server-config).
- **Vehicle kits:** Gondola and LRT.

## Guides

- [Elevator building guide](docs/ELEVATORS3_SIMPLE_WORKFLOW.md)
- [LRT build guide](LRT_BUILD_GUIDE.md)
- [Platform customization guide](PLATFORM_CUSTOMIZATION_GUIDE.md)

## Server config

`config/createtransit-server.toml`, or a per-world copy in `<world>/serverconfig/`:

- `radio.stationEditors`: who can add or remove radio stations. The default is operators only. Set it to `"BUILDERS"` to let every player do it.
- `radio.allowedHosts`: an optional list of approved stream hosts. Wildcards such as `*.example.net` are allowed.
- `radio.defaultStations`: `"Name|URL"` stations added to each world once.

## Changes

See [CHANGELOG.md](CHANGELOG.md) for release notes and [ROADMAP.md](ROADMAP.md) for planned work. Releases are on the [GitHub releases page](https://github.com/SanctuaryUnited/CreateStationUtilities/releases).

## Building from source

```
./gradlew build                  # compile, run the model and lang checks, and build the jar
./gradlew runGameTestServer      # run the in-game tests
./gradlew runClient              # start a dev client
```

The jar is written to `build/libs/`. Add `-PwithoutRailways` to skip the Steam 'n' Rails tests.

## License

All Rights Reserved.
