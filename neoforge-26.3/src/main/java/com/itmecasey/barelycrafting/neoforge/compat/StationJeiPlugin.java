package com.itmecasey.barelycrafting.neoforge.compat;

import com.itmecasey.barelycrafting.BarelyCrafting;
import com.itmecasey.barelycrafting.ModContent;
import com.itmecasey.barelycrafting.compat.StationTransfer;
import com.itmecasey.barelycrafting.menu.StationMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * JEI: the plus button of a crafting recipe fills the grid of the Crafting Station (see {@link StationTransfer}). JEI finds this
 * class by its annotation and only when JEI is installed, so the mod runs the same without it.
 */
@JeiPlugin
public class StationJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(BarelyCrafting.MODID, "jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new IRecipeTransferHandler<StationMenu, RecipeHolder<CraftingRecipe>>() {
            @Override
            public Class<? extends StationMenu> getContainerClass() {
                return StationMenu.class;
            }

            @Override
            public Optional<MenuType<StationMenu>> getMenuType() {
                return Optional.of(ModContent.STATION_MENU);
            }

            @Override
            public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
                return RecipeTypes.CRAFTING;
            }

            @Override
            @SuppressWarnings("removal")
            public @Nullable IRecipeTransferError transferRecipe(StationMenu menu, RecipeHolder<CraftingRecipe> recipe,
                    IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
                if (doTransfer) {
                    List<List<ItemStack>> cells = new ArrayList<>();
                    List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
                    for (int i = 0; i < StationMenu.GRID; i++) {
                        cells.add(i < inputs.size() ? inputs.get(i).getItemStacks().toList() : List.of());
                    }
                    StationTransfer.fill(menu, cells);
                }
                return null;
            }
        }, RecipeTypes.CRAFTING);
    }
}
