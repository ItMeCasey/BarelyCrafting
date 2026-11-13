# Scripting with ModJS

Barely Crafting adds a `BarelyCrafting` global to ModJS server scripts. It exists only when ModJS is installed; without ModJS nothing is loaded and nothing changes. Client scripts do not get the global (the crafting rules run on the server).

The station keeps crafting exactly as it does without scripts. Scripts can listen to what happens, read the station, and take recipes away from it. They cannot change the crafting rules.

## Events

Each `on...` call takes a function that receives one event object.

| Call | When | Can cancel |
|---|---|---|
| `BarelyCrafting.onBeforeCraft(e => ...)` | before each craft (a shift-click or Craft All fires it once per craft) | yes: nothing is made, nothing is used |
| `BarelyCrafting.onCrafted(e => ...)` | after each craft | no |
| `BarelyCrafting.onButton(e => ...)` | when a player presses a button of the screen | yes: the button does nothing |

Event methods:

| Method | Returns |
|---|---|
| `e.name()` | `beforeCraft`, `crafted` or `button` |
| `e.player()` / `e.playerId()` | player name / UUID text |
| `e.result()` | item id of the craft (`minecraft:stick`), empty for a button event |
| `e.count()` | items one craft makes, 0 for a button event |
| `e.button()` | `clear`, `rotate`, `mirror`, `balance`, `refill` or `craft_all`, empty for a craft event |
| `e.cancel()` | stops a `beforeCraft` or `button` event |
| `e.isCancelled()` | whether a listener cancelled it |
| `e.station()` | read-only snapshot of the station (below) |

The station snapshot:

| Method | Returns |
|---|---|
| `dimension()`, `x()`, `y()`, `z()` | where the station is |
| `grid()` | the nine grid slots, row by row, each `"N namespace:item"` or `""` |
| `slot(i)` | one grid slot (0 to 8) |
| `result()` | what the grid crafts now, `"N namespace:item"`, or `""` |
| `storages()` | the connected storages; each has `side()` (down, up, north, south, west, east), `block()` (block id), `used()` and `total()` slots |

## Recipes

The station uses the game's own crafting recipes, so recipes added with ModJS or a datapack work in it without anything else. Scripts can take recipes away:

| Call | Effect |
|---|---|
| `BarelyCrafting.denyRecipe("minecraft:tnt")` | the station no longer crafts this recipe (recipe id) |
| `BarelyCrafting.denyResult("minecraft:bow")` | the station no longer crafts anything that makes this item |
| `BarelyCrafting.allowOnly(["minecraft:stick", "minecraft:torch"])` | the station crafts only these recipes |

A refused recipe shows no result in the station. Crafting tables are not affected.

## Other calls

| Call | Effect |
|---|---|
| `BarelyCrafting.setting("craft.max_per_click")` | reads one value of `barelycrafting.properties` as text (empty for an unknown key); read only |
| `BarelyCrafting.reset()` | removes every listener and recipe rule scripts registered |

Call `BarelyCrafting.reset()` first in a server script so a script reload replaces the old listeners instead of adding a second copy.

A listener that throws an error is logged as one line and skipped; the craft carries on.

## Example

```js
// server_scripts/station.js
BarelyCrafting.reset();

// No TNT from stations.
BarelyCrafting.denyResult("minecraft:tnt");

// Log every craft.
BarelyCrafting.onCrafted(e => {
    console.log(e.player() + " crafted " + e.count() + " " + e.result() +
        " at " + e.station().x() + " " + e.station().y() + " " + e.station().z());
});

// Disable the Clear button for everyone.
BarelyCrafting.onButton(e => {
    if (e.button() == "clear") e.cancel();
});
```
