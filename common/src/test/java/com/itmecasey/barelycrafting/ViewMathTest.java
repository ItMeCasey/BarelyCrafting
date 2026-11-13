package com.itmecasey.barelycrafting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ViewMathTest {
    @Test
    void smallStorageNeverScrolls() {
        assertEquals(0, ViewMath.maxRow(27, 9, 6));
        assertEquals(0, ViewMath.maxRow(54, 9, 6));
        assertEquals(0, ViewMath.maxRow(0, 9, 6));
        assertEquals(0, ViewMath.maxRow(5, 9, 6));
    }

    @Test
    void bigStorageScrollsByWholeRows() {
        assertEquals(1, ViewMath.maxRow(55, 9, 6));
        assertEquals(4, ViewMath.maxRow(90, 9, 6));
        assertEquals(10, ViewMath.maxRow(144, 9, 6));
    }

    @Test
    void clampKeepsTheWindowInside() {
        assertEquals(0, ViewMath.clampRow(-3, 144, 9, 6));
        assertEquals(10, ViewMath.clampRow(99, 144, 9, 6));
        assertEquals(4, ViewMath.clampRow(4, 144, 9, 6));
        assertEquals(0, ViewMath.clampRow(7, 20, 9, 6));
    }
}
