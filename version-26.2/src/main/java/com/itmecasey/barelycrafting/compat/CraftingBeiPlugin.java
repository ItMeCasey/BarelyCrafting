package com.itmecasey.barelycrafting.compat;

import com.itmecasey.barelycrafting.menu.StationMenu;
import com.itmecasey.bei.api.BeiPlugin;
import com.itmecasey.bei.api.IBeiPlugin;
import com.itmecasey.bei.api.IRecipeTransferHandler;
import com.itmecasey.bei.api.ITransferRegistration;
import com.itmecasey.bei.api.TransferContext;
import com.itmecasey.bei.api.TransferState;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * BEI (Barely Enough Items): the plus button of a crafting recipe fills the Crafting Station's grid. The grid is emptied first
 * (its items go to the neighboring storage, then your inventory), then every slot is filled with one ingredient taken from your
 * inventory first and the neighboring storages second. Loaded only when BEI is installed.
 */
@BeiPlugin
public class CraftingBeiPlugin implements IBeiPlugin {
    @Override
    public void registerTransfers(ITransferRegistration registration) {
        registration.register(new IRecipeTransferHandler<StationMenu>() {
            @Override
            public Class<StationMenu> menuClass() {
                return StationMenu.class;
            }

            @Override
            public TransferState check(TransferContext<StationMenu> ctx) {
                String type = ctx.recipe().type();
                return type.equals("ShapedCrafting") || type.equals("ShapelessCrafting") ? TransferState.enabled() : TransferState.hidden();
            }

            @Override
            public boolean transfer(TransferContext<StationMenu> ctx) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player == null || minecraft.gameMode == null) {
                    return false;
                }
                StationMenu menu = ctx.menu();
                int width = Math.max(0, ctx.recipe().width());
                int[] raw = new int[StationMenu.GRID];
                for (int i = 0; i < ctx.recipe().inputs().size(); i++) {
                    int cell = width > 0 ? (i / width) * 3 + (i % width) : i;
                    if (cell >= 0 && cell < StationMenu.GRID) {
                        ItemStack pick = pick(ctx.recipe().inputs().get(i), minecraft);
                        raw[cell] = pick.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(pick.getItem());
                    }
                }
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StationMenu.BTN_FILL_START);
                for (int cell = 0; cell < StationMenu.GRID; cell++) {
                    if (raw[cell] > 0) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StationMenu.BTN_FILL + cell * StationMenu.FILL_STRIDE + raw[cell]);
                    }
                }
                return true;
            }
        });
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
