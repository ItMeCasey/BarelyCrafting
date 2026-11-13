package com.itmecasey.barelycrafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.itmecasey.barelycrafting.script.ScriptEvents;
import com.itmecasey.barelycrafting.script.ScriptHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * The crafting rules of the station, all on the server.
 *
 * <p><b>One craft</b> uses one item from every filled grid slot (so the grid always holds what the recipe needs) and then keeps
 * the pattern alive: a slot that ran empty is refilled with one matching item from the neighbors, first neighbor first. When no
 * neighbor has it the slot stays empty and crafting stops as soon as the recipe no longer matches. Items a recipe leaves behind
 * (an empty bucket, a damaged tool) go into the slot they came from when it is empty, as in a crafting table; if that slot still
 * holds something else they go to the neighbors, then your inventory, then the ground at your feet. Nothing is ever deleted.
 */
public final class Crafter {
    private Crafter() {
    }

    private static CraftingInput input(Container grid) {
        List<ItemStack> items = new ArrayList<>(Math.max(grid.getContainerSize(), 9));
        for (int i = 0; i < 9; i++) {
            items.add(grid.getItem(i).copy());
        }
        return CraftingInput.of(3, 3, items);
    }

    /** A read-only snapshot of a station for ModJS script events. */
    public static com.itmecasey.barelycrafting.script.StationInfo snapshot(ServerLevel level, BlockPos pos, Container grid) {
        return StationSnapshot.of(level, pos, grid);
    }

    /** The game's recipe lookup, minus any recipe a ModJS script took away from the station. */
    private static Optional<RecipeHolder<CraftingRecipe>> lookup(ServerLevel level, CraftingInput input) {
        Optional<RecipeHolder<CraftingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (found.isPresent()) {
            ItemStack made = found.get().value().assemble(input);
            String item = made.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(made.getItem()).toString();
            if (!ScriptHooks.allowed(found.get().id().identifier().toString(), item)) {
                return Optional.empty();
            }
        }
        return found;
    }

    /** The recipe the grid currently matches, if any. */
    public static Optional<RecipeHolder<CraftingRecipe>> find(ServerLevel level, Container grid) {
        return lookup(level, input(grid));
    }

    /** What one craft would produce right now (empty when the grid matches no recipe). The grid is not changed. */
    public static ItemStack preview(ServerLevel level, Container grid) {
        CraftingInput input = input(grid);
        Optional<RecipeHolder<CraftingRecipe>> found = lookup(level, input);
        return found.isPresent() ? found.get().value().assemble(input) : ItemStack.EMPTY;
    }

    /**
     * Crafts once and returns the result, or an empty stack when the grid matches no recipe. The caller owns the result stack.
     * Ingredients come from the grid; emptied slots are refilled from the neighbors; leftovers are stored as described above.
     */
    public static ItemStack craftOnce(ServerLevel level, Container grid, List<ExternalInventory> neighbors, ServerPlayer player) {
        return craftOnce(level, grid, neighbors, player, null);
    }

    /** As {@link #craftOnce(ServerLevel, Container, List, ServerPlayer)}; with the station's position, ModJS script events fire. */
    public static ItemStack craftOnce(ServerLevel level, Container grid, List<ExternalInventory> neighbors, ServerPlayer player, @Nullable BlockPos station) {
        CraftingInput input = input(grid);
        Optional<RecipeHolder<CraftingRecipe>> found = lookup(level, input);
        if (found.isEmpty()) {
            return ItemStack.EMPTY;
        }
        RecipeHolder<CraftingRecipe> holder = found.get();
        ItemStack out = holder.value().assemble(input);
        if (out.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String outId = BuiltInRegistries.ITEM.getKey(out.getItem()).toString();
        if (station != null && ScriptEvents.fire(ScriptEvents.BEFORE_CRAFT, () -> StationSnapshot.of(level, station, grid),
                player.getName().getString(), player.getStringUUID(), outId, out.getCount(), "")) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> remainders = holder.value().getRemainingItems(input);
        List<ItemStack> extras = new ArrayList<>();
        StackKey[] refill = new StackKey[9];
        for (int i = 0; i < 9; i++) {
            ItemStack in = grid.getItem(i);
            if (in.isEmpty()) {
                continue;
            }
            StackKey key = new StackKey(in);
            grid.removeItem(i, 1);
            ItemStack rem = i < remainders.size() ? remainders.get(i) : ItemStack.EMPTY;
            ItemStack now = grid.getItem(i);
            if (!rem.isEmpty()) {
                if (now.isEmpty()) {
                    grid.setItem(i, rem.copy());
                } else if (ItemStack.isSameItemSameComponents(now, rem) && now.getCount() + rem.getCount() <= now.getMaxStackSize()) {
                    now.grow(rem.getCount());
                } else {
                    extras.add(rem.copy());
                }
            } else if (now.isEmpty()) {
                refill[i] = key;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (refill[i] != null && grid.getItem(i).isEmpty()) {
                ItemStack more = Neighbors.take(refill[i], 1, neighbors);
                if (!more.isEmpty()) {
                    grid.setItem(i, more);
                }
            }
        }
        for (ItemStack extra : extras) {
            stash(extra, neighbors, player);
        }
        out.onCraftedBy(player, out.getCount());
        if (station != null) {
            ScriptEvents.fire(ScriptEvents.CRAFTED, () -> StationSnapshot.of(level, station, grid),
                    player.getName().getString(), player.getStringUUID(), outId, out.getCount(), "");
        }
        return out;
    }

    /**
     * Crafts as many times as possible straight into the player's inventory (Shift-click on the result, the Craft All button).
     * Stops when the grid no longer matches a recipe, the inventory cannot take another result or the safety limit is reached.
     *
     * @return the number of crafts
     */
    public static int craftMax(ServerLevel level, Container grid, List<ExternalInventory> neighbors, ServerPlayer player) {
        return craftMax(level, grid, neighbors, player, null);
    }

    /** As {@link #craftMax(ServerLevel, Container, List, ServerPlayer)}; with the station's position, ModJS script events fire. */
    public static int craftMax(ServerLevel level, Container grid, List<ExternalInventory> neighbors, ServerPlayer player, @Nullable BlockPos station) {
        int limit = Math.max(1, Settings.getInt("craft.max_per_click"));
        int crafts = 0;
        ItemStack first = ItemStack.EMPTY;
        while (crafts < limit) {
            ItemStack preview = preview(level, grid);
            if (preview.isEmpty() || !canHold(player.getInventory(), preview)) {
                break;
            }
            if (first.isEmpty()) {
                first = preview;
            } else if (!ItemStack.isSameItemSameComponents(first, preview)) {
                break; // the leftovers now match a different recipe (like three cobblestone making slabs): stop, as a crafting table does
            }
            ItemStack out = craftOnce(level, grid, neighbors, player, station);
            if (out.isEmpty()) {
                break;
            }
            give(player, out);
            crafts++;
        }
        return crafts;
    }

    /** True when the main inventory (hotbar included) has room for the whole stack. */
    public static boolean canHold(Inventory inv, ItemStack stack) {
        int room = 0;
        int cap = Math.min(stack.getMaxStackSize(), inv.getMaxStackSize());
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack in = inv.getItem(i);
            if (in.isEmpty()) {
                room += cap;
            } else if (ItemStack.isSameItemSameComponents(in, stack)) {
                room += Math.max(0, Math.min(in.getMaxStackSize(), inv.getMaxStackSize()) - in.getCount());
            }
            if (room >= stack.getCount()) {
                return true;
            }
        }
        return false;
    }

    /** Gives a stack to the player; whatever does not fit lands on the ground at their feet. */
    public static void give(ServerPlayer player, ItemStack stack) {
        ItemStack rest = stack.copy();
        player.getInventory().add(rest);
        if (!rest.isEmpty()) {
            player.drop(rest, false);
        }
    }

    /** Stores a stack: neighbors first, then the player's inventory, then the ground. */
    public static void stash(ItemStack stack, List<ExternalInventory> neighbors, ServerPlayer player) {
        ItemStack rest = Neighbors.insert(stack, neighbors);
        if (!rest.isEmpty()) {
            give(player, rest);
        }
    }
}
