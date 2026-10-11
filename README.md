# gielinorcore

A clean-room RuneScape-style game server core. The repository holds **code and
content deltas only**: no client, no cache, no keys, no decompiled anything.
Players supply their own cache at runtime; content authors ship packs of
additions that are compiled onto it.

```
./gradlew build                                   # compile + tests
./gradlew :server:run                             # boots on 43594, loads plugins, no cache needed
./gradlew :tools:run --args="verify $PWD/packs/example-pack"
./gradlew :tools:run --args="inspect /path/to/your/cache"
```

## Modules

| Module | Role |
| --- | --- |
| `buffer` | Byte/bit cursors with the game's integer and string encodings |
| `cache` | Store (disk/memory), Js5 containers, reference tables, group files, config/model/map types |
| `content` | Content packs: manifest, partial definitions, overlay compiler, layered store |
| `transcode` | Cross-revision asset conversion (model, animation, texture, map) |
| `protocol` | Revision abstraction, packet registries, login stages |
| `game` | Tick loop, world, entities, event bus |
| `script` | Plugin API (`ServiceLoader`) and the handler DSL content uses |
| `server` | Netty bootstrap and wiring |
| `tools` | CLI: inspect, verify, pack, transcode |
| `plugins/*` | Content plugins (one Gradle module each) |
| `packs/*` | Content packs (data deltas, no code) |

Read `docs/ARCHITECTURE.md` for how the pieces fit, `docs/DELTAS.md` for the
ordered implementation work, and `docs/CLEAN_ROOM.md` for what may and may not
enter this repository.
