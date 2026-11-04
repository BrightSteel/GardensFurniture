# GardensFurniture

[![Build](https://github.com/BrightSteel/gardens-furniture/actions/workflows/build.yml/badge.svg)](https://github.com/BrightSteel/gardens-furniture/actions/workflows/build.yml)

A configurable framework for persistent, interactive custom blocks and furniture on Paper servers. It powers the furniture and custom blocks on [GardensMC](https://gardensmc.com), and other Gardens plugins build on its API.

<!-- TODO: add a screenshot or GIF of placing and sitting on furniture here -->

## Features

- **Custom blocks and items**, defined in config: furniture with rotation and facing, barrier-based hitboxes, seats and interactions.
- **Persistent storage** of every placed piece, so furniture survives restarts and chunk reloads.
- **A recipe system**, including an importer for existing CustomCrafting recipe files.
- **Painting and dyeing** (`/paint`), structures and world-generation hooks.
- **Bedrock support** through Geyser, so Bedrock players see the same furniture.
- **Protection-aware placing and breaking**, with hooks for WorldGuard, GriefPrevention, Prism, FastAsyncWorldEdit and ModelEngine.

More docs: [docs/](docs/)

## How it's built

- **Paper 26.1 / Java 25**, built with Gradle, paperweight-userdev and Shadow.
- **Jackson and Typesafe Config** for definitions, and **packetevents / ProtocolLib** for packet-level work.
- **Unit tests** for rotation, barriers, recipe matching, the CustomCrafting importer and the store, run in GitHub Actions.

## Part of the Gardens suite

The Gardens suite is a set of Paper plugins that run [GardensMC](https://gardensmc.com), a survival community with 736 registered players. The plugins share APIs and depend on each other:

| Plugin | Role |
|---|---|
| [GardensPlayers](https://github.com/BrightSteel/GardensPlayers) | Core platform: economy, quests, shops, warps, resource worlds, player data |
| [GardensEnchants](https://github.com/BrightSteel/gardens-enchants) | Custom enchanting system that replaces vanilla enchanting |
| [GardensFurniture](https://github.com/BrightSteel/gardens-furniture) | Framework for persistent, interactive custom blocks and furniture |
| [GardensCrops](https://github.com/BrightSteel/GardensCrops) | Farming: planters, sprinklers, watering cans, harvest golems |

```
GardensPlayers ──► GardensEnchants ──► GardensFurniture
       │                                     ▲
       └──────────► GardensCrops ────────────┘
```

## Building

Requires Java 25.

```
./gradlew build
```

The shaded jar is written to `build/libs/`. Plugins that are not published to a Maven repository (other Gardens plugins and some third-party plugins) are compiled against jars in `dependencies/`, which are not committed. See [dependencies/README.md](dependencies/README.md) for the list.

## License

[MIT](LICENSE)
