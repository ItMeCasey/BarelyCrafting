package com.itmecasey.barelycrafting.compat;

import com.itmecasey.barelycrafting.menu.StationMenu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * The recipe viewers' plus button for the Crafting Station (BEI, JEI and REI share this). The grid is emptied first (its items go to
 * the neighboring storage, then your inventory), then every slot is filled with one ingredient taken from your inventory first and
 * the neighboring storages second. Client side only.
 */
public final class StationTransfer {
    private StationTransfer() {
    }

    /**
     * Fills the grid. {@code cells} has one entry per grid slot (row by row, nine entries); each entry lists the items that fit
     * that slot, empty when the slot stays empty.
     */
    public static boolean fill(StationMenu menu, List<List<ItemStack>> cells) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null) {
            return false;
        }
        int[] raw = new int[StationMenu.GRID];
        for (int cell = 0; cell < StationMenu.GRID && cell < cells.size(); cell++) {
            ItemStack pick = pick(cells.get(cell), minecraft);
            raw[cell] = pick.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(pick.getItem());
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StationMenu.BTN_FILL_START);
        for (int cell = 0; cell < StationMenu.GRID; cell++) {
            if (raw[cell] > 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StationMenu.BTN_FILL + cell * StationMenu.FILL_STRIDE + raw[cell]);
            }
        }
        return true;
    }

    /** Of the acceptable items, one you carry; else the first one. */
    private static ItemStack pick(List<ItemStack> candidates, Minecraft minecraft) {
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack c : candidates) {
            if (c.isEmpty()) {
                continue;
            }
            if (first.isEmpty()) {
                first = c;
            }
            if (minecraft.player.getInventory().contains(s -> ItemStack.isSameItem(s, c))) {
                return c;
            }
        }
        return first;
    }
}
