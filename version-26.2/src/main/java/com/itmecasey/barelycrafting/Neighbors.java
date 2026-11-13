package com.itmecasey.barelycrafting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The storages next to a station. Every one of the six sides is checked in the order the {@code neighbors.faces} setting lists
 * (default: below, above, north, south, west, east); a side counts when the block there offers an item handler on its face
 * toward the station. At most {@code neighbors.max} storages are used.
 */
public final class Neighbors {
    private Neighbors() {
    }

    /** The scanned sides, in order, from the setting (unknown names are skipped). */
    public static List<Direction> faces() {
        Set<Direction> out = new LinkedHashSet<>();
        for (String name : Settings.getString("neighbors.faces").split(",")) {
            Direction d = Direction.byName(name.trim().toLowerCase(java.util.Locale.ROOT));
            if (d != null) {
                out.add(d);
            }
        }
        return new ArrayList<>(out);
    }

    /** A storage found next to the station and the side of the station it touches. */
    public record Found(Direction side, ExternalInventory inventory) {
    }

    /** The neighboring storages in pull order. Empty when neighbor use is switched off. */
    public static List<ExternalInventory> scan(Level level, BlockPos pos) {
        List<ExternalInventory> out = new ArrayList<>();
        for (Found f : scanFull(level, pos)) {
            out.add(f.inventory());
        }
        return out;
    }

    /** Like scan, but keeps the side each storage was found on. */
    public static List<Found> scanFull(Level level, BlockPos pos) {
        List<Found> out = new ArrayList<>();
        if (!Settings.getBool("neighbors.enabled")) {
            return out;
        }
        int max = Math.max(1, Math.min(6, Settings.getInt("neighbors.max")));
        for (Direction d : faces()) {
            if (out.size() >= max) {
                break;
            }
            BlockPos at = pos.relative(d);
            ExternalInventory inv = Inventories.at(level, at, d.getOpposite());
            if (inv == null || inv.slots() <= 0) {
                continue;
            }
            out.add(new Found(d, inv));
        }
        return out;
    }

    // ---- Views of inventories as keys and counts, for the pull rules ----

    /** A vanilla container (the grid) as {@link Slots}. */
    public static Slots<StackKey> slots(Container container) {
        return new Slots<>() {
            @Override
            public int size() {
                return container.getContainerSize();
            }

            @Override
            public StackKey key(int slot) {
                ItemStack s = container.getItem(slot);
                return s.isEmpty() ? null : new StackKey(s);
            }

            @Override
            public int count(int slot) {
                return container.getItem(slot).getCount();
            }

            @Override
            public int extract(int slot, int amount) {
                return container.removeItem(slot, amount).getCount();
            }
        };
    }

    /** A neighboring storage as {@link Slots}. */
    public static Slots<StackKey> slots(ExternalInventory inv) {
        return new Slots<>() {
            @Override
            public int size() {
                return inv.slots();
            }

            @Override
            public StackKey key(int slot) {
                ItemStack s = inv.get(slot);
                return s.isEmpty() ? null : new StackKey(s);
            }

            @Override
            public int count(int slot) {
                return inv.get(slot).getCount();
            }

            @Override
            public int extract(int slot, int amount) {
                return inv.extract(slot, amount, false).getCount();
            }
        };
    }

    public static List<Slots<StackKey>> slotsOf(List<ExternalInventory> inventories) {
        List<Slots<StackKey>> out = new ArrayList<>();
        for (ExternalInventory inv : inventories) {
            out.add(slots(inv));
        }
        return out;
    }

    /** How many of {@code key} the neighbors hold. */
    public static long count(StackKey key, List<ExternalInventory> neighbors) {
        return Pull.count(key, slotsOf(neighbors));
    }

    /** Takes up to {@code amount} of the item out of the neighbors, first neighbor first, and returns it as one stack (or several merged). */
    public static ItemStack take(StackKey key, int amount, List<ExternalInventory> neighbors) {
        int got = Pull.take(key, amount, slotsOf(neighbors), null);
        return got <= 0 ? ItemStack.EMPTY : key.stack().copyWithCount(got);
    }

    /** Puts a stack into the neighbors, first neighbor first, and returns what did not fit. */
    public static ItemStack insert(ItemStack stack, List<ExternalInventory> neighbors) {
        ItemStack rest = stack.copy();
        for (ExternalInventory inv : neighbors) {
            if (rest.isEmpty()) {
                break;
            }
            rest = inv.insert(rest, false);
        }
        return rest;
    }
}
