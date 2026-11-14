package com.itmecasey.barelycrafting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every tunable value in the mod, from one list of keys ({@link #KEYS}): the loaders build their {@code .toml} config specs from that
 * list (names, defaults, ranges, descriptions) and push the values into this class when a config loads or reloads. Game code and
 * tests read and override values here; a key nobody set uses its default.
 */
public final class Settings {
    /** The kind of value a key holds. */
    public enum Kind {
        BOOL, INT, CHOICE, FACES
    }

    /**
     * One setting.
     *
     * @param path    dotted key; the part before the first dot is the toml table (for example {@code buttons.clear})
     * @param min     lowest value of an INT key
     * @param max     highest value of an INT key
     * @param choices allowed values of a CHOICE key
     * @param client  true for a client-only value (client config file), false for the common config file
     */
    public record Key(String path, Kind kind, Object def, int min, int max, List<String> choices, boolean client, String comment) {
        public String fallback() {
            return String.valueOf(def);
        }

        /** The comment plus the range or the allowed values, as written above the key in the toml. */
        public String describe() {
            return switch (kind) {
                case INT -> comment + " Range: " + min + " to " + max + ".";
                case CHOICE -> comment + " Allowed: " + String.join(", ", choices) + ".";
                default -> comment;
            };
        }
    }

    /** The six sides a storage can touch the station on. */
    public static final List<String> SIDES = List.of("down", "up", "north", "south", "west", "east");

    public static final List<Key> KEYS = List.of(
            new Key("ui.theme", Kind.CHOICE, "minecraft", 0, 0, List.of("minecraft", "modern"), true,
                    "Look of the screens: minecraft (gray, like the game) or modern (dark with gold, like Show Me What This Is)."),
            bool("buttons.clear", "Show the Clear button (grid items go to neighboring storage first, then your inventory)."),
            bool("buttons.rotate", "Show the Rotate button (turns the grid contents a quarter turn clockwise)."),
            bool("buttons.mirror", "Show the Mirror button (flips the grid contents left to right)."),
            bool("buttons.balance", "Show the Balance button (spreads equal items evenly over their grid slots)."),
            bool("buttons.refill", "Show the Refill button (tops up every filled grid slot to a full stack from your inventory, then neighbors)."),
            bool("buttons.craft_all", "Show the Craft All button (crafts as many as possible into your inventory, like Shift-clicking the result)."),
            bool("neighbors.enabled", "Let the station use items from storages next to it."),
            new Key("neighbors.faces", Kind.FACES, "down,up,north,south,west,east", 0, 0, List.of(), false,
                    "Which sides are scanned, in this order, as a comma-separated list of down, up, north, south, west, east. The first storage that has an item is used first."),
            new Key("neighbors.max", Kind.INT, 6, 1, 6, List.of(), false, "Most neighboring storages scanned."),
            new Key("craft.max_per_click", Kind.INT, 4096, 1, 65536, List.of(), false,
                    "Most crafts one Shift-click or Craft All performs (a safety limit)."));

    private static final Map<String, Key> BY_PATH = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    static {
        for (Key key : KEYS) {
            BY_PATH.put(key.path(), key);
        }
    }

    private static Key bool(String path, String comment) {
        return new Key(path, Kind.BOOL, true, 0, 0, List.of(), false, comment);
    }

    private Settings() {
    }

    /** True when every entry of a comma-separated side list is a side name (an empty list is allowed: no side is scanned). */
    public static boolean validFaces(String text) {
        for (String part : text.split(",")) {
            String side = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (!side.isEmpty() && !SIDES.contains(side)) {
                return false;
            }
        }
        return true;
    }

    /** The key with this path, or null. */
    public static Key key(String path) {
        return BY_PATH.get(path);
    }

    /** Restores every default. */
    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Key k = BY_PATH.get(key);
        if (k == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, k.fallback());
    }

    /** Sets one value (a loaded config pushes its values through here; tests use it too). */
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
            return (int) Math.round(Double.parseDouble(BY_PATH.get(key).fallback()));
        }
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }
}
