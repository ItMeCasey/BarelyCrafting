package com.itmecasey.barelycrafting.dev;

import com.itmecasey.barelycrafting.Crafter;
import com.itmecasey.barelycrafting.Dev;
import com.itmecasey.barelycrafting.ModContent;
import com.itmecasey.barelycrafting.Neighbors;
import com.itmecasey.barelycrafting.Settings;
import com.itmecasey.barelycrafting.block.StationBlockEntity;
import com.itmecasey.barelycrafting.menu.StationMenu;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check of the station on a real server: persistence, crafting from the grid, crafting from neighboring chests (through
 * the loader's real item handler), conservation of every item, leftovers, every tweak button and the settings that switch them
 * off. Runs once at server start when {@code BARELYCRAFTING_SELFTEST} is set and prints {@code SELFTEST PASS/FAIL} lines ending in
 * {@code SELFTEST DONE failures=N}.
 */
public final class SelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyCrafting");
    private static ServerLevel level;
    private static ServerPlayer player;
    private static int failures;

    private SelfTest() {
    }

    public static boolean requested() {
        return System.getenv("BARELYCRAFTING_SELFTEST") != null && Dev.ENABLED;
    }

    public static int run(MinecraftServer server) {
        level = server.overworld();
        failures = 0;
        BlockPos spawn = level.getRespawnData().pos();
        int x = spawn.getX() + 3;
        int z = spawn.getZ() + 3;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 20;
        player = new ServerPlayer(server, level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "selftest"), ClientInformation.createDefault());
        try {
            registered(new BlockPos(x, y, z));
            persistence(new BlockPos(x, y, z + 4));
            scan(new BlockPos(x, y, z + 8));
            craftFromGrid(new BlockPos(x, y, z + 12));
            craftFromNeighbor(new BlockPos(x, y, z + 16));
            gridFirstThenNeighbor(new BlockPos(x, y, z + 20));
            remainders(new BlockPos(x, y, z + 24));
            fullInventory(new BlockPos(x, y, z + 28));
            buttons(new BlockPos(x, y, z + 32));
            disabledButtons(new BlockPos(x, y, z + 36));
            syncedNumbers(new BlockPos(x, y, z + 40));
            resultEdgeCases(new BlockPos(x, y, z + 44));
            pickaxeConservation(new BlockPos(x, y, z + 48));
            connectedList(new BlockPos(x, y, z + 52));
        } catch (RuntimeException e) {
            LOGGER.error("SELFTEST error", e);
            verdict("selftest_ran_clean", false, e.toString());
        } finally {
            Settings.reset();
        }
        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    // ---- helpers ----

    private static void verdict(String name, boolean ok, String detail) {
        if (ok) {
            LOGGER.info("SELFTEST PASS {} ({})", name, detail);
        } else {
            failures++;
            LOGGER.error("SELFTEST FAIL {} ({})", name, detail);
        }
    }

    private static void area(BlockPos p) {
        for (BlockPos b : BlockPos.betweenClosed(p.offset(-2, -1, -2), p.offset(2, 2, 2))) {
            level.setBlock(b, Blocks.AIR.defaultBlockState(), 2);
        }
        player.getInventory().clearContent();
        Settings.reset();
    }

    private static StationBlockEntity station(BlockPos p) {
        level.setBlock(p, ModContent.STATION_BLOCK.defaultBlockState(), 3);
        return (StationBlockEntity) level.getBlockEntity(p);
    }

    private static Container chest(BlockPos p) {
        level.setBlock(p, Blocks.CHEST.defaultBlockState(), 3);
        return (Container) level.getBlockEntity(p);
    }

    private static StationMenu open(StationBlockEntity be) {
        StationMenu menu = new StationMenu(7, player.getInventory(), be);
        menu.broadcastChanges();
        return menu;
    }

    private static int count(Container c, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(item)) {
                n += c.getItem(i).getCount();
            }
        }
        return n;
    }

    private static int count(net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }

    private static void planks2x2(StationBlockEntity be, int each) {
        be.grid.setItem(0, new ItemStack(Items.OAK_PLANKS, each));
        be.grid.setItem(1, new ItemStack(Items.OAK_PLANKS, each));
        be.grid.setItem(3, new ItemStack(Items.OAK_PLANKS, each));
        be.grid.setItem(4, new ItemStack(Items.OAK_PLANKS, each));
    }

    // ---- tests ----

    private static void registered(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        verdict("block_and_block_entity_exist", be != null && level.getBlockState(p).is(ModContent.STATION_BLOCK), "be=" + be);
        verdict("menu_type_and_item_exist", ModContent.STATION_MENU != null && ModContent.STATION_ITEM != null, "ok");
    }

    private static void persistence(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        be.grid.setItem(0, new ItemStack(Items.STICK, 5));
        be.grid.setItem(8, new ItemStack(Items.DIAMOND, 2));
        StationMenu menu = open(be);
        menu.removed(player);
        verdict("closing_the_screen_keeps_the_grid", be.grid.getItem(0).getCount() == 5 && be.grid.getItem(8).getCount() == 2, "slot0=" + be.grid.getItem(0));
        CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
        BlockEntity copy = BlockEntity.loadStatic(p, level.getBlockState(p), tag, level.registryAccess());
        boolean same = copy instanceof StationBlockEntity c && c.grid.getItem(0).is(Items.STICK) && c.grid.getItem(0).getCount() == 5
                && c.grid.getItem(8).is(Items.DIAMOND) && c.grid.getItem(8).getCount() == 2 && c.grid.getItem(4).isEmpty();
        verdict("grid_survives_a_save_and_load", same, "copy=" + copy);
    }

    private static void scan(BlockPos p) {
        area(p);
        station(p);
        chest(p.east());
        chest(p.west());
        level.setBlock(p.above(), Blocks.STONE.defaultBlockState(), 3);
        verdict("scan_finds_two_chests_not_stone", Neighbors.scan(level, p).size() == 2, "found " + Neighbors.scan(level, p).size());
        Settings.set("neighbors.faces", "east");
        verdict("scan_follows_the_faces_setting", Neighbors.scan(level, p).size() == 1, "found " + Neighbors.scan(level, p).size());
        Settings.reset();
        Settings.set("neighbors.max", 1);
        verdict("scan_respects_the_max_setting", Neighbors.scan(level, p).size() == 1, "found " + Neighbors.scan(level, p).size());
        Settings.reset();
        Settings.set("neighbors.enabled", false);
        verdict("scan_is_empty_when_switched_off", Neighbors.scan(level, p).isEmpty(), "found " + Neighbors.scan(level, p).size());
    }

    private static void craftFromGrid(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        planks2x2(be, 1);
        StationMenu menu = open(be);
        verdict("result_slot_shows_the_recipe", menu.slots.get(StationMenu.RESULT).getItem().is(Items.CRAFTING_TABLE), "result=" + menu.slots.get(StationMenu.RESULT).getItem());
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.PICKUP, player);
        verdict("click_takes_one_craft_and_uses_the_grid", menu.getCarried().is(Items.CRAFTING_TABLE) && menu.getCarried().getCount() == 1 && be.grid.isEmpty(),
                "carried=" + menu.getCarried() + " gridEmpty=" + be.grid.isEmpty());
        menu.broadcastChanges();
        verdict("result_clears_when_the_grid_empties", menu.slots.get(StationMenu.RESULT).getItem().isEmpty(), "result=" + menu.slots.get(StationMenu.RESULT).getItem());
    }

    private static void craftFromNeighbor(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        Container chest = chest(p.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        planks2x2(be, 1);
        StationMenu menu = open(be);
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.QUICK_MOVE, player);
        int tables = count(player.getInventory(), Items.CRAFTING_TABLE);
        int left = count(chest, Items.OAK_PLANKS) + count(be.grid, Items.OAK_PLANKS);
        verdict("shift_click_crafts_the_grid_and_the_chest_down_to_nothing", tables == 17 && left == 0,
                "tables=" + tables + " planksLeft=" + left + " (68 planks make 17 tables)");
        verdict("nothing_is_created_or_lost_when_crafting_from_a_neighbor", tables * 4 + left == 68, "tables*4+left=" + (tables * 4 + left));
        verdict("grid_pattern_is_gone_after_the_last_craft", be.grid.isEmpty(), "gridEmpty=" + be.grid.isEmpty());
    }

    private static void gridFirstThenNeighbor(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        Container chest = chest(p.east());
        chest.setItem(0, new ItemStack(Items.CLAY_BALL, 6));
        for (int slot : new int[] {0, 1, 3, 4}) {
            be.grid.setItem(slot, new ItemStack(Items.CLAY_BALL, 2));
        }
        StationMenu menu = open(be);
        verdict("clay_recipe_matches", menu.slots.get(StationMenu.RESULT).getItem().is(Items.CLAY), "result=" + menu.slots.get(StationMenu.RESULT).getItem());
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.QUICK_MOVE, player);
        int blocks = count(player.getInventory(), Items.CLAY);
        int inGrid = count(be.grid, Items.CLAY_BALL);
        int inChest = count(chest, Items.CLAY_BALL);
        verdict("grid_is_used_before_the_neighbor", blocks == 3 && inChest == 0 && inGrid == 2,
                "blocks=" + blocks + " grid=" + inGrid + " chest=" + inChest + " (14 balls: 3 blocks use 12, 2 stay in the grid)");
        verdict("conservation_with_a_stocked_grid", blocks * 4 + inGrid + inChest == 14, "total=" + (blocks * 4 + inGrid + inChest));
    }

    private static void remainders(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        Container chest = chest(p.east());
        for (int i = 0; i < 3; i++) {
            be.grid.setItem(i, new ItemStack(Items.MILK_BUCKET));
        }
        be.grid.setItem(3, new ItemStack(Items.SUGAR));
        be.grid.setItem(4, new ItemStack(Items.EGG));
        be.grid.setItem(5, new ItemStack(Items.SUGAR));
        for (int i = 6; i < 9; i++) {
            be.grid.setItem(i, new ItemStack(Items.WHEAT));
        }
        for (int i = 0; i < 3; i++) {
            chest.setItem(i, new ItemStack(Items.MILK_BUCKET));
        }
        StationMenu menu = open(be);
        verdict("cake_recipe_matches", menu.slots.get(StationMenu.RESULT).getItem().is(Items.CAKE), "result=" + menu.slots.get(StationMenu.RESULT).getItem());
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.PICKUP, player);
        int buckets = 0;
        for (int i = 0; i < 3; i++) {
            buckets += be.grid.getItem(i).is(Items.BUCKET) ? 1 : 0;
        }
        verdict("container_items_come_back_into_their_slots", menu.getCarried().is(Items.CAKE) && buckets == 3
                && count(chest, Items.MILK_BUCKET) == 3, "carried=" + menu.getCarried() + " emptyBucketsInGrid=" + buckets + " milkInChest=" + count(chest, Items.MILK_BUCKET));
    }

    private static void fullInventory(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        Container chest = chest(p.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        planks2x2(be, 1);
        for (int i = 0; i < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; i++) {
            player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        StationMenu menu = open(be);
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.QUICK_MOVE, player);
        verdict("shift_click_stops_when_the_inventory_is_full_and_loses_nothing",
                count(player.getInventory(), Items.CRAFTING_TABLE) == 0 && count(chest, Items.OAK_PLANKS) == 64 && count(be.grid, Items.OAK_PLANKS) == 4,
                "tables=" + count(player.getInventory(), Items.CRAFTING_TABLE) + " chest=" + count(chest, Items.OAK_PLANKS) + " grid=" + count(be.grid, Items.OAK_PLANKS));
        player.getInventory().setItem(0, ItemStack.EMPTY);
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.QUICK_MOVE, player);
        verdict("shift_click_uses_the_one_free_slot_and_loses_nothing",
                count(player.getInventory(), Items.CRAFTING_TABLE) == 17 && count(chest, Items.OAK_PLANKS) + count(be.grid, Items.OAK_PLANKS) == 0,
                "tables=" + count(player.getInventory(), Items.CRAFTING_TABLE));
    }

    private static void buttons(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        Container chest = chest(p.east());
        StationMenu menu = open(be);
        be.grid.setItem(0, new ItemStack(Items.STICK));
        be.grid.setItem(1, new ItemStack(Items.DIAMOND));
        boolean ok = menu.clickMenuButton(player, StationMenu.BTN_ROTATE);
        verdict("rotate_turns_the_grid_clockwise", ok && be.grid.getItem(2).is(Items.STICK) && be.grid.getItem(5).is(Items.DIAMOND) && be.grid.getItem(0).isEmpty(),
                "slot2=" + be.grid.getItem(2) + " slot5=" + be.grid.getItem(5));
        menu.clickMenuButton(player, StationMenu.BTN_MIRROR);
        verdict("mirror_flips_left_to_right", be.grid.getItem(0).is(Items.STICK) && be.grid.getItem(3).is(Items.DIAMOND), "slot0=" + be.grid.getItem(0) + " slot3=" + be.grid.getItem(3));
        be.grid.removeItemNoUpdate(0);
        be.grid.removeItemNoUpdate(3);
        be.grid.setItem(0, new ItemStack(Items.OAK_PLANKS, 7));
        be.grid.setItem(1, new ItemStack(Items.OAK_PLANKS, 1));
        be.grid.setItem(2, new ItemStack(Items.OAK_PLANKS, 1));
        menu.clickMenuButton(player, StationMenu.BTN_BALANCE);
        verdict("balance_spreads_evenly", be.grid.getItem(0).getCount() == 3 && be.grid.getItem(1).getCount() == 3 && be.grid.getItem(2).getCount() == 3,
                "counts=" + be.grid.getItem(0).getCount() + "," + be.grid.getItem(1).getCount() + "," + be.grid.getItem(2).getCount());
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 36));
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 36));
        menu.clickMenuButton(player, StationMenu.BTN_REFILL);
        verdict("refill_tops_up_from_the_player_first_then_the_neighbor",
                be.grid.getItem(0).getCount() == 64 && be.grid.getItem(1).getCount() == 64 && be.grid.getItem(2).getCount() == 64
                        && count(player.getInventory(), Items.OAK_PLANKS) == 0 && count(chest, Items.OAK_PLANKS) == 17,
                "grid=" + count(be.grid, Items.OAK_PLANKS) + " player=" + count(player.getInventory(), Items.OAK_PLANKS) + " chest=" + count(chest, Items.OAK_PLANKS));
        verdict("refill_conserves_items", count(be.grid, Items.OAK_PLANKS) + count(player.getInventory(), Items.OAK_PLANKS) + count(chest, Items.OAK_PLANKS) == 209,
                "total=" + (count(be.grid, Items.OAK_PLANKS) + count(player.getInventory(), Items.OAK_PLANKS) + count(chest, Items.OAK_PLANKS)));
        be.grid.setItem(3, new ItemStack(Items.STICK, 3));
        menu.clickMenuButton(player, StationMenu.BTN_CLEAR);
        verdict("clear_empties_the_grid_into_the_neighbor_first",
                be.grid.isEmpty() && count(chest, Items.STICK) == 3 && count(chest, Items.OAK_PLANKS) == 209 && count(player.getInventory(), Items.OAK_PLANKS) == 0,
                "gridEmpty=" + be.grid.isEmpty() + " chestPlanks=" + count(chest, Items.OAK_PLANKS) + " chestSticks=" + count(chest, Items.STICK));
        // Craft All.
        area(p);
        be = station(p);
        chest = chest(p.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
        planks2x2(be, 1);
        menu = open(be);
        menu.clickMenuButton(player, StationMenu.BTN_CRAFT_ALL);
        verdict("craft_all_crafts_everything_it_can", count(player.getInventory(), Items.CRAFTING_TABLE) == 3 && count(chest, Items.OAK_PLANKS) == 0 && be.grid.isEmpty(),
                "tables=" + count(player.getInventory(), Items.CRAFTING_TABLE) + " (12 planks make 3)");
    }

    private static void disabledButtons(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        StationMenu menu = open(be);
        be.grid.setItem(0, new ItemStack(Items.STICK));
        Settings.set("buttons.rotate", false);
        boolean refused = !menu.clickMenuButton(player, StationMenu.BTN_ROTATE);
        verdict("a_disabled_button_is_refused_by_the_server", refused && be.grid.getItem(0).is(Items.STICK) && be.grid.getItem(2).isEmpty(), "refused=" + refused);
        Settings.reset();
        verdict("unknown_button_ids_are_refused", !menu.clickMenuButton(player, 77), "ok");
    }

    private static void syncedNumbers(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        Container chest = chest(p.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 20));
        planks2x2(be, 1);
        StationMenu menu = open(be);
        verdict("menu_reports_the_storage_stock_and_crafts", menu.storages() == 1 && menu.stockAt(0) == 20 && menu.stockAt(2) == 0 && menu.craftsLeft() == 6
                && menu.refillsFromNeighbors(), "storages=" + menu.storages() + " stock0=" + menu.stockAt(0) + " crafts=" + menu.craftsLeft());
    }

    private static void connectedList(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        Container chest = chest(p.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 20));
        chest.setItem(5, new ItemStack(Items.STICK, 2));
        level.setBlock(p.below(), Blocks.BARREL.defaultBlockState(), 3);
        StationMenu menu = open(be);
        verdict("connected_list_names_each_storage_side_block_and_fill",
                menu.storages() == 2 && menu.neighborSide(0) == 0 && menu.neighborBlock(0) == Blocks.BARREL
                        && menu.neighborSide(1) == 5 && menu.neighborBlock(1) == Blocks.CHEST && menu.neighborUsed(1) == 2 && menu.neighborTotal(1) == 27
                        && menu.neighborSide(2) == -1,
                "n=" + menu.storages() + " first=" + menu.neighborSide(0) + "/" + menu.neighborBlock(0) + " second=" + menu.neighborSide(1) + "/" + menu.neighborBlock(1)
                        + " used=" + menu.neighborUsed(1) + "/" + menu.neighborTotal(1));
        // The panel shows the selected storage: the barrel (tab 0) first, then the chest after its tab is picked.
        int base = StationMenu.FIRST_PLAYER + 36;
        boolean barrelEmpty = menu.slots.get(base).getItem().isEmpty() && menu.selectedTab() == 0;
        menu.clickMenuButton(player, StationMenu.BTN_TAB + 1);
        menu.broadcastChanges();
        boolean planks = menu.selectedTab() == 1 && menu.slots.get(base).getItem().is(Items.OAK_PLANKS) && menu.slots.get(base).getItem().getCount() == 20;
        boolean sticks = menu.slots.get(base + 5).getItem().is(Items.STICK);
        boolean readOnly = !menu.slots.get(base).mayPickup(player) && !menu.slots.get(base).mayPlace(new ItemStack(Items.DIRT));
        verdict("tabs_switch_the_shown_storage_and_slots_are_read_only", barrelEmpty && planks && sticks && readOnly,
                "barrelEmpty=" + barrelEmpty + " planks=" + planks + " sticks=" + sticks + " readOnly=" + readOnly);
        menu.clicked(base, 0, ContainerInput.QUICK_MOVE, player);
        menu.clicked(base, 0, ContainerInput.PICKUP, player);
        verdict("clicking_a_connected_slot_changes_nothing", count(chest, Items.OAK_PLANKS) == 20 && menu.getCarried().isEmpty() && count(player.getInventory(), Items.OAK_PLANKS) == 0,
                "chest=" + count(chest, Items.OAK_PLANKS));
        menu.clickMenuButton(player, StationMenu.BTN_SCROLL + 1 * 10000 + 5);
        menu.broadcastChanges();
        verdict("scroll_is_clamped_for_a_storage_that_fits", menu.viewScroll() == 0 && menu.slots.get(base).getItem().is(Items.OAK_PLANKS), "scroll=" + menu.viewScroll());
        menu.clickMenuButton(player, StationMenu.BTN_TAB + 5);
        menu.broadcastChanges();
        verdict("a_tab_past_the_last_storage_falls_back_to_the_last_one", menu.selectedTab() == 1, "tab=" + menu.selectedTab());
        level.setBlock(p.below(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static void resultEdgeCases(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        be.grid.setItem(0, new ItemStack(Items.DIAMOND));
        StationMenu menu = open(be);
        verdict("no_recipe_means_an_empty_result", menu.slots.get(StationMenu.RESULT).getItem().isEmpty(), "result=" + menu.slots.get(StationMenu.RESULT).getItem());
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.PICKUP, player);
        verdict("clicking_an_empty_result_changes_nothing", menu.getCarried().isEmpty() && be.grid.getItem(0).is(Items.DIAMOND), "carried=" + menu.getCarried());
        planks2x2(be, 1);
        be.grid.setItem(0, new ItemStack(Items.OAK_PLANKS));
        be.grid.removeItemNoUpdate(2);
        menu.broadcastChanges();
        menu.setCarried(new ItemStack(Items.DIRT, 3));
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.PICKUP, player);
        verdict("a_different_carried_item_blocks_the_craft", menu.getCarried().is(Items.DIRT) && count(be.grid, Items.OAK_PLANKS) == 4, "carried=" + menu.getCarried() + " grid=" + count(be.grid, Items.OAK_PLANKS));
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(StationMenu.RESULT, 1, ContainerInput.PICKUP, player);
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.THROW, player);
        verdict("right_click_crafts_once_and_other_click_types_are_ignored", menu.getCarried().is(Items.CRAFTING_TABLE) && menu.getCarried().getCount() == 1,
                "carried=" + menu.getCarried());
        verdict("preview_matches_what_a_craft_gives", Crafter.preview(level, be.grid).isEmpty() == be.grid.isEmpty(), "ok");
    }

    /** The screenshot tour's setup: a chest with three item types, a barrel, and a grid that holds a stack of one ingredient. */
    private static void pickaxeConservation(BlockPos p) {
        area(p);
        StationBlockEntity be = station(p);
        Container chest = chest(p.east());
        level.setBlock(p.west(), Blocks.BARREL.defaultBlockState(), 3);
        Container barrel = (Container) level.getBlockEntity(p.west());
        barrel.setItem(0, new ItemStack(Items.IRON_INGOT, 24));
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        chest.setItem(1, new ItemStack(Items.STICK, 32));
        chest.setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        for (int i = 0; i < 3; i++) {
            be.grid.setItem(i, new ItemStack(Items.COBBLESTONE, 2));
        }
        be.grid.setItem(4, new ItemStack(Items.STICK, 1));
        be.grid.setItem(7, new ItemStack(Items.STICK, 1));
        StationMenu menu = open(be);
        menu.clicked(StationMenu.RESULT, 0, ContainerInput.QUICK_MOVE, player);
        int picks = count(player.getInventory(), Items.STONE_PICKAXE);
        int cobble = count(player.getInventory(), Items.COBBLESTONE) + count(be.grid, Items.COBBLESTONE) + count(chest, Items.COBBLESTONE) + count(barrel, Items.COBBLESTONE);
        int sticks = count(player.getInventory(), Items.STICK) + count(be.grid, Items.STICK) + count(chest, Items.STICK);
        verdict("pickaxe_craft_conserves_cobblestone_and_sticks_and_stops_at_a_different_recipe", cobble + picks * 3 == 70 && sticks + picks * 2 == 34 && count(player.getInventory(), Items.COBBLESTONE_SLAB) == 0,
                "picks=" + picks + " cobble=" + cobble + " (expect " + (70 - picks * 3) + ") sticks=" + sticks + " (expect " + (34 - picks * 2) + ")");
    }
}
