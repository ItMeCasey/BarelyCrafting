package com.itmecasey.barelycrafting.neoforge.compat;

import com.itmecasey.barelycrafting.compat.StationTransfer;
import com.itmecasey.barelycrafting.menu.StationMenu;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.crafting.CraftingDisplay;
import net.minecraft.world.item.ItemStack;

/**
 * REI: the plus button of a crafting recipe fills the grid of the Crafting Station (see {@link StationTransfer}). REI finds this
 * class by its annotation and only when REI is installed, so the mod runs the same without it.
 */
@REIPluginClient
public class StationReiPlugin implements REIClientPlugin {
    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(context -> {
            if (!(context.getMenu() instanceof StationMenu menu) || !(context.getDisplay() instanceof CraftingDisplay display)) {
                return TransferHandler.Result.createNotApplicable();
            }
            if (context.isActuallyCrafting()) {
                List<List<ItemStack>> cells = new ArrayList<>();
                List<EntryIngredient> organised = display.getOrganisedInputEntries(3, 3);
                for (int i = 0; i < StationMenu.GRID; i++) {
                    List<ItemStack> items = new ArrayList<>();
                    if (i < organised.size()) {
                        for (EntryStack<?> entry : organised.get(i)) {
                            if (entry.getType() == VanillaEntryTypes.ITEM) {
                                items.add(entry.<ItemStack>castValue());
                            }
                        }
                    }
                    cells.add(items);
                }
                StationTransfer.fill(menu, cells);
            }
            return TransferHandler.Result.createSuccessful().blocksFurtherHandling();
        });
    }
}
