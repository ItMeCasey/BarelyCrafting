# Barely Crafting

A crafting station for Minecraft 26.2 and 26.3 on Forge and NeoForge. The 3x3 grid **stays in the block**, it **crafts from the chests and storages next to it**, and a column of small buttons next to the grid clears, rotates, mirrors, balances and refills it.

Part of the Barely series by ItMeCasey Studios. MIT license. Wiki: https://itmecasey.com/wiki/barelycrafting/ (not deployed yet).

## What it does

- **Persistent grid.** Closing the screen leaves the items in the block. They are saved with the world and drop when the block is broken.
- **Neighbor storages.** The six sides are scanned in the order below, north, south, west, east (setting `neighbors.faces`); any block with an item handler counts. Nearest first, slot 0 upward.
- **Rules.** One craft takes one item from every filled grid slot (the grid first). A slot that runs empty is refilled with one matching item from the neighbors; when none has it the slot stays empty and crafting stops. Leftovers (empty buckets, damaged tools) go back into their own slot when it is empty, else to a neighbor, your inventory or the ground. Everything runs on the server and nothing is deleted or created.
- **Shift-click the result** crafts as many as possible into your inventory (stops when the grid stops matching, when the result would become a different item, or when the inventory is full). **Craft All** does the same from a button.
- **Buttons** (plain monochrome icons left of the grid, name tooltips drawn on top of everything): Clear (to neighbors, then you), Rotate, Mirror, Balance, Refill (from you, then neighbors), Craft All. Each can be switched off in the config; the server enforces it.
- **Connected panel:** left of the screen, the real contents of the connected storages (first 27 slots of up to four), read-only.
- **Grid marks:** a small blue mark in grid slots whose item the neighbors hold more of.
- **BEI:** the plus button of a crafting recipe fills the grid (clears it first; ingredients come from your inventory, then the neighbors).

## Config

`config/barelycrafting.properties`, written with defaults on first start. See the wiki page "Config file".

## Versions

One jar per Minecraft version (26.2 and 26.3), each with both loaders. Version scheme `YYYY.M.increment`, starting at 2026.11.1. No Fabric, no older Minecraft.

## Build and test

Each Forge or NeoForge folder is its own Gradle build (Java 25): `forge-26.2`, `neoforge-26.2`, `forge-26.3`, `neoforge-26.3`. Shared pure logic and the config live in `common/`; everything that touches the game lives in `version-26.2` and `version-26.3` (kept in step by `scripts/sync_versions.py`).

```
cd neoforge-26.2 && ./gradlew test                  # unit tests of the shared logic
python scripts/server_check.py all                  # headless server self test on all four layers (36+ checks each)
python scripts/client_tour.py neoforge-26.2         # real-client screenshot tour through the isolated launcher
```

CI builds only the modules a change touches (`.github/scripts/plan_matrix.py`).

## Status

See `PROJECT_STATUS.md`. Honest limits are in the wiki page "Versions and known gaps".
