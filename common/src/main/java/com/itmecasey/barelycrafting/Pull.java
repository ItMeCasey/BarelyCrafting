package com.itmecasey.barelycrafting;

import java.util.List;
import java.util.Map;

/**
 * The order the station takes ingredients in: sources are tried in list order (the grid first, then each neighbor in scan
 * order), and inside a source from slot 0 upward. Nothing is created or lost: every item taken is reported to the caller.
 */
public final class Pull {
    private Pull() {
    }

    /** How many items of {@code key} the sources hold in total. */
    public static <K> long count(K key, List<? extends Slots<K>> sources) {
        long total = 0;
        for (Slots<K> source : sources) {
            for (int i = 0; i < source.size(); i++) {
                if (key.equals(source.key(i))) {
                    total += source.count(i);
                }
            }
        }
        return total;
    }

    /**
     * Takes up to {@code amount} items of {@code key}, source by source. {@code perSource}, when not null, receives the amount
     * taken from each source (same indexes as {@code sources}).
     *
     * @return how many items were taken
     */
    public static <K> int take(K key, int amount, List<? extends Slots<K>> sources, int[] perSource) {
        int left = amount;
        for (int s = 0; s < sources.size() && left > 0; s++) {
            Slots<K> source = sources.get(s);
            for (int i = 0; i < source.size() && left > 0; i++) {
                if (key.equals(source.key(i)) && source.count(i) > 0) {
                    int got = source.extract(i, left);
                    left -= got;
                    if (perSource != null) {
                        perSource[s] += got;
                    }
                }
            }
        }
        return amount - left;
    }

    /**
     * All or nothing: takes every need, or leaves every source untouched when one need cannot be met.
     *
     * @return true when everything was taken
     */
    public static <K> boolean takeAll(Map<K, Integer> needs, List<? extends Slots<K>> sources) {
        for (Map.Entry<K, Integer> e : needs.entrySet()) {
            if (count(e.getKey(), sources) < e.getValue()) {
                return false;
            }
        }
        for (Map.Entry<K, Integer> e : needs.entrySet()) {
            take(e.getKey(), e.getValue(), sources, null);
        }
        return true;
    }
}
