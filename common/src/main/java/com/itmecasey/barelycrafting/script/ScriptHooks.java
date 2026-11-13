package com.itmecasey.barelycrafting.script;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which recipes the station accepts. By default every crafting recipe is accepted (the station follows the game's own recipes, so
 * recipes added by datapacks or ModJS work without anything here). Scripts can take recipes away, never add new rules.
 */
public final class ScriptHooks {
    private static final Set<String> DENIED_RECIPES = ConcurrentHashMap.newKeySet();
    private static final Set<String> DENIED_RESULTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> ONLY_RECIPES = ConcurrentHashMap.newKeySet();
    private static volatile boolean restricted;

    private ScriptHooks() {
    }

    public static void denyRecipe(String id) {
        DENIED_RECIPES.add(id);
    }

    public static void denyResult(String item) {
        DENIED_RESULTS.add(item);
    }

    /** Only these recipes work in the station (an empty list means no restriction). */
    public static void allowOnly(Iterable<String> ids) {
        ONLY_RECIPES.clear();
        for (String id : ids) {
            ONLY_RECIPES.add(id);
        }
        restricted = !ONLY_RECIPES.isEmpty();
    }

    /** Whether the station may craft this recipe (its id) into this item (its id). */
    public static boolean allowed(String recipeId, String resultItem) {
        if (restricted && !ONLY_RECIPES.contains(recipeId)) {
            return false;
        }
        return !DENIED_RECIPES.contains(recipeId) && !DENIED_RESULTS.contains(resultItem);
    }

    public static void clear() {
        DENIED_RECIPES.clear();
        DENIED_RESULTS.clear();
        ONLY_RECIPES.clear();
        restricted = false;
    }
}
