# Barely Crafting: plan and decisions

**Name and id:** Barely Crafting, `barelycrafting`, slug `barelycrafting`, repo `ItMeCasey/BarelyCrafting`.
**One sentence:** A crafting station with a persistent 3x3 grid that crafts from the chests next to it, plus grid tweak buttons.
**Targets:** Forge and NeoForge for Minecraft 26.2 and 26.3 only (four modules). **Side:** both. **License:** MIT.

## Design decisions (show these to Casey)

1. **Pattern-keeping refill, not a ghost grid.** The grid holds real items. A craft uses one item from every filled slot (grid first); a slot that runs empty is refilled with one item from the neighbors. So four planks in the grid can craft a whole chest. When no neighbor has the item the slot stays empty and crafting stops.
2. **Neighbors.** All six sides, in the order below, above, north, south, west, east (`neighbors.faces`), at most 6 storages (`neighbors.max`), any block with an item handler, slot 0 upward, first storage first. `neighbors.enabled=false` turns it off. The station never pulls on its own, only for a craft or a button.
3. **Leftovers** go back into the slot they came from when it is empty (vanilla behavior), else a neighbor, then the player, then the ground.
4. **Shift-click / Craft All** stop when the grid stops matching, when the result would become a different item (like three leftover cobblestone turning into slabs), when the inventory is full, or at `craft.max_per_click`.
5. **Clear** sends the items to neighbors first, then the player, then the ground. **Refill** tops filled slots up to a full stack from the player first, then neighbors.
6. **Buttons** are configurable per button; the server refuses a disabled button, the client hides it.
7. **Connected panel** with the real contents of the connected storages (read-only view slots, first 27 slots of up to four storages), because a summary was not enough.
8. **Persistence** writes all nine slots (empty ones too), because the stock `SimpleContainer.storeAsItemList` skips empty slots and would shuffle the pattern.
9. **Result clicks** are handled in the menu's `clicked` (server only), not in a vanilla result slot, so craft-max and refills stay in one code path.

## Compatibility standard

- **BEI:** plus button done (`compat/CraftingBeiPlugin`), optional, loads only with BEI.
- **JEI, REI, EMI:** the item and its recipe appear like any vanilla recipe; a plus-button transfer handler is not written (listed in the known gaps).
- **SMWTI / Jade:** no station lines yet (known gap). The station shows nothing worth a line beyond its screen.

## Testing

Unit tests of the pure logic (`GridMath`, `Pull`, `CraftMath`, `Settings`), a headless server self test on every layer (`scripts/server_check.py`), and a real-client tour on one Forge and one NeoForge layer through the isolated launcher (`scripts/client_tour.py`).

## House rule: no hints

No hint, tutorial or status-explainer text anywhere. Buttons get only their plain name as tooltip. Wiki has reference pages only.
