package com.itmecasey.barelycrafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CraftMathTest {
    @AfterEach
    void reset() {
        Settings.reset();
    }

    @Test
    void perCraftAddsEqualItemsAcrossSlots() {
        String[] g = {"plank", "plank", null, "plank", "plank", null, null, "stick", null};
        Map<String, Integer> need = CraftMath.perCraft(g);
        assertEquals(4, need.get("plank"));
        assertEquals(1, need.get("stick"));
    }

    @Test
    void craftsIsTheTightestIngredient() {
        Map<String, Integer> need = Map.of("plank", 4, "stick", 1);
        long crafts = CraftMath.crafts(need, k -> k.equals("plank") ? 18 : 100, 9999);
        assertEquals(4, crafts);
    }

    @Test
    void craftsIsCappedAndNeverNegative() {
        assertEquals(10, CraftMath.crafts(Map.of("a", 1), k -> 1_000_000L, 10));
        assertEquals(0, CraftMath.crafts(Map.of("a", 2), k -> 1L, 10));
        assertEquals(0, CraftMath.crafts(Map.of(), k -> 5L, 10));
    }

    @Test
    void settingsReadOverridesAndKeepDefaults() throws IOException {
        assertTrue(Settings.getBool("buttons.rotate"));
        Settings.load(new StringReader("buttons.rotate=false\nneighbors.max=3\nunknown.key=1\n"));
        assertEquals(false, Settings.getBool("buttons.rotate"));
        assertEquals(3, Settings.getInt("neighbors.max"));
        assertTrue(Settings.getBool("buttons.clear"));
        assertEquals("down,up,north,south,west,east", Settings.getString("neighbors.faces"));
    }
}
