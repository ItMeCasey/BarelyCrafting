package com.itmecasey.barelycrafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelycrafting.script.BarelyCraftingApi;
import com.itmecasey.barelycrafting.script.ModJsBridge;
import com.itmecasey.barelycrafting.script.ScriptEvents;
import com.itmecasey.barelycrafting.script.ScriptHooks;
import com.itmecasey.barelycrafting.script.StationInfo;
import com.itmecasey.barelycrafting.script.StorageInfo;
import com.itmecasey.modjs.script.ExtraBindings;
import com.itmecasey.modjs.script.ScriptType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Runs real JavaScript (Rhino, the engine ModJS uses) against the BarelyCrafting global through the reflective ModJS bridge. */
class ScriptBridgeTest {
    private static final StationInfo STATION = new StationInfo("minecraft:overworld", 1, 64, -3,
            List.of("1 minecraft:oak_planks", "", "", "", "1 minecraft:oak_planks", "", "", "", ""), "4 minecraft:stick",
            List.of(new StorageInfo("down", "minecraft:chest", 3, 27)));

    @AfterEach
    void tearDown() {
        ScriptEvents.clear();
        ScriptHooks.clear();
        ExtraBindings.PROVIDERS.clear();
    }

    Object run(ScriptType type, String js) {
        assertTrue(ModJsBridge.install(new BarelyCraftingApi()));
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            ExtraBindings.PROVIDERS.get(0).install(type, cx, scope);
            return cx.evaluateString(scope, js, "test.js", 1, null);
        } finally {
            Context.exit();
        }
    }

    boolean fireBefore() {
        return ScriptEvents.fire(ScriptEvents.BEFORE_CRAFT, () -> STATION, "alice", "id-1", "minecraft:stick", 4, "");
    }

    @Test
    void beforeCraftCanBeCancelledAndSeesTheStation() {
        run(ScriptType.SERVER, "BarelyCrafting.onBeforeCraft(e => {"
                + " if (e.station().x() == 1 && e.station().slot(0) == '1 minecraft:oak_planks' && e.station().storages().get(0).side() == 'down'"
                + "   && e.station().result() == '4 minecraft:stick' && e.player() == 'alice' && e.count() == 4) e.cancel(); });");
        assertTrue(fireBefore());
    }

    @Test
    void craftedIsNotCancellableAndNothingFiresWithoutListeners() {
        assertFalse(fireBefore());
        run(ScriptType.SERVER, "BarelyCrafting.onCrafted(e => { e.cancel(); java.lang.System.setProperty('bc.crafted', e.result()); });");
        assertFalse(ScriptEvents.fire(ScriptEvents.CRAFTED, () -> STATION, "alice", "id-1", "minecraft:stick", 4, ""));
        assertEquals("minecraft:stick", System.getProperty("bc.crafted"));
    }

    @Test
    void buttonEventNamesTheButton() {
        run(ScriptType.SERVER, "BarelyCrafting.onButton(e => { if (e.button() == 'rotate') e.cancel(); });");
        assertTrue(ScriptEvents.fire(ScriptEvents.BUTTON, () -> STATION, "alice", "id-1", "", 0, "rotate"));
        assertFalse(ScriptEvents.fire(ScriptEvents.BUTTON, () -> STATION, "alice", "id-1", "", 0, "clear"));
    }

    @Test
    void aScriptThatThrowsNeverBreaksTheCraft() {
        run(ScriptType.SERVER, "BarelyCrafting.onBeforeCraft(e => { throw new java.lang.RuntimeException('boom'); });"
                + "BarelyCrafting.onBeforeCraft(e => e.cancel());");
        assertTrue(fireBefore(), "the second listener still runs");
    }

    @Test
    void snapshotFailureIsLoggedNotThrown() {
        run(ScriptType.SERVER, "BarelyCrafting.onBeforeCraft(e => e.cancel());");
        assertFalse(ScriptEvents.fire(ScriptEvents.BEFORE_CRAFT, () -> {
            throw new IllegalStateException("level gone");
        }, "alice", "id-1", "x", 1, ""));
    }

    @Test
    void recipeRulesDenyAndRestrict() {
        assertTrue(ScriptHooks.allowed("minecraft:stick", "minecraft:stick"));
        run(ScriptType.SERVER, "BarelyCrafting.denyRecipe('minecraft:tnt'); BarelyCrafting.denyResult('minecraft:bow');");
        assertFalse(ScriptHooks.allowed("minecraft:tnt", "minecraft:tnt"));
        assertFalse(ScriptHooks.allowed("minecraft:some_bow", "minecraft:bow"));
        assertTrue(ScriptHooks.allowed("minecraft:stick", "minecraft:stick"));
        run(ScriptType.SERVER, "BarelyCrafting.allowOnly(['minecraft:stick']);");
        assertTrue(ScriptHooks.allowed("minecraft:stick", "minecraft:stick"));
        assertFalse(ScriptHooks.allowed("minecraft:torch", "minecraft:torch"));
        run(ScriptType.SERVER, "BarelyCrafting.reset();");
        assertTrue(ScriptHooks.allowed("minecraft:torch", "minecraft:torch"));
    }

    @Test
    void resetDropsOldListenersAfterAReload() {
        run(ScriptType.SERVER, "BarelyCrafting.onBeforeCraft(e => e.cancel()); BarelyCrafting.reset();");
        assertFalse(fireBefore());
    }

    @Test
    void settingsAreReadableAndUnknownKeysAreEmpty() {
        assertEquals("4096", run(ScriptType.SERVER, "String(BarelyCrafting.setting('craft.max_per_click'))").toString());
        assertEquals("", run(ScriptType.SERVER, "String(BarelyCrafting.setting('nope'))").toString());
    }

    @Test
    void clientScriptsGetNoGlobal() {
        assertEquals("undefined", run(ScriptType.CLIENT, "typeof BarelyCrafting").toString());
        assertEquals("object", run(ScriptType.SERVER, "typeof BarelyCrafting").toString());
    }
}
