package com.itmecasey.barelycrafting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Every tunable value in the mod. The file {@code config/barelycrafting.properties} holds one {@code key=value} line per setting;
 * a missing key uses its default, and the file is rewritten with every key and a comment after loading.
 */
public final class Settings {
    private record Entry(String fallback, String comment) {
    }

    private static final Map<String, Entry> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void def(String key, Object fallback, String comment) {
        DEFS.put(key, new Entry(String.valueOf(fallback), comment));
    }

    static {
        def("ui.theme", "minecraft", "Look of the screens: minecraft (gray, like the game) or modern (dark with gold, like Show Me What This Is). Client side.");

        def("buttons.clear", true, "Show the Clear button (grid items go to neighboring storage first, then your inventory).");
        def("buttons.rotate", true, "Show the Rotate button (turns the grid contents a quarter turn clockwise).");
        def("buttons.mirror", true, "Show the Mirror button (flips the grid contents left to right).");
        def("buttons.balance", true, "Show the Balance button (spreads equal items evenly over their grid slots).");
        def("buttons.refill", true, "Show the Refill button (tops up every filled grid slot to a full stack from your inventory, then neighbors).");
        def("buttons.craft_all", true, "Show the Craft All button (crafts as many as possible into your inventory, like Shift-clicking the result).");

        def("neighbors.enabled", true, "Let the station use items from storages next to it.");
        def("neighbors.faces", "down,up,north,south,west,east", "Which sides are scanned, in this order. The first storage that has an item is used first.");
        def("neighbors.max", 6, "Most neighboring storages scanned (1 to 6).");

        def("craft.max_per_click", 4096, "Most crafts one Shift-click or Craft All performs (a safety limit).");
    }

    private Settings() {
    }

    /** Restores every default. */
    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Entry e = DEFS.get(key);
        if (e == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, e.fallback());
    }

    /** Overrides one setting (tests only). */
    public static synchronized void set(String key, Object value) {
        raw(key);
        VALUES.put(key, String.valueOf(value));
    }

    public static String getString(String key) {
        return raw(key).trim();
    }

    public static int getInt(String key) {
        try {
            return (int) Math.round(Double.parseDouble(raw(key).trim()));
        } catch (NumberFormatException ex) {
            return (int) Math.round(Double.parseDouble(DEFS.get(key).fallback()));
        }
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }

    /** Reads overrides from a properties text (also used by tests). */
    public static synchronized void load(Reader reader) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        VALUES.clear();
        for (String key : p.stringPropertyNames()) {
            if (DEFS.containsKey(key)) {
                VALUES.put(key, p.getProperty(key));
            }
        }
    }

    /** Loads the file if there is one, then rewrites it with every key so players can see what they can change. */
    public static synchronized void loadFile(Path file) {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    load(r);
                }
            }
            write(file);
        } catch (IOException | RuntimeException ex) {
            System.err.println("[BarelyCrafting] Could not read " + file + ", using defaults: " + ex);
            VALUES.clear();
        }
    }

    private static void write(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("# Barely Crafting settings. Edit a value and restart the game or server. Delete a line to restore its default.\n");
            w.write("# The server enforces the button and neighbor settings; a client hides the buttons its own file turns off.\n");
            for (Map.Entry<String, Entry> e : DEFS.entrySet()) {
                w.write("\n# " + e.getValue().comment() + "\n");
                w.write(e.getKey() + "=" + VALUES.getOrDefault(e.getKey(), e.getValue().fallback()) + "\n");
            }
        }
    }
}
