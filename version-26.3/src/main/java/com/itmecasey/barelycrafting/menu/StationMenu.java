package com.itmecasey.barelycrafting.menu;

import com.itmecasey.barelycrafting.Crafter;
import com.itmecasey.barelycrafting.CraftMath;
import com.itmecasey.barelycrafting.ExternalInventory;
import com.itmecasey.barelycrafting.GridMath;
import com.itmecasey.barelycrafting.ModContent;
import com.itmecasey.barelycrafting.Neighbors;
import com.itmecasey.barelycrafting.Pull;
import com.itmecasey.barelycrafting.Settings;
import com.itmecasey.barelycrafting.StackKey;
import com.itmecasey.barelycrafting.block.GridContainer;
import com.itmecasey.barelycrafting.block.StationBlockEntity;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The Crafting Station: a 3x3 grid that lives in the block (closing the screen leaves the items where they are), a result slot,
 * the player's inventory, and a column of tweak buttons. Everything that changes items happens on the server; the client only
 * gets slot updates and a few numbers (storages found, crafts left, what the neighbors hold of each grid item).
 */
public class StationMenu extends AbstractContainerMenu {
    public static final int GRID = 9;
    public static final int RESULT = 9;
    public static final int FIRST_PLAYER = 10;
    public static final int GRID_X = 56;
    public static final int GRID_Y = 33;
    public static final int RESULT_X = 140;
    public static final int RESULT_Y = 51;
    public static final int PLAYER_Y = 116;
    public static final int PLAYER_END = FIRST_PLAYER + 36;
    /** The Connected panel shows the selected storage as a window of 9 by 6 slots; tabs pick the storage, the wheel scrolls it. */
    public static final int VIEW_COLS = 9;
    public static final int VIEW_ROWS = 6;
    public static final int VIEW_PER = VIEW_COLS * VIEW_ROWS;
    public static final int VIEW_X = -184;
    public static final int VIEW_Y = 34;
    public static final int BTN_TAB = 200;
    /** {@code BTN_SCROLL + tab * 10000 + row} sets the first visible row of one storage. */
    public static final int BTN_SCROLL = 1_000_000;

    // Menu button ids (what the screen sends through clickMenuButton).
    public static final int BTN_CLEAR = 1;
    public static final int BTN_ROTATE = 2;
    public static final int BTN_MIRROR = 3;
    public static final int BTN_BALANCE = 4;
    public static final int BTN_REFILL = 5;
    public static final int BTN_CRAFT_ALL = 6;
    /** Used by recipe viewers: empties the grid (not controlled by the button settings). */
    public static final int BTN_FILL_START = 100;
    /** Used by recipe viewers: {@code BTN_FILL + slot * FILL_STRIDE + item registry id} puts one of that item in a grid slot. */
    public static final int BTN_FILL = 100_000_000;
    public static final int FILL_STRIDE = 2_000_000;

    // Synced numbers.
    private static final int D_STORAGES = 0;
    private static final int D_CRAFTS = 1;
    private static final int D_STOCK = 2;
    /** Per connected storage (up to 6): side + 1 (0 = none), block id, slots in use, slots in total. */
    private static final int D_NEIGH = D_STOCK + GRID;
    private static final int NEIGH_FIELDS = 4;
    private static final int D_SEL = D_NEIGH + 6 * NEIGH_FIELDS;
    private static final int D_SCROLL = D_SEL + 1;

    private final GridContainer grid;
    private final SimpleContainer result = new SimpleContainer(1);
    /** A read-only copy of what the connected storages hold; the server refreshes it, Minecraft syncs it like any slots. */
    private final SimpleContainer view = new SimpleContainer(VIEW_PER);
    private final ContainerData data = new SimpleContainerData(D_STOCK + GRID + 6 * NEIGH_FIELDS + 2);
    @Nullable
    private final Level level;
    @Nullable
    private final BlockPos pos;
    private final Runnable listener = this::updateResult;
    private int selected;
    private final int[] scroll = new int[6];
    private int tick;
    private boolean dirty = true;

    /** Client side. */
    public StationMenu(int id, Inventory inventory) {
        this(id, inventory, new GridContainer(() -> { }), null, null);
    }

    /** Server side: the grid is the block entity's own container. */
    public StationMenu(int id, Inventory inventory, StationBlockEntity be) {
        this(id, inventory, be.grid, be.getLevel(), be.getBlockPos());
    }

    private StationMenu(int id, Inventory inventory, GridContainer grid, @Nullable Level level, @Nullable BlockPos pos) {
        super(ModContent.STATION_MENU, id);
        this.grid = grid;
        this.level = level;
        this.pos = pos;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                addSlot(new Slot(grid, c + r * 3, GRID_X + c * 18, GRID_Y + r * 18));
            }
        }
        addSlot(new Slot(result, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, PLAYER_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, PLAYER_Y + 58));
        }
        for (int i = 0; i < VIEW_PER; i++) {
            final int k = i;
            addSlot(new Slot(view, i, VIEW_X + (i % VIEW_COLS) * 18, VIEW_Y + (i / VIEW_COLS) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return data.get(D_STORAGES) > 0 && data.get(D_SCROLL) * VIEW_COLS + k < neighborTotal(data.get(D_SEL));
                }
            });
        }
        addDataSlots(data);
        if (isServer()) {
            grid.listen(listener);
            updateResult();
        }
    }

    private boolean isServer() {
        return level instanceof ServerLevel && pos != null;
    }

    // ---- What the screen shows ----

    /** Neighboring storages the station found (0 to 6). */
    public int storages() {
        return data.get(D_STORAGES);
    }

    /** Crafts the grid and the neighbors can still make. */
    public int craftsLeft() {
        return data.get(D_CRAFTS);
    }

    /** How many of the item in a grid slot the neighbors hold (capped at 32767). */
    public int stockAt(int gridSlot) {
        return data.get(D_STOCK + gridSlot);
    }

    /** True when the next craft will refill a grid slot from a neighbor. */
    public boolean refillsFromNeighbors() {
        if (result.getItem(0).isEmpty()) {
            return false;
        }
        for (int i = 0; i < GRID; i++) {
            ItemStack s = grid.getItem(i);
            if (!s.isEmpty() && s.getCount() == 1 && stockAt(i) > 0) {
                return true;
            }
        }
        return false;
    }

    /** The side (0 down, 1 up, 2 north, 3 south, 4 west, 5 east) of the n-th connected storage, or -1 when there is none. */
    public int neighborSide(int n) {
        return data.get(D_NEIGH + n * NEIGH_FIELDS) - 1;
    }

    /** The block of the n-th connected storage (shown as its item). */
    public net.minecraft.world.level.block.Block neighborBlock(int n) {
        return BuiltInRegistries.BLOCK.byId(data.get(D_NEIGH + n * NEIGH_FIELDS + 1));
    }

    /** The tab (connected storage) shown in the panel. */
    public int selectedTab() {
        return data.get(D_SEL);
    }

    /** The first visible row of the shown storage. */
    public int viewScroll() {
        return data.get(D_SCROLL);
    }

    public int neighborUsed(int n) {
        return data.get(D_NEIGH + n * NEIGH_FIELDS + 2);
    }

    public int neighborTotal(int n) {
        return data.get(D_NEIGH + n * NEIGH_FIELDS + 3);
    }

    public Container grid() {
        return grid;
    }

    public boolean resultReady() {
        return !result.getItem(0).isEmpty();
    }

    public boolean gridEmpty() {
        for (int i = 0; i < GRID; i++) {
            if (!grid.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ---- Server: result and numbers ----

    private void updateResult() {
        if (level instanceof ServerLevel server) {
            result.setItem(0, Crafter.preview(server, grid));
            dirty = true;
        }
    }

    private void refreshData() {
        ServerLevel server = (ServerLevel) level;
        List<Neighbors.Found> found = Neighbors.scanFull(server, pos);
        List<ExternalInventory> neighbors = new java.util.ArrayList<>();
        for (Neighbors.Found f : found) {
            neighbors.add(f.inventory());
        }
        data.set(D_STORAGES, neighbors.size());
        for (int n = 0; n < 6; n++) {
            int base = D_NEIGH + n * NEIGH_FIELDS;
            if (n < found.size()) {
                Neighbors.Found f = found.get(n);
                ExternalInventory inv = f.inventory();
                int used = 0;
                for (int i = 0; i < inv.slots(); i++) {
                    if (!inv.get(i).isEmpty()) {
                        used++;
                    }
                }
                data.set(base, f.side().ordinal() + 1);
                data.set(base + 1, BuiltInRegistries.BLOCK.getId(server.getBlockState(pos.relative(f.side())).getBlock()));
                data.set(base + 2, Math.min(32767, used));
                data.set(base + 3, Math.min(32767, inv.slots()));
            } else {
                data.set(base, 0);
                data.set(base + 1, 0);
                data.set(base + 2, 0);
                data.set(base + 3, 0);
            }
        }
        if (found.isEmpty()) {
            selected = 0;
            data.set(D_SEL, 0);
            data.set(D_SCROLL, 0);
            for (int k = 0; k < VIEW_PER; k++) {
                if (!view.getItem(k).isEmpty()) {
                    view.setItem(k, ItemStack.EMPTY);
                }
            }
        } else {
            selected = Math.max(0, Math.min(selected, found.size() - 1));
            ExternalInventory shownInv = found.get(selected).inventory();
            int total = shownInv.slots();
            scroll[selected] = com.itmecasey.barelycrafting.ViewMath.clampRow(scroll[selected], total, VIEW_COLS, VIEW_ROWS);
            data.set(D_SEL, selected);
            data.set(D_SCROLL, scroll[selected]);
            for (int k = 0; k < VIEW_PER; k++) {
                int at = scroll[selected] * VIEW_COLS + k;
                ItemStack shown = at < total ? shownInv.get(at) : ItemStack.EMPTY;
                shown = shown.isEmpty() ? ItemStack.EMPTY : shown.copyWithCount(Math.min(shown.getCount(), 99));
                if (!ItemStack.matches(view.getItem(k), shown)) {
                    view.setItem(k, shown);
                }
            }
        }
        StackKey[] keys = new StackKey[GRID];
        for (int i = 0; i < GRID; i++) {
            ItemStack s = grid.getItem(i);
            keys[i] = s.isEmpty() ? null : new StackKey(s);
            data.set(D_STOCK + i, keys[i] == null ? 0 : (int) Math.min(32767, Neighbors.count(keys[i], neighbors)));
        }
        int crafts = 0;
        if (!result.getItem(0).isEmpty()) {
            Map<StackKey, Integer> need = CraftMath.perCraft(keys);
            List<com.itmecasey.barelycrafting.Slots<StackKey>> sources = new java.util.ArrayList<>();
            sources.add(Neighbors.slots(grid));
            sources.addAll(Neighbors.slotsOf(neighbors));
            crafts = CraftMath.crafts(need, key -> Pull.count(key, sources), 9999);
        }
        data.set(D_CRAFTS, crafts);
    }

    @Override
    public void broadcastChanges() {
        if (isServer() && (dirty || ++tick % 5 == 0)) {
            dirty = false;
            refreshData();
        }
        super.broadcastChanges();
    }

    // ---- Server: clicks ----

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex == RESULT) {
            if (isServer() && player instanceof ServerPlayer sp) {
                resultClick((ServerLevel) level, sp, input);
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    private void resultClick(ServerLevel server, ServerPlayer player, ContainerInput input) {
        List<ExternalInventory> neighbors = Neighbors.scan(server, pos);
        if (input == ContainerInput.QUICK_MOVE) {
            Crafter.craftMax(server, grid, neighbors, player);
        } else if (input == ContainerInput.PICKUP) {
            ItemStack preview = Crafter.preview(server, grid);
            if (preview.isEmpty()) {
                return;
            }
            ItemStack carried = getCarried();
            if (!carried.isEmpty() && (!ItemStack.isSameItemSameComponents(carried, preview)
                    || carried.getCount() + preview.getCount() > carried.getMaxStackSize())) {
                return;
            }
            ItemStack out = Crafter.craftOnce(server, grid, neighbors, player);
            if (out.isEmpty()) {
                return;
            }
            if (carried.isEmpty()) {
                setCarried(out);
            } else {
                carried.grow(out.getCount());
            }
        }
        dirty = true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!isServer() || !(player instanceof ServerPlayer sp)) {
            return false;
        }
        ServerLevel server = (ServerLevel) level;
        List<ExternalInventory> neighbors = Neighbors.scan(server, pos);
        if (id >= BTN_TAB && id < BTN_TAB + 6) {
            selected = id - BTN_TAB;
            dirty = true;
            return true;
        }
        if (id >= BTN_SCROLL && id < BTN_FILL) {
            int tab = (id - BTN_SCROLL) / 10000;
            if (tab < 6) {
                scroll[tab] = Math.max(0, (id - BTN_SCROLL) % 10000);
                dirty = true;
            }
            return true;
        }
        if (id >= BTN_FILL) {
            int code = id - BTN_FILL;
            fillSlot(code / FILL_STRIDE, code % FILL_STRIDE, sp, neighbors);
            return true;
        }
        switch (id) {
            case BTN_FILL_START -> clearGrid(sp, neighbors);
            case BTN_CLEAR -> {
                if (!Settings.getBool("buttons.clear")) {
                    return false;
                }
                clearGrid(sp, neighbors);
            }
            case BTN_ROTATE -> {
                if (!Settings.getBool("buttons.rotate")) {
                    return false;
                }
                ItemStack[] cells = cells();
                GridMath.rotate(cells);
                setCells(cells);
            }
            case BTN_MIRROR -> {
                if (!Settings.getBool("buttons.mirror")) {
                    return false;
                }
                ItemStack[] cells = cells();
                GridMath.mirror(cells);
                setCells(cells);
            }
            case BTN_BALANCE -> {
                if (!Settings.getBool("buttons.balance")) {
                    return false;
                }
                balance();
            }
            case BTN_REFILL -> {
                if (!Settings.getBool("buttons.refill")) {
                    return false;
                }
                refill(sp, neighbors);
            }
            case BTN_CRAFT_ALL -> {
                if (!Settings.getBool("buttons.craft_all")) {
                    return false;
                }
                Crafter.craftMax(server, grid, neighbors, sp);
            }
            default -> {
                return false;
            }
        }
        dirty = true;
        return true;
    }

    private ItemStack[] cells() {
        ItemStack[] cells = new ItemStack[GRID];
        for (int i = 0; i < GRID; i++) {
            cells[i] = grid.getItem(i);
        }
        return cells;
    }

    private void setCells(ItemStack[] cells) {
        for (int i = 0; i < GRID; i++) {
            grid.setItem(i, cells[i]);
        }
    }

    /** Empties the grid: neighbors first, then the player's inventory, then the ground. */
    private void clearGrid(ServerPlayer player, List<ExternalInventory> neighbors) {
        for (int i = 0; i < GRID; i++) {
            ItemStack s = grid.removeItemNoUpdate(i);
            if (!s.isEmpty()) {
                Crafter.stash(s, neighbors, player);
            }
        }
        grid.setChanged();
        updateResult();
    }

    private void balance() {
        StackKey[] keys = new StackKey[GRID];
        int[] counts = new int[GRID];
        int max = 64;
        for (int i = 0; i < GRID; i++) {
            ItemStack s = grid.getItem(i);
            if (!s.isEmpty()) {
                keys[i] = new StackKey(s);
                counts[i] = s.getCount();
                max = Math.min(max, s.getMaxStackSize());
            }
        }
        if (GridMath.balance(keys, counts, max)) {
            for (int i = 0; i < GRID; i++) {
                if (keys[i] != null) {
                    grid.getItem(i).setCount(counts[i]);
                }
            }
            grid.setChanged();
        }
    }

    /** Tops every filled slot up to a full stack: your inventory first, then the neighbors. */
    private void refill(ServerPlayer player, List<ExternalInventory> neighbors) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < GRID; i++) {
            ItemStack s = grid.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            int room = s.getMaxStackSize() - s.getCount();
            for (int j = 0; j < Inventory.INVENTORY_SIZE && room > 0; j++) {
                ItemStack in = inv.getItem(j);
                if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, s)) {
                    int take = Math.min(room, in.getCount());
                    in.shrink(take);
                    s.grow(take);
                    room -= take;
                }
            }
            if (room > 0) {
                ItemStack more = Neighbors.take(new StackKey(s), room, neighbors);
                s.grow(more.getCount());
            }
        }
        grid.setChanged();
    }

    /** Recipe viewer transfer: puts one of the item in a grid slot, taken from the grid's own stock, your inventory or the neighbors. */
    private void fillSlot(int slot, int rawItem, ServerPlayer player, List<ExternalInventory> neighbors) {
        if (slot < 0 || slot >= GRID || rawItem <= 0) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.byId(rawItem);
        if (item == Items.AIR || !grid.getItem(slot).isEmpty()) {
            return;
        }
        Inventory inv = player.getInventory();
        for (int j = 0; j < Inventory.INVENTORY_SIZE; j++) {
            ItemStack in = inv.getItem(j);
            if (!in.isEmpty() && in.is(item)) {
                grid.setItem(slot, in.split(1));
                return;
            }
        }
        for (ExternalInventory inv2 : neighbors) {
            for (int j = 0; j < inv2.slots(); j++) {
                ItemStack in = inv2.get(j);
                if (!in.isEmpty() && in.is(item)) {
                    ItemStack got = inv2.extract(j, 1, false);
                    if (!got.isEmpty()) {
                        grid.setItem(slot, got);
                        return;
                    }
                }
            }
        }
    }

    // ---- Shift-click ----

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == RESULT || index < 0 || index >= PLAYER_END) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        boolean moved;
        if (index < GRID) {
            moved = moveItemStackTo(stack, FIRST_PLAYER, PLAYER_END, true);
        } else if (!moveItemStackTo(stack, 0, GRID, false)) {
            int hotbar = PLAYER_END - 9;
            moved = index >= PLAYER_END ? false : index < hotbar ? moveItemStackTo(stack, hotbar, PLAYER_END, false) : moveItemStackTo(stack, FIRST_PLAYER, hotbar, false);
        } else {
            moved = true;
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (isServer()) {
            grid.unlisten(listener);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (pos == null || level == null) {
            return true;
        }
        return level.getBlockState(pos).is(ModContent.STATION_BLOCK)
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
