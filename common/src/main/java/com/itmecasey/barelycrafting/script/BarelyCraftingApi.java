package com.itmecasey.barelycrafting.script;

import com.itmecasey.barelycrafting.Settings;
import java.util.List;
import java.util.function.Consumer;

/**
 * The scripting surface, exposed to ModJS scripts as the global {@code BarelyCrafting}. Everything uses plain strings and numbers.
 * Server side only: the crafting rules run on the server.
 *
 * <p>Example: {@code BarelyCrafting.onBeforeCraft(e => { if (e.result() == "minecraft:tnt") e.cancel(); })}.
 */
public final class BarelyCraftingApi {
    /** Runs before each craft and may cancel it (nothing is made, nothing is used). */
    public void onBeforeCraft(Consumer<StationEvent> listener) {
        ScriptEvents.on(ScriptEvents.BEFORE_CRAFT, listener);
    }

    /** Runs after each craft. */
    public void onCrafted(Consumer<StationEvent> listener) {
        ScriptEvents.on(ScriptEvents.CRAFTED, listener);
    }

    /** Runs when a player presses a button of the screen; may cancel it. {@code e.button()} names it. */
    public void onButton(Consumer<StationEvent> listener) {
        ScriptEvents.on(ScriptEvents.BUTTON, listener);
    }

    /** Drops every listener and recipe rule scripts registered; call it first in a server script so a reload does not double them. */
    public void reset() {
        ScriptEvents.clear();
        ScriptHooks.clear();
    }

    /** The station no longer crafts this recipe (id such as minecraft:tnt). */
    public void denyRecipe(String recipeId) {
        ScriptHooks.denyRecipe(recipeId);
    }

    /** The station no longer crafts anything that makes this item. */
    public void denyResult(String itemId) {
        ScriptHooks.denyResult(itemId);
    }

    /** The station crafts only these recipes. */
    public void allowOnly(List<String> recipeIds) {
        ScriptHooks.allowOnly(recipeIds);
    }

    /** Reads one setting of barelycrafting.properties (for example craft.max_per_click) as text; "" for an unknown key. */
    public String setting(String key) {
        try {
            return Settings.getString(key);
        } catch (RuntimeException e) {
            return "";
        }
    }
}
