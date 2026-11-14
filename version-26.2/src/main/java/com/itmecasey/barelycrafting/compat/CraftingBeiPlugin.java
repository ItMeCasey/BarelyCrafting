package com.itmecasey.barelycrafting.compat;

import com.itmecasey.barelycrafting.menu.StationMenu;
import com.itmecasey.bei.api.BeiPlugin;
import com.itmecasey.bei.api.IBeiPlugin;
import com.itmecasey.bei.api.IRecipeTransferHandler;
import com.itmecasey.bei.api.ITransferRegistration;
import com.itmecasey.bei.api.TransferContext;
import com.itmecasey.bei.api.TransferState;
import java.util.ArrayList;
import java.util.List;
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
                int width = Math.max(0, ctx.recipe().width());
                List<List<ItemStack>> cells = new ArrayList<>();
                for (int i = 0; i < StationMenu.GRID; i++) {
                    cells.add(List.of());
                }
                for (int i = 0; i < ctx.recipe().inputs().size(); i++) {
                    int cell = width > 0 ? (i / width) * 3 + (i % width) : i;
                    if (cell >= 0 && cell < StationMenu.GRID) {
                        cells.set(cell, ctx.recipe().inputs().get(i));
                    }
                }
                return StationTransfer.fill(ctx.menu(), cells);
            }
        });
    }
}
