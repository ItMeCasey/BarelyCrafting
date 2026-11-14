package com.itmecasey.barelycrafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelycrafting.script.Bridges;
import com.itmecasey.barelycrafting.script.KubeJsBridge;
import com.itmecasey.barelycrafting.script.ScriptEvents;
import com.itmecasey.barelycrafting.script.ScriptHooks;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** The KubeJS helper script, and that its global is the same API object ModJS gets. */
class KubeJsBridgeTest {
    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        ScriptEvents.clear();
        ScriptHooks.clear();
    }

    @Test
    void helperScriptPutsTheSharedApiInTheGlobal() throws Exception {
        assertTrue(KubeJsBridge.writeHelper(dir));
        String script = Files.readString(dir.resolve("kubejs/startup_scripts/barelycrafting.js"));
        assertTrue(script.contains("global.BarelyCrafting = Java.loadClass('com.itmecasey.barelycrafting.script.Bridges').API"));
        assertTrue(Bridges.class.getName().equals("com.itmecasey.barelycrafting.script.Bridges"));
    }

    @Test
    void theApiObjectKubeJsLoadsWorksFromJavaScript() {
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            ScriptableObject.putProperty(scope, "api", Context.javaToJS(Bridges.API, scope));
            cx.evaluateString(scope, "api.reset(); api.denyResult('minecraft:tnt');", "kube.js", 1, null);
            assertFalse(ScriptHooks.allowed("minecraft:tnt", "minecraft:tnt"));
            cx.evaluateString(scope, "api.reset();", "kube.js", 1, null);
            assertTrue(ScriptHooks.allowed("minecraft:tnt", "minecraft:tnt"));
        } finally {
            Context.exit();
        }
        assertSame(Bridges.API, Bridges.API);
    }

    @Test
    void anExistingHelperIsKept() throws Exception {
        Path file = dir.resolve("kubejs/startup_scripts/barelycrafting.js");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "// custom");
        assertTrue(KubeJsBridge.writeHelper(dir));
        assertEquals("// custom", Files.readString(file));
    }

    @Test
    void aBlockedPathReportsFailure() throws Exception {
        Files.writeString(dir.resolve("kubejs"), "a file blocks the folder");
        assertFalse(KubeJsBridge.writeHelper(dir));
    }

    @Test
    void withoutKubeJsNothingIsCreated() {
        assertFalse(KubeJsBridge.install(dir));
        assertFalse(Files.exists(dir.resolve("kubejs")));
    }
}
