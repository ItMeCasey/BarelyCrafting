package com.itmecasey.barelycrafting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToLongFunction;

/** Counting helpers for the crafts the station can still make. */
public final class CraftMath {
    private CraftMath() {
    }

    /** Ingredients one craft uses: one item per filled grid slot, so equal items in several slots add up. */
    public static <K> Map<K, Integer> perCraft(K[] gridKeys) {
        Map<K, Integer> need = new LinkedHashMap<>();
        for (K key : gridKeys) {
            if (key != null) {
                need.merge(key, 1, Integer::sum);
            }
        }
        return need;
    }

    /**
     * How many more crafts the supply allows: the smallest {@code supply / need} over every ingredient, capped at {@code cap}.
     * Zero when nothing is in the grid.
     */
    public static <K> int crafts(Map<K, Integer> perCraft, ToLongFunction<K> supply, int cap) {
        if (perCraft.isEmpty()) {
            return 0;
        }
        long best = cap;
        for (Map.Entry<K, Integer> e : perCraft.entrySet()) {
            best = Math.min(best, supply.applyAsLong(e.getKey()) / Math.max(1, e.getValue()));
        }
        return (int) Math.max(0, best);
    }
}
