# data/

Runtime-only directory for data that gielinorcore does **not** ship:

- `data/cache/` – the player's own game cache (`main_file_cache.dat2`, `main_file_cache.idx*`).
- `data/keys.json` – map region keys, if the targeted revision needs them.
- `data/packs/` – compiled content-pack overlays produced by `tools pack`.

Everything in here except this file is gitignored. See `docs/CLEAN_ROOM.md`.
