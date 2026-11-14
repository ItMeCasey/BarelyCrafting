# Changelog

## 2026.11.4

- Config: the loader's standard config files replace the hand-written settings file: `config/barelycrafting-common.toml` (server values) and `config/barelycrafting-client.toml` (theme), with comments and ranges, and an in-game config screen on NeoForge. Forge 26.x has no config screen yet.
- Migration: on the first start, when a toml file does not exist yet, every value from `config/barelycrafting.properties` (the file of 2026.11.1 to 2026.11.3) is imported into it and checked, then the old file is moved to `config/barelycrafting.properties.back` with a first line saying where and why it migrated (`.back2`, `.back3` if one exists). A file that cannot be read, or a value that is not valid, uses the default and leaves the old file where it is. Out-of-range numbers are clamped; everything is logged.

## 2026.11.3

- JEI and REI: the plus button of a crafting recipe fills the grid of the station (NeoForge 26.2 and 26.3; JEI and REI have no Forge 26.x build). BEI, JEI and REI share one fill routine.
- Show Me What This Is (all four builds) and Jade (NeoForge): lines for connected storages and the last craft.
- Every craft at the station posts the loader's item-crafted event, so other mods that count crafts see it.
- KubeJS: a startup script gives scripts the `BarelyCrafting` global (the same API as ModJS).
- Editor typings for the global: docs/barelycrafting.d.ts.

## 2026.11.2

- ModJS scripting (optional): a `BarelyCrafting` global for server scripts with `onBeforeCraft` and `onButton` (both cancellable), `onCrafted`, a read-only station snapshot (grid, result, connected storages), recipe rules (`denyRecipe`, `denyResult`, `allowOnly`), `setting` and `reset`. See docs/scripting.md.
- No hint text found in the game: names, values and the storage tab tooltip are the only text.

## 2026.11.1

First version. One jar per Minecraft version (26.2 and 26.3), each for Forge and NeoForge.

- Crafting Station block: a 3x3 grid that keeps its items in the block, a result slot and your inventory.
- Crafts from the storages next to it (all six sides, in a set order); the grid is used first and emptied slots are refilled from the neighbors.
- Shift-click on the result crafts as many as possible; leftovers such as empty buckets are handled without losing anything.
- Six buttons left of the grid: Clear, Rotate, Mirror, Balance, Refill and Craft All, each with a tooltip and a config switch.
- BEI: the plus button of a crafting recipe fills the grid.
