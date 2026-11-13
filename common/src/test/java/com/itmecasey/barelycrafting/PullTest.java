package com.itmecasey.barelycrafting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PullTest {
    /** A plain array inventory: keys and counts. */
    private static final class Inv implements Slots<String> {
        final String[] keys;
        final int[] counts;

        Inv(String... spec) {
            keys = new String[spec.length];
            counts = new int[spec.length];
            for (int i = 0; i < spec.length; i++) {
                if (spec[i] != null) {
                    String[] p = spec[i].split(":");
                    keys[i] = p[0];
                    counts[i] = Integer.parseInt(p[1]);
                }
            }
        }

        @Override
        public int size() {
            return keys.length;
        }

        @Override
        public String key(int slot) {
            return counts[slot] > 0 ? keys[slot] : null;
        }

        @Override
        public int count(int slot) {
            return counts[slot];
        }

        @Override
        public int extract(int slot, int amount) {
            int took = Math.min(amount, counts[slot]);
            counts[slot] -= took;
            return took;
        }

        int total(String key) {
            int t = 0;
            for (int i = 0; i < keys.length; i++) {
                if (key.equals(keys[i])) {
                    t += counts[i];
                }
            }
            return t;
        }
    }

    @Test
    void countAddsEverySource() {
        Inv grid = new Inv("plank:2", null);
        Inv chest = new Inv("plank:30", "stick:4", "plank:5");
        assertEquals(37, Pull.count("plank", List.of(grid, chest)));
        assertEquals(4, Pull.count("stick", List.of(grid, chest)));
        assertEquals(0, Pull.count("gold", List.of(grid, chest)));
    }

    @Test
    void takesFromTheGridBeforeTheNeighbors() {
        Inv grid = new Inv("plank:3");
        Inv chest = new Inv("plank:64");
        int[] per = new int[2];
        assertEquals(5, Pull.take("plank", 5, List.of(grid, chest), per));
        assertEquals(0, grid.total("plank"));
        assertEquals(62, chest.total("plank"));
        assertArrayEquals(new int[] {3, 2}, per);
    }

    @Test
    void takesFromTheFirstNeighborBeforeTheSecond() {
        Inv grid = new Inv((String) null);
        Inv first = new Inv("iron:2");
        Inv second = new Inv("iron:10");
        int[] per = new int[3];
        assertEquals(6, Pull.take("iron", 6, List.of(grid, first, second), per));
        assertEquals(0, first.total("iron"));
        assertEquals(6, second.total("iron"));
        assertArrayEquals(new int[] {0, 2, 4}, per);
    }

    @Test
    void takeWorksSlotByAscendingSlot() {
        Inv chest = new Inv("plank:1", "plank:1", "plank:60");
        Pull.take("plank", 2, List.of(chest), null);
        assertEquals(0, chest.counts[0]);
        assertEquals(0, chest.counts[1]);
        assertEquals(60, chest.counts[2]);
    }

    @Test
    void takeReportsAShortfallAndTakesWhatThereIs() {
        Inv chest = new Inv("plank:3");
        assertEquals(3, Pull.take("plank", 10, List.of(chest), null));
        assertEquals(0, chest.total("plank"));
    }

    @Test
    void takeAllIsAllOrNothing() {
        Inv grid = new Inv("plank:1");
        Inv chest = new Inv("plank:3", "stick:1");
        Map<String, Integer> needs = new LinkedHashMap<>();
        needs.put("plank", 4);
        needs.put("stick", 2);
        assertFalse(Pull.takeAll(needs, List.of(grid, chest)));
        assertEquals(1, grid.total("plank"));
        assertEquals(3, chest.total("plank"));
        assertEquals(1, chest.total("stick"));
        needs.put("stick", 1);
        assertTrue(Pull.takeAll(needs, List.of(grid, chest)));
        assertEquals(0, grid.total("plank"));
        assertEquals(0, chest.total("plank"));
        assertEquals(0, chest.total("stick"));
    }

    @Test
    void nothingIsCreatedOrLost() {
        Inv grid = new Inv("a:2", "b:1");
        Inv chest = new Inv("a:7", "b:9", "a:4");
        int before = grid.total("a") + chest.total("a");
        int took = Pull.take("a", 9, List.of(grid, chest), null);
        assertEquals(before - took, grid.total("a") + chest.total("a"));
    }
}
