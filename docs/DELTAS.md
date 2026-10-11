# Implementation deltas

Ordered work items. Each is small enough to be one PR and names the module and
the test that proves it. Items inside a group are in order; groups C and P can
proceed in parallel.

## C: cache (unblocks everything)

| Id | Delta | Done when |
| --- | --- | --- |
| C1 | `DiskStore.write`: sector allocation, chain reuse, index update | round-trip test writes a 3-sector group and reads it back; `inspect` on a real cache still matches |
| C2 | `Container`: BZIP2 (prepend the `BZh1` header the format omits) and LZMA; XTEA-encrypted payload option | decode every group of a user cache without error |
| C3 | `ReferenceTable.encode` + CRC/version maintenance so a modified group gets a new version and table checksum | decode(encode(t)) == t; client accepts a patched group |
| C4 | Revision module `cache-r667` (or your target): `ConfigCodec` for items, npcs, objects, sequences, interfaces, varbits | decode all definitions in a user cache, re-encode, byte-identical |
| C5 | Model codec and map region codec for that revision | decode/encode round trip on every model and map group |
| C6 | Name hashing so packs can reference groups by name | lookup by name equals lookup by id |

## N: content packs

| Id | Delta | Done when |
| --- | --- | --- |
| N1 | `OverlayCompiler` for definitions: decode base, merge override, encode, write to delta store; `tools pack` | compiling `packs/example-pack` yields a delta with exactly one changed item group |
| N2 | Asset import: OBJ (and glTF later) -> `Model`, PNG -> sprite/texture; placed in the pack's id range | an item wearing a Blender mesh renders in the client |
| N3 | Animation import: skeleton + keyframes from glTF -> sequence/frame groups | a custom NPC plays a custom idle |
| N4 | Map chunk deltas: object placements and tile overrides per region | a pack can add a building to an existing region |
| N5 | Pack validation in `tools verify`: id range use, dangling references, revision mismatch | CI runs verify on all `packs/*` |
| N6 | Persist delta stores (`data/packs/<id>/`) and load through `LayeredStore` at boot | server boots with base + delta and serves patched groups over Js5 |

## T: transcode

| Id | Delta | Done when |
| --- | --- | --- |
| T1 | Model codecs for a second revision family (OSRS) and `ModelRules` for the pair | an OSRS mesh appears correctly in a 667 client |
| T2 | Animation transcoding: skeleton and frame formats, skin id remap | an OSRS boss animation plays on its transcoded model |
| T3 | Texture and sprite transcoding | textured OSRS models render without pink/black faces |
| T4 | Map transcoding with underlay/overlay id remap tables | an OSRS region loads in a 667 world |
| T5 | `tools transcode` batch mode driven by a manifest (ids, source, destination) | one command ports a boss (models, anims, defs) |

## P: protocol + server

| Id | Delta | Done when |
| --- | --- | --- |
| P1 | `protocol-rNNN`: incoming and outgoing registries with codecs for the ~30 packets needed to stand in the world | registry size test; fuzz test on decoders |
| P2 | Login: handshake, RSA/ISAAC setup, credentials, response codes, session hand-off to `World` | a client reaches the lobby/world |
| P3 | Game pipeline: framed decoder -> message -> `GameQueue`; outbound encoder with ISAAC | `::home` from the example plugin moves the player |
| P4 | Js5 service: serve groups from `LayeredStore` with priority queues | client downloads a patched cache from the server |
| P5 | World list service | client shows the world |

## G: game

| Id | Delta | Done when |
| --- | --- | --- |
| G1 | Collision map from map regions; region loading around players | pathfinder cannot walk through walls |
| G2 | Movement: path request, step queue, run energy | smooth walking in client |
| G3 | Entity sync: player and NPC update blocks (appearance, animation, chat, hits) | two clients see each other |
| G4 | Inventory, equipment, skills, varps/varbits | bank and equip an item |
| G5 | Combat core: hit queue, formulas as data, death/respawn, drop tables | kill an NPC, receive drops |
| G6 | Interfaces: open/close, buttons, component state | an interface from a pack works |
| G7 | Persistence: player save/load (JSON first) | logout/login keeps state |

## S: script

| Id | Delta | Done when |
| --- | --- | --- |
| S1 | Extend `ScriptContext`: object options, buttons, timers, areas, NPC spawns | example plugin spawns an NPC and reacts to a button |
| S2 | Hot reload: classloader swap and handler re-registration on file change | edit a plugin, see it without restart |
| S3 | Dialogue DSL and queued player scripts (coroutines) | multi-step NPC conversation |
| S4 | Boss DSL: phases, mechanics as composable behaviours, instance support | a custom boss with two phases |
| S5 | Pack + plugin pairing: a plugin declares the pack ids it needs | server refuses to load a plugin whose pack is missing |

## X: cross-cutting

| Id | Delta | Done when |
| --- | --- | --- |
| X1 | CI: build, tests, `verify` on packs (present) | green on push |
| X2 | `docs/formats/`: prose specs for every format implemented, cited from comments | each codec file links a spec |
| X3 | Client decision: ship nothing, document a supported client version, or start a clean-room renderer as a separate repo | decision recorded in ARCHITECTURE.md |

## Suggested first milestone

C1 -> C2 -> C4 -> N1 -> P1 -> P2 -> P3 -> G2 -> G3. That is the shortest path to
"two players stand in a world and see an item renamed by a content pack", which
exercises every module once.
