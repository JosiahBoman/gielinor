# Architecture

## The Elorin recipe, and its clean-room equivalent

Pre-EOC servers in Elorin's family add content in three ways:

1. **Hand-written Java** in a shared server base for every boss, skill and interface.
2. **Cache packing**: OSRS models, animations, definitions and map chunks are
   transcoded into a 667/718 cache with one-off community tools, then the cache is
   redistributed with the client.
3. **Client patches** to a deobfuscated client so it accepts the packed formats.

gielinorcore keeps the first two capabilities but moves them into libraries that
work on the player's own cache, and treats the client as out of scope (or a
separate clean-room project).

```
             content packs (packs/*)        plugins (plugins/*)
                     |                              |
   base cache  --> :content OverlayCompiler         |  :script ScriptContext
   (data/cache)      |  uses :transcode             |
                     v                              v
              delta store  ---- LayeredStore ---> :game World <--- :protocol <--- Netty (:server)
                                     ^
                                     |
                              :cache Cache/Store
                                     |
                                  :buffer
```

## Layers

### buffer
`ReadBuffer`/`WriteBuffer` and `BitReader`/`BitWriter`. Everything that touches
bytes goes through here so encodings (smart, big-smart, versioned strings) are
implemented once.

### cache
- `store`: raw (archive, group) -> compressed bytes. `DiskStore` reads the
  sector format; `MemoryStore` backs tests and compiler output.
- `js5`: `Container` (compression + version trailer), `ReferenceTable`
  (archive metadata), `GroupFiles` (file splitting).
- `Cache`: the typed facade. Readers above call `cache.file(archive, group, file)`.
- `config`, `model`, `map`: revision-neutral data classes plus codec interfaces.
  A **revision module** (e.g. `cache-r667`) supplies the codecs; the core never
  hardcodes opcodes.

### content
A pack is a directory of partial definitions and new assets. `OverlayCompiler`
decodes the base definition, merges the override, re-encodes with the revision's
codec and writes only changed groups to a delta `MemoryStore` that is then
persisted. `LayeredStore` makes the server read delta-then-base. Packs declare an
id range so two packs cannot allocate the same ids.

### transcode
`Transcoder` is decode -> neutral model -> encode. Pairs that need semantic fixes
(texture id spaces, priority ranges) go in `ModelRules`. Animation conversion
follows the same pattern with skeletons and frames.

### protocol
`Revision` bundles two `PacketRegistry` instances. Game code sends
`OutgoingMessage` objects; the registry resolves opcode and length. Adding a
revision never touches `game`.

### game
Single game thread. `World.tick()` drains `GameQueue`, ticks entities, then (once
implemented) runs movement, combat and sync. `EventBus` is the only way content
reacts to the world.

### script
`Plugin` is discovered through `ServiceLoader`. `ScriptContext` exposes a small
DSL (`onCommand`, `onNpcOption`, `onItemOption`) and grows with the engine.

### server
Netty bootstrap, service handshake, wiring of cache + world + plugins. Thin by
design.

### tools
Operator CLI. `inspect` and `verify` work today; `pack` and `transcode` land with
the deltas.

## Threading
Netty threads decode packets and post closures to `GameQueue`. Only the game
thread mutates world state. Sync snapshots are built on the game thread and
written from Netty threads.

## Adding a revision
1. `cache-rNNN`: config/model/map codecs implementing the interfaces in `:cache`.
2. `protocol-rNNN`: `Revision` with both registries and the login handshake.
3. Register both in `server` config. Nothing else changes.
