package com.itmecasey.barelycrafting.script;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * KubeJS support without a compile-time dependency on any KubeJS version: scripts reach the {@link BarelyCraftingApi} through
 * KubeJS's own {@code Java.loadClass}. When KubeJS is installed, a small startup script is written once to
 * {@code kubejs/startup_scripts/barelycrafting.js}; it puts the API in {@code global.BarelyCrafting}. An existing file is never
 * overwritten. KubeJS is optional; without it nothing happens.
 */
public final class KubeJsBridge {
    private static final Logger LOG = Logger.getLogger("barelycrafting");

    static final String SCRIPT = "// Written by Barely Crafting. An existing file is never overwritten.\n"
            + "// Use global.BarelyCrafting from server scripts (events, station reads, recipe rules).\n"
            + "global.BarelyCrafting = Java.loadClass('com.itmecasey.barelycrafting.script.Bridges').API\n";

    private KubeJsBridge() {
    }

    /** True when a KubeJS class is visible. */
    public static boolean present() {
        for (String name : new String[] {"dev.latvian.mods.kubejs.KubeJS", "dev.latvian.mods.kubejs.KubeJSPlugin"}) {
            try {
                Class.forName(name, false, KubeJsBridge.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                // try the next name
            }
        }
        return false;
    }

    /** Writes the helper script into the game folder's kubejs when KubeJS is installed; returns whether KubeJS was found. */
    public static boolean install(Path gameDir) {
        if (gameDir == null || !present()) {
            return false;
        }
        return writeHelper(gameDir);
    }

    public static boolean writeHelper(Path gameDir) {
        Path file = gameDir.resolve("kubejs").resolve("startup_scripts").resolve("barelycrafting.js");
        try {
            if (Files.exists(file) && !Files.isRegularFile(file)) {
                return false;
            }
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, SCRIPT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                LOG.info("Barely Crafting: wrote " + file + " so KubeJS scripts get the BarelyCrafting global");
            }
        } catch (FileAlreadyExistsException e) {
            return Files.isRegularFile(file);
        } catch (IOException e) {
            LOG.warning("Barely Crafting: could not write the KubeJS helper script: " + e);
            return false;
        }
        return true;
    }
}
