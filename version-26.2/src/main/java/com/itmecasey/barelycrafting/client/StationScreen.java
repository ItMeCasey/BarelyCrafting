package com.itmecasey.barelycrafting.client;

import com.itmecasey.barelycrafting.Settings;
import com.itmecasey.barelycrafting.menu.StationMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The Crafting Station screen: a column of tweak buttons on the left, the 3x3 grid, the result, and lines that say where
 * ingredients come from. Drawn with plain fills in the familiar gray look, so it needs no background texture.
 */
public class StationScreen extends AbstractContainerScreen<StationMenu> {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 198;
    private static final int TEXT = 0xFF404040;
    private static final int GREEN = 0xFF1E6B2E;
    private static final int BLUE = 0xFF2A5FD0;

    public StationScreen(StationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = StationMenu.PLAYER_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        // Keep the Connected panel on screen: when the window is narrow the whole screen moves right just enough.
        int needed = VIEW_W + 4;
        if (leftPos < needed) {
            leftPos = Math.min(needed, Math.max(leftPos, width - imageWidth - 2));
        }
        List<Spec> specs = new ArrayList<>();
        specs.add(new Spec("clear", "clear", StationMenu.BTN_CLEAR, "buttons.clear", () -> !menu.gridEmpty()));
        specs.add(new Spec("rotate", "rotate", StationMenu.BTN_ROTATE, "buttons.rotate", () -> !menu.gridEmpty()));
        specs.add(new Spec("mirror", "mirror", StationMenu.BTN_MIRROR, "buttons.mirror", () -> !menu.gridEmpty()));
        specs.add(new Spec("balance", "balance", StationMenu.BTN_BALANCE, "buttons.balance", () -> !menu.gridEmpty()));
        specs.add(new Spec("refill", "refill", StationMenu.BTN_REFILL, "buttons.refill", () -> !menu.gridEmpty()));
        specs.add(new Spec("craft_all", "craft_all", StationMenu.BTN_CRAFT_ALL, "buttons.craft_all", menu::resultReady));
        int row = 0;
        for (Spec s : specs) {
            if (!Settings.getBool(s.setting)) {
                continue;
            }
            int id = s.id;
            addRenderableWidget(new IconButton(leftPos + 8, topPos + 18 + row * 13, s.icon,
                    Component.translatable("gui.barelycrafting.tip_" + s.key), s.usable, () -> press(id)));
            row++;
        }
    }

    /** Where the main panel really sits (the tour aims the mouse with it). */
    public int guiLeft() {
        return leftPos;
    }

    public int guiTop() {
        return topPos;
    }

    private record Spec(String key, String icon, int id, String setting, java.util.function.BooleanSupplier usable) {
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // ---- Drawing ----

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + 4, y + h - 3, 0xFFFFFFFF);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 2, y + 2, x + 3, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, 0xFF555555);
        g.fill(x + w - 4, y + 3, x + w - 3, y + h - 3, 0xFF555555);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFF555555);
        g.fill(x + w - 3, y + 2, x + w - 2, y + h - 2, 0xFF555555);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFFC6C6C6);
    }

    /** An inset square: dark on top and left, white on bottom and right, gray inside. */
    private static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFFFFFFF);
        g.fill(x, y, x + w - 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF8B8B8B);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        panel(g, leftPos, topPos, imageWidth, imageHeight);
        drawConnected(g);
        for (Slot s : menu.slots) {
            if (!s.isActive()) {
                continue;
            }
            if (s.index == StationMenu.RESULT) {
                inset(g, leftPos + s.x - 5, topPos + s.y - 5, 26, 26);
            }
            inset(g, leftPos + s.x - 1, topPos + s.y - 1, 18, 18);
        }
        // The arrow from the grid to the result.
        int ax = leftPos + 100;
        int ay = topPos + 41;
        g.fill(ax, ay + 2, ax + 14, ay + 8, 0xFF8B8B8B);
        for (int i = 0; i < 6; i++) {
            g.fill(ax + 14 + i, ay + i, ax + 15 + i, ay + 10 - i, 0xFF8B8B8B);
        }
        // Where ingredients come from.
        int x = leftPos + StationMenu.GRID_X;
        int y = topPos + 77;
        int storages = menu.storages();
        g.text(font, Component.translatable(storages > 0 ? "gui.barelycrafting.storages" : "gui.barelycrafting.no_storages", storages), x, y, TEXT, false);
        if (menu.resultReady()) {
            g.text(font, Component.translatable("gui.barelycrafting.crafts_left", menu.craftsLeft()), x, y + 9, TEXT, false);
            if (menu.refillsFromNeighbors()) {
                g.text(font, Component.translatable("gui.barelycrafting.from_neighbors"), x, y + 18, GREEN, false);
            }
        }
    }

    private static final String[] SIDES = {"down", "up", "north", "south", "west", "east"};
    private static final int VIEW_W = 178;

    /**
     * The panel left of the screen: the real contents of the connected storages (their first 27 slots each, up to four storages),
     * under a header with the block, the side it touches and how many of its slots are in use.
     */
    private void drawConnected(GuiGraphicsExtractor g) {
        int n = menu.storages();
        int shown = Math.min(n, StationMenu.VIEW_SECTIONS);
        int x = leftPos - VIEW_W - 2;
        int y = topPos;
        int h = n == 0 ? 36 : 20 + shown * StationMenu.SECTION_H + (n > shown ? 12 : 0);
        panel(g, x, y, VIEW_W, h);
        g.text(font, Component.translatable("gui.barelycrafting.connected"), x + 8, y + 7, TEXT, false);
        if (n == 0) {
            g.text(font, Component.translatable("gui.barelycrafting.no_storages"), x + 62, y + 7, 0xFF6A6A6A, false);
            return;
        }
        for (int i = 0; i < shown; i++) {
            int top = y + 20 + i * StationMenu.SECTION_H;
            int side = menu.neighborSide(i);
            net.minecraft.world.level.block.Block block = menu.neighborBlock(i);
            g.item(new net.minecraft.world.item.ItemStack(block.asItem()), x + 7, top - 1);
            String where = side >= 0 && side < 6 ? Component.translatable("gui.barelycrafting.side_" + SIDES[side]).getString() : "";
            String name = block.getName().getString();
            String info = where + "  " + menu.neighborUsed(i) + "/" + menu.neighborTotal(i) + (menu.neighborTotal(i) > StationMenu.VIEW_PER ? " (first 27)" : "");
            g.text(font, name, x + 27, top, TEXT, false);
            g.text(font, info, x + 27, top + 9, 0xFF6A6A6A, false);
        }
        if (n > shown) {
            g.text(font, Component.translatable("gui.barelycrafting.more_storages", n - shown), x + 8, y + 22 + shown * StationMenu.SECTION_H - 4, 0xFF6A6A6A, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mx, int my) {
        super.extractSlot(g, slot, mx, my);
        if (slot.index < StationMenu.GRID && slot.hasItem() && menu.stockAt(slot.index) > 0) {
            // A small blue chest mark in the corner: the neighbors hold more of this item.
            g.nextStratum();
            int x = slot.x;
            int y = slot.y;
            g.fill(x, y, x + 5, y + 5, 0xFF000000);
            g.fill(x + 1, y + 1, x + 4, y + 4, BLUE);
            g.fill(x + 1, y + 2, x + 4, y + 3, 0xFFFFFFFF);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mx, int my) {
        if (hoveredSlot != null && hoveredSlot.index < StationMenu.GRID && hoveredSlot.hasItem() && menu.getCarried().isEmpty()
                && menu.stockAt(hoveredSlot.index) > 0) {
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));
            lines.add(Component.translatable("gui.barelycrafting.in_neighbors", menu.stockAt(hoveredSlot.index)));
            g.setComponentTooltipForNextFrame(font, lines, mx, my);
            return;
        }
        super.extractTooltip(g, mx, my);
    }
}
