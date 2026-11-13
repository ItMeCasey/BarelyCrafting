package com.itmecasey.barelycrafting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GridMathTest {
    private static String[] grid(String s) {
        String[] cells = new String[9];
        for (int i = 0; i < 9; i++) {
            char c = s.charAt(i);
            cells[i] = c == '.' ? null : String.valueOf(c);
        }
        return cells;
    }

    @Test
    void rotateTurnsClockwise() {
        String[] g = grid("ABC" + "DEF" + "GHI");
        GridMath.rotate(g);
        assertArrayEquals(grid("GDA" + "HEB" + "IFC"), g);
    }

    @Test
    void fourRotationsAreTheIdentity() {
        String[] g = grid("AB." + ".C." + "D.E");
        String[] copy = g.clone();
        for (int i = 0; i < 4; i++) {
            GridMath.rotate(g);
        }
        assertArrayEquals(copy, g);
    }

    @Test
    void rotateMovesAnLShapeAroundTheCorners() {
        String[] g = grid("A.." + "A.." + "AAA");
        GridMath.rotate(g);
        assertArrayEquals(grid("AAA" + "A.." + "A.."), g);
    }

    @Test
    void mirrorFlipsLeftToRight() {
        String[] g = grid("ABC" + "D.F" + "GHI");
        GridMath.mirror(g);
        assertArrayEquals(grid("CBA" + "F.D" + "IHG"), g);
    }

    @Test
    void mirrorTwiceIsTheIdentity() {
        String[] g = grid("AB." + ".C." + "D.E");
        String[] copy = g.clone();
        GridMath.mirror(g);
        GridMath.mirror(g);
        assertArrayEquals(copy, g);
    }

    @Test
    void balanceSpreadsEqualItems() {
        Object[] keys = {"plank", null, "plank", null, "plank", null, null, null, null};
        int[] counts = {10, 0, 1, 0, 1, 0, 0, 0, 0};
        assertTrue(GridMath.balance(keys, counts, 64));
        assertArrayEquals(new int[] {4, 0, 4, 0, 4, 0, 0, 0, 0}, counts);
    }

    @Test
    void balanceGivesRemaindersToTheEarliestSlots() {
        Object[] keys = {"a", "a", "a", null, null, null, null, null, null};
        int[] counts = {1, 1, 1, 0, 0, 0, 0, 0, 0};
        assertFalse(GridMath.balance(keys, counts, 64));
        counts[0] = 5;
        assertTrue(GridMath.balance(keys, counts, 64));
        assertArrayEquals(new int[] {3, 2, 2, 0, 0, 0, 0, 0, 0}, counts);
    }

    @Test
    void balanceKeepsDifferentItemsApart() {
        Object[] keys = {"a", "b", "a", "b", null, null, null, null, null};
        int[] counts = {7, 1, 1, 9, 0, 0, 0, 0, 0};
        GridMath.balance(keys, counts, 64);
        assertArrayEquals(new int[] {4, 5, 4, 5, 0, 0, 0, 0, 0}, counts);
    }

    @Test
    void balanceConservesTheTotalAndRespectsTheStackLimit() {
        Object[] keys = {"a", "a", "a", "a", null, null, null, null, null};
        int[] counts = {16, 16, 16, 15, 0, 0, 0, 0, 0};
        GridMath.balance(keys, counts, 16);
        int sum = 0;
        for (int c : counts) {
            assertTrue(c <= 16);
            sum += c;
        }
        assertEquals(63, sum);
    }

    @Test
    void balanceNeverEmptiesASlot() {
        Object[] keys = {"a", "a", null, null, null, null, null, null, null};
        int[] counts = {1, 1, 0, 0, 0, 0, 0, 0, 0};
        GridMath.balance(keys, counts, 64);
        assertEquals(1, counts[0]);
        assertEquals(1, counts[1]);
    }

    @Test
    void sameLayoutComparesKeys() {
        assertTrue(GridMath.sameLayout(grid("A........"), grid("A........")));
        assertFalse(GridMath.sameLayout(grid("A........"), grid("..A......")));
    }
}
