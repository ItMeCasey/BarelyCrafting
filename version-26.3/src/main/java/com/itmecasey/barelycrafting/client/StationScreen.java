package com.itmecasey.barelycrafting.client;

import com.itmecasey.barelycrafting.Settings;
import com.itmecasey.barelycrafting.ViewMath;
import com.itmecasey.barelycrafting.menu.StationMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
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
            addRenderableWidget(new IconButton(leftPos + 7 + (row % 2) * 18, topPos + StationMenu.GRID_Y + (row / 2) * 19, s.icon,
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
        int ax = leftPos + 105;
        int ay = topPos + 41;
        g.fill(ax, ay + 2, ax + 14, ay + 8, 0xFF8B8B8B);
        for (int i = 0; i < 6; i++) {
            g.fill(ax + 14 + i, ay + i, ax + 15 + i, ay + 10 - i, 0xFF8B8B8B);
        }
    }

    private static final String[] SIDES = {"down", "up", "north", "south", "west", "east"};
    private static final int VIEW_W = 190;
    private static final int VIEW_H = StationMenu.VIEW_Y + StationMenu.VIEW_ROWS * 18 + 8;
    private static final int BAR_X = 174;
    private static final int BAR_H = StationMenu.VIEW_ROWS * 18;
    private boolean draggingBar;
    private int sentRow = -1;

    private int panelX() {
        return leftPos - VIEW_W - 2;
    }

    private int maxRow() {
        return ViewMath.maxRow(menu.neighborTotal(menu.selectedTab()), StationMenu.VIEW_COLS, StationMenu.VIEW_ROWS);
    }

    /** The panel left of the screen: a tab per connected storage (its block), and the selected storage's slots, scrollable. */
    private void drawConnected(GuiGraphicsExtractor g) {
        int n = menu.storages();
        int x = panelX();
        int y = topPos;
        panel(g, x, y, VIEW_W, n == 0 ? 36 : VIEW_H);
        if (n == 0) {
            g.text(font, Component.translatable("gui.barelycrafting.connected"), x + 8, y + 7, TEXT, false);
            return;
        }
        int sel = menu.selectedTab();
        for (int i = 0; i < n; i++) {
            int tx = x + 6 + i * 24;
            int ty = y + 5;
            g.fill(tx, ty, tx + 22, ty + 24, 0xFF000000);
            g.fill(tx + 1, ty + 1, tx + 21, ty + 23, i == sel ? 0xFFC6C6C6 : 0xFF8B8B8B);
            if (i == sel) {
                g.fill(tx + 1, ty + 1, tx + 21, ty + 2, 0xFFFFFFFF);
                g.fill(tx + 1, ty + 1, tx + 2, ty + 23, 0xFFFFFFFF);
            }
            g.item(new net.minecraft.world.item.ItemStack(menu.neighborBlock(i).asItem()), tx + 3, ty + 4);
        }
        int max = maxRow();
        if (max > 0) {
            int bx = x + BAR_X;
            int by = y + StationMenu.VIEW_Y;
            g.fill(bx, by, bx + 6, by + BAR_H, 0xFF373737);
            int handle = Math.max(10, BAR_H * StationMenu.VIEW_ROWS / (max + StationMenu.VIEW_ROWS));
            int hy = by + (BAR_H - handle) * menu.viewScroll() / max;
            g.fill(bx, hy, bx + 6, hy + handle, 0xFF000000);
            g.fill(bx + 1, hy + 1, bx + 5, hy + handle - 1, 0xFFC6C6C6);
            g.fill(bx + 1, hy + 1, bx + 5, hy + 2, 0xFFFFFFFF);
        }
    }

    /** The tab under the mouse, or -1. */
    private int tabAt(double mx, double my) {
        for (int i = 0; i < menu.storages(); i++) {
            int tx = panelX() + 6 + i * 24;
            if (mx >= tx && mx < tx + 22 && my >= topPos + 5 && my < topPos + 29) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mx, int my) {
        int tab = tabAt(mx, my);
        if (tab >= 0) {
            int side = menu.neighborSide(tab);
            String where = side >= 0 && side < 6 ? Component.translatable("gui.barelycrafting.side_" + SIDES[side]).getString() : "";
            java.util.List<Component> lines = new java.util.ArrayList<>();
            lines.add(menu.neighborBlock(tab).getName());
            lines.add(Component.literal(where + "  " + menu.neighborUsed(tab) + "/" + menu.neighborTotal(tab)));
            g.setComponentTooltipForNextFrame(font, lines, mx, my);
            return;
        }
        super.extractTooltip(g, mx, my);
    }

    private void scrollTo(int row) {
        row = Math.max(0, Math.min(row, maxRow()));
        if (sentRow == menu.viewScroll()) {
            sentRow = -1;
        }
        if (row != menu.viewScroll() && row != sentRow) {
            sentRow = row;
            press(StationMenu.BTN_SCROLL + menu.selectedTab() * 10000 + row);
        }
    }

    private void scrollFromMouse(double mouseY) {
        int max = maxRow();
        int handle = Math.max(10, BAR_H * StationMenu.VIEW_ROWS / (max + StationMenu.VIEW_ROWS));
        double frac = (mouseY - (topPos + StationMenu.VIEW_Y) - handle / 2.0) / Math.max(1, BAR_H - handle);
        scrollTo((int) Math.round(frac * max));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (menu.storages() > 0 && x >= panelX() && x < panelX() + VIEW_W && y >= topPos && y < topPos + VIEW_H) {
            int base = sentRow >= 0 && sentRow != menu.viewScroll() ? sentRow : menu.viewScroll();
            if (scrollY != 0) {
                scrollTo(base + (scrollY > 0 ? -1 : 1));
            }
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int n = menu.storages();
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                int tx = panelX() + 6 + i * 24;
                if (event.x() >= tx && event.x() < tx + 22 && event.y() >= topPos + 5 && event.y() < topPos + 29) {
                    press(StationMenu.BTN_TAB + i);
                    return true;
                }
            }
            int bx = panelX() + BAR_X;
            if (maxRow() > 0 && event.x() >= bx && event.x() < bx + 6 && event.y() >= topPos + StationMenu.VIEW_Y && event.y() < topPos + StationMenu.VIEW_Y + BAR_H) {
                draggingBar = true;
                scrollFromMouse(event.y());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingBar) {
            scrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingBar = false;
        return super.mouseReleased(event);
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
}
