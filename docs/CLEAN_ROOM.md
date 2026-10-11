# Clean-room policy

The point of gielinorcore is to get the *capabilities* of an Elorin-style
server (fast content iteration, cross-revision assets, custom bosses and
interfaces) without inheriting its lineage of leaked client and server code.

## Never committed

- Any game cache, in whole or part (`.dat2`, `.idx*`, extracted groups, models, sprites, maps).
- Map region keys.
- Decompiled, deobfuscated or refactored client or server code, or code derived by reading it.
- Opcode tables, packet layouts or config formats copied from such code.

## Allowed sources

- Publicly written format specifications and community wiki pages describing the
  on-disk and on-wire formats in prose.
- Observing the behaviour of a running client as a black box (network captures,
  cache files on your own disk) and writing your own description of it.
- Your own assets: meshes, textures, animations, maps and definitions you created.

## Working rule

Write the format description in prose first (in `docs/formats/`), then implement
from the prose. A code comment that cites a source should cite a spec page, never
a class name from another codebase.

## Data at runtime

Jagex data is treated as **user-supplied input**: the server opens whatever cache
sits in `data/cache/` and the overlay compiler writes deltas next to it. This is
the same stance OpenRS2 and rsmod take, and it is what keeps the repository
distributable.
