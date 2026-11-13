package com.itmecasey.barelycrafting.script;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** The script listeners: {@code beforeCraft}, {@code crafted} and {@code button}. A listener that throws is logged and skipped. */
public final class ScriptEvents {
    public static final String BEFORE_CRAFT = "beforeCraft";
    public static final String CRAFTED = "crafted";
    public static final String BUTTON = "button";

    private static final Logger LOG = Logger.getLogger("barelycrafting");
    private static final Map<String, List<Consumer<StationEvent>>> LISTENERS = new ConcurrentHashMap<>();

    private ScriptEvents() {
    }

    public static void on(String name, Consumer<StationEvent> listener) {
        LISTENERS.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** True when a script listens, so callers can skip building the snapshot. */
    public static boolean has(String name) {
        List<Consumer<StationEvent>> list = LISTENERS.get(name);
        return list != null && !list.isEmpty();
    }

    /** Fires an event and returns true when a listener cancelled it. The snapshot is built only when someone listens. */
    public static boolean fire(String name, Supplier<StationInfo> station, String player, String playerId, String result, int count, String button) {
        List<Consumer<StationEvent>> list = LISTENERS.get(name);
        if (list == null || list.isEmpty()) {
            return false;
        }
        StationInfo info;
        try {
            info = station.get();
        } catch (RuntimeException e) {
            LOG.warning("Barely Crafting: could not build the " + name + " event: " + e);
            return false;
        }
        StationEvent event = new StationEvent(name, info, player, playerId, result, count, button);
        for (Consumer<StationEvent> listener : list) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                LOG.warning("Barely Crafting: a script listener for " + name + " failed: " + e);
            }
        }
        return event.isCancelled();
    }

    public static void clear() {
        LISTENERS.clear();
    }
}
