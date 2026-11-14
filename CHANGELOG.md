# Changelog

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
