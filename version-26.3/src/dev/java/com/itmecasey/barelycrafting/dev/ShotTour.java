package com.itmecasey.barelycrafting.dev;

import com.itmecasey.barelycrafting.Dev;
import com.itmecasey.barelycrafting.ExternalInventory;
import com.itmecasey.barelycrafting.Inventories;
import com.itmecasey.barelycrafting.ModContent;
import com.itmecasey.barelycrafting.block.StationBlockEntity;
import com.itmecasey.barelycrafting.menu.StationMenu;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only camera tour: opens a fresh superflat world, builds a station with a chest beside it, and saves screenshots of
 * the block and the screen into {@code run/screenshots} as {@code barelycrafting_<name>.png}, then quits. Does nothing unless the
 * environment variable {@code BARELYCRAFTING_SHOTS} is set. It never touches the real mouse or keyboard: it calls the game's own
 * handlers (the mouse handler's move, container clicks, menu buttons) from inside the game.
 */
public final class ShotTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyCrafting");
    private static final boolean ENABLED = System.getenv("BARELYCRAFTING_SHOTS") != null;
    private static final String WORLD = "barelycrafting_shots";

    private enum Stage { WAITING_FOR_MENU, LOADING_WORLD, TOURING, DONE }

    private record Step(int delay, Runnable action) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static Stage stage = Stage.WAITING_FOR_MENU;
    private static int idle;
    private static int wait;
    private static int ground;
    private static boolean pauseOnLostFocus;

    private ShotTour() {
    }

    static {
        if ("modern".equals(System.getenv("BARELYCRAFTING_THEME"))) {
            com.itmecasey.barelycrafting.Settings.set("ui.theme", "modern");
        }
    }

    public static void tick() {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.mouseHandler.isMouseGrabbed()) {
                minecraft.mouseHandler.releaseMouse();
            }
            switch (stage) {
                case WAITING_FOR_MENU -> waitForMenu(minecraft);
                case LOADING_WORLD -> waitForWorld(minecraft);
                case TOURING -> runSteps();
                default -> {
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("BARELYCRAFTING-SHOTS failed", e);
            quit(minecraft);
        }
    }

    private static void quit(Minecraft minecraft) {
        minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
        LOGGER.info("BARELYCRAFTING-SHOTS DONE");
        stage = Stage.DONE;
        minecraft.stop();
    }

    private static void waitForMenu(Minecraft minecraft) {
        if (minecraft.gui.overlay() != null || minecraft.level != null || minecraft.gui.screen() == null || ++idle < 20) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;
        LevelStorageSource source = minecraft.getLevelSource();
        if (source.levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                LOGGER.warn("BARELYCRAFTING-SHOTS could not delete the old world", e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        stage = Stage.LOADING_WORLD;
        idle = 0;
        LOGGER.info("BARELYCRAFTING-SHOTS creating world");
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                registries -> registries.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null
                || minecraft.getSingleplayerServer() == null || ++idle < 40) {
            return;
        }
        ground = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        LOGGER.info("BARELYCRAFTING-SHOTS in the world, ground at y={}", ground);
        plan();
        stage = Stage.TOURING;
    }

    private static void runSteps() {
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.poll();
        if (step == null) {
            return;
        }
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    // ---- The tour ----

    private static StationMenu menu() {
        var screen = Minecraft.getInstance().gui.screen();
        return screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> c && c.getMenu() instanceof StationMenu m ? m : null;
    }

    private static void plan() {
        int y = ground;
        BlockPos station = new BlockPos(0, y, 0);
        then(0, () -> {
            server("gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule spawn_monsters false",
                    "difficulty peaceful", "time set noon", "weather clear", "kill @e[type=!player,type=!item]", "clear @p");
            player().getAbilities().flying = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        then(4, () -> {
            server("fill -4 " + (y - 1) + " -4 12 " + (y - 1) + " 4 minecraft:smooth_stone");
            server("setblock 7 " + y + " 0 barelycrafting:crafting_station");
            installBigStorage();
            server("setblock 0 " + y + " 1 minecraft:gold_block");
            server("setblock 0 " + y + " 0 barelycrafting:crafting_station", "setblock 1 " + y + " 0 minecraft:chest[facing=west]",
                    "setblock -1 " + y + " 0 minecraft:barrel[facing=up]");
        });
        then(10, () -> onServer(server -> {
            ServerLevel level = server.overworld();
            Container chest = (Container) level.getBlockEntity(new BlockPos(1, y, 0));
            chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
            chest.setItem(1, new ItemStack(Items.STICK, 32));
            chest.setItem(2, new ItemStack(Items.COBBLESTONE, 64));
            Container barrel = (Container) level.getBlockEntity(new BlockPos(-1, y, 0));
            barrel.setItem(0, new ItemStack(Items.IRON_INGOT, 24));
            StationBlockEntity be = (StationBlockEntity) level.getBlockEntity(station);
            // A pickaxe: three cobblestone on top, two sticks down the middle. One of each, so the rest comes from the chest.
            for (int i = 0; i < 3; i++) {
                be.grid.setItem(i, new ItemStack(Items.COBBLESTONE, 2));
            }
            be.grid.setItem(4, new ItemStack(Items.STICK, 1));
            be.grid.setItem(7, new ItemStack(Items.STICK, 1));
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            p.getInventory().add(new ItemStack(Items.OAK_PLANKS, 40));
        }));
        // The block, seen from outside.
        then(2, () -> moveTo(new Vec3(0.5, y + 2.2, -3.4), new Vec3(0.5, y + 0.5, 0.5)));
        shoot("station_block", 30);
        then(2, () -> moveTo(new Vec3(7.5, y + 3.6, -0.5), new Vec3(7.5, y + 0.4, 0.5)));
        shoot("station_top", 25);
        then(2, () -> moveTo(new Vec3(10.4, y + 1.7, -2.6), new Vec3(7.5, y + 0.6, 0.5)));
        shoot("station_side_ne", 25);
        then(2, () -> moveTo(new Vec3(4.6, y + 1.7, 3.4), new Vec3(7.5, y + 0.6, 0.5)));
        shoot("station_side_nw", 25);
        then(2, () -> server("item replace entity @p weapon.mainhand with barelycrafting:crafting_station"));
        then(2, () -> moveTo(new Vec3(0.5, y + 1.8, -3.0), new Vec3(0.5, y + 1.0, 0.5)));
        shoot("item_in_hand", 25);
        then(2, () -> Minecraft.getInstance().gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(player())));
        then(5, () -> hoverGuiAway());
        shoot("item_inventory", 30);
        then(2, () -> close());
        then(2, () -> moveTo(new Vec3(0.5, y + 2.2, -3.4), new Vec3(0.5, y + 0.5, 0.5)));
        // The screen.
        then(5, () -> open(station));
        shoot("gui_station", 40);
        // A tooltip over the button column: the mouse rests on the first button and the tooltip falls over the buttons below it.
        then(3, () -> hoverButton(0));
        shoot("gui_tooltip_over_buttons", 50);
        then(3, () -> hoverButton(2));
        shoot("gui_tooltip_mirror", 50);
        // Rotate with the real button, then craft everything through the result slot.
        then(3, () -> hoverGuiAway());
        then(3, () -> press(StationMenu.BTN_TAB + 2));
        shoot("gui_chest_tab", 25);
        then(3, () -> press(StationMenu.BTN_ROTATE));
        shoot("gui_after_rotate", 25);
        then(3, () -> press(StationMenu.BTN_ROTATE));
        then(3, () -> press(StationMenu.BTN_ROTATE));
        then(3, () -> press(StationMenu.BTN_ROTATE));
        then(3, () -> onServer(server -> {
            ServerLevel level = server.overworld();
            StationBlockEntity be = (StationBlockEntity) level.getBlockEntity(station);
            LOGGER.info("BARELYCRAFTING-SHOTS grid before craft: {} {} {} | {} | {}", be.grid.getItem(0), be.grid.getItem(1), be.grid.getItem(2), be.grid.getItem(4), be.grid.getItem(7));
        }));
        then(10, () -> clientClick(StationMenu.RESULT, ContainerInput.QUICK_MOVE));
        then(20, () -> onServer(server -> {
            ServerLevel level = server.overworld();
            StationBlockEntity be = (StationBlockEntity) level.getBlockEntity(station);
            Container chest = (Container) level.getBlockEntity(new BlockPos(1, y, 0));
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            int pick = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(Items.STONE_PICKAXE)) {
                    pick += p.getInventory().getItem(i).getCount();
                }
            }
            int cobble = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (chest.getItem(i).is(Items.COBBLESTONE)) {
                    cobble += chest.getItem(i).getCount();
                }
            }
            LOGGER.info("BARELYCRAFTING-SHOTS after shift-click: pickaxes={} cobbleInChest={} gridEmpty={}", pick, cobble, be.grid.isEmpty());
        }));
        shoot("gui_after_craft", 15);
        // The big storage: its tab, then the wheel scrolls its window down to row 12 and the mouse rests on an item.
        then(3, () -> press(StationMenu.BTN_TAB));
        then(15, () -> wheel(12));
        then(25, () -> hoverViewSlot(10));
        then(40, () -> LOGGER.info("BARELYCRAFTING-SHOTS big storage scroll row={} tab={} first={}", menu().viewScroll(), menu().selectedTab(), menu().slots.get(StationMenu.PLAYER_END).getItem()));
        shoot("gui_scrolled_big_storage", 10);
        then(3, () -> {
            var screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof com.itmecasey.barelycrafting.client.StationScreen scr) {
                moveMouseGui(scr.guiLeft() - 192 + 6 + 24 + 11, scr.guiTop() + 5 + 12);
            }
        });
        shoot("gui_tab_tooltip", 50);
        then(2, () -> close());
        then(20, () -> quit(Minecraft.getInstance()));
    }

    /** A dev-only 216-slot storage standing in for a modded one (vanilla has nothing over 54 slots). Read-only: nothing can be taken or put in. */
    private static final class BigStorage implements ExternalInventory {
        private static final net.minecraft.world.item.Item[] ITEMS = {Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.REDSTONE, Items.COAL, Items.BREAD, Items.APPLE, Items.ARROW};

        @Override
        public int slots() {
            return 216;
        }

        @Override
        public ItemStack get(int slot) {
            return new ItemStack(ITEMS[slot % ITEMS.length], slot % 63 + 1);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }

    private static void installBigStorage() {
        Inventories.Finder base = Inventories.loaderFinder;
        Inventories.loaderFinder = (level, pos, face) -> level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK) ? new BigStorage() : base.find(level, pos, face);
    }

    /** Scrolls the open screen with the mouse wheel through the screen's own handler, as a player would over the Connected panel. */
    private static void wheel(int clicks) {
        var screen = Minecraft.getInstance().gui.screen();
        if (screen instanceof com.itmecasey.barelycrafting.client.StationScreen station) {
            double x = station.guiLeft() - 192 + 60;
            double y = station.guiTop() + 100;
            for (int i = 0; i < clicks; i++) {
                station.mouseScrolled(x, y, 0.0, -1.0);
            }
        }
    }

    private static void hoverViewSlot(int k) {
        var screen = Minecraft.getInstance().gui.screen();
        var m = menu();
        if (m != null && screen instanceof com.itmecasey.barelycrafting.client.StationScreen station) {
            var slot = m.slots.get(StationMenu.PLAYER_END + k);
            moveMouseGui(station.guiLeft() + slot.x + 8, station.guiTop() + slot.y + 8);
        }
    }

    // ---- helpers ----

    private static void press(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        var m = menu();
        if (m != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(m.containerId, id);
        }
    }

    private static void clientClick(int slot, ContainerInput input) {
        Minecraft minecraft = Minecraft.getInstance();
        var m = menu();
        if (m != null) {
            minecraft.gameMode.handleContainerInput(m.containerId, slot, 0, input, minecraft.player);
        }
    }

    /** Moves the game's own mouse position onto the n-th button of the column, so its tooltip appears. */
    private static void hoverButton(int n) {
        Minecraft minecraft = Minecraft.getInstance();
        var screen = minecraft.gui.screen();
        if (screen == null) {
            return;
        }
        if (!(screen instanceof com.itmecasey.barelycrafting.client.StationScreen station)) {
            return;
        }
        int left = station.guiLeft();
        int top = station.guiTop();
        moveMouseGui(left + 15 + (n % 2) * 18 + 8, top + 33 + (n / 2) * 19 + 8);
    }

    private static void hoverGuiAway() {
        Minecraft minecraft = Minecraft.getInstance();
        var screen = minecraft.gui.screen();
        if (screen != null) {
            moveMouseGui(screen.width - 4, screen.height - 4);
        }
    }

    private static void moveMouseGui(int guiX, int guiY) {
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        double rawX = guiX * (double) window.getScreenWidth() / window.getGuiScaledWidth();
        double rawY = guiY * (double) window.getScreenHeight() / window.getGuiScaledHeight();
        try {
            for (Method m : minecraft.mouseHandler.getClass().getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("onMove") && (p.length == 3 || p.length == 5) && p[0] == long.class) {
                    m.setAccessible(true);
                    Object[] args = new Object[p.length];
                    args[0] = window.handle();
                    args[1] = rawX;
                    args[2] = rawY;
                    for (int i = 3; i < p.length; i++) {
                        args[i] = 0.0;
                    }
                    m.invoke(minecraft.mouseHandler, args);
                    return;
                }
            }
            LOGGER.error("BARELYCRAFTING-SHOTS no onMove on the mouse handler");
        } catch (ReflectiveOperationException e) {
            LOGGER.error("BARELYCRAFTING-SHOTS mouse move failed", e);
        }
    }

    private static void open(BlockPos pos) {
        onServer(server -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            ServerLevel level = server.overworld();
            var result = level.getBlockState(pos).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
            LOGGER.info("BARELYCRAFTING-SHOTS open {} result={} menu={}", pos, result, p.containerMenu.getClass().getSimpleName());
        });
    }

    private static void close() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null) {
            minecraft.gui.screen().onClose();
        }
    }

    static void moveTo(Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        server(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y - 1.62, eye.z, yaw, pitch));
        LocalPlayer player = player();
        player.snapTo(eye.x, eye.y - 1.62, eye.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    static void shoot(String name, int delay) {
        then(delay, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.hud.getChat().clearMessages(false);
            minecraft.gui.toastManager().clear();
        });
        then(2, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            LOGGER.info("BARELYCRAFTING-SHOTS {}", name);
            Screenshot.grab(minecraft.gameDirectory, "barelycrafting_" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> LOGGER.info("BARELYCRAFTING-SHOTS saved {}", name));
        });
    }

    static void then(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    static void onServer(java.util.function.Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> action.accept(server));
        }
    }

    static void server(String... commands) {
        onServer(server -> {
            for (String command : commands) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        });
    }

    static {
        if (Dev.ENABLED && ENABLED) {
            LOGGER.info("BARELYCRAFTING-SHOTS enabled");
        }
        // ModContent is referenced so a missing registration fails loudly at class load instead of mid-tour.
        if (ModContent.STATION_MENU == null) {
            throw new IllegalStateException("station menu missing");
        }
    }
}
