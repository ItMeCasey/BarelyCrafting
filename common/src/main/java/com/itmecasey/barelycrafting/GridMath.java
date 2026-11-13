package com.itmecasey.barelycrafting;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure arrangement logic for the 3x3 grid, written on plain arrays so it can be tested without the game. Slot {@code row * 3 + col}
 * counts from the top left.
 */
public final class GridMath {
    public static final int SIZE = 3;
    public static final int CELLS = SIZE * SIZE;

    private GridMath() {
    }

    /** Turns the grid a quarter turn clockwise: the left column becomes the top row. */
    public static <T> void rotate(T[] cells) {
        T[] old = cells.clone();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                cells[row * SIZE + col] = old[(SIZE - 1 - col) * SIZE + row];
            }
        }
    }

    /** Flips the grid left to right. */
    public static <T> void mirror(T[] cells) {
        T[] old = cells.clone();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                cells[row * SIZE + col] = old[row * SIZE + (SIZE - 1 - col)];
            }
        }
    }

    /**
     * Spreads equal items evenly over the slots that hold them. {@code keys[i]} is {@code null} for an empty slot; two slots are
     * "equal" when their keys are. Remainders go to the earliest slots. A slot never ends with more than {@code maxStack} items
     * (a group never holds more than its slots already did, so that cannot cut anything off for valid input).
     *
     * @return true when any count changed
     */
    public static boolean balance(Object[] keys, int[] counts, int maxStack) {
        Map<Object, int[]> groups = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && counts[i] > 0) {
                int[] g = groups.computeIfAbsent(keys[i], k -> new int[2]);
                g[0] += counts[i];
                g[1]++;
            }
        }
        boolean changed = false;
        Map<Object, Integer> seen = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == null || counts[i] <= 0) {
                continue;
            }
            int[] g = groups.get(keys[i]);
            int index = seen.merge(keys[i], 1, Integer::sum) - 1;
            int share = g[0] / g[1] + (index < g[0] % g[1] ? 1 : 0);
            share = Math.min(share, Math.max(1, maxStack));
            if (share != counts[i]) {
                counts[i] = share;
                changed = true;
            }
        }
        return changed;
    }

    /** True when two arrays hold the same keys in the same slots (null counts as empty). */
    public static boolean sameLayout(Object[] a, Object[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!Objects.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }
}
