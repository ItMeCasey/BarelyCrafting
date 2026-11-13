package com.itmecasey.barelycrafting.client;

import com.itmecasey.barelycrafting.ModContent;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A small square button with a 10x10 pixel icon. The tooltip is the widget's own, so the game draws it in its tooltip layer on top
 * of everything else. A button whose action would do nothing right now is drawn dimmed and ignores clicks.
 */
final class IconButton extends AbstractWidget {
    static final int SIZE = 16;
    private final Identifier icon;
    private final Runnable action;
    private final BooleanSupplier usable;

    IconButton(int x, int y, String icon, Component tip, BooleanSupplier usable, Runnable action) {
        super(x, y, SIZE, SIZE, tip);
        this.icon = ModContent.id("textures/gui/icon/" + icon + ".png");
        this.action = action;
        this.usable = usable;
        setTooltip(Tooltip.create(tip));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (usable.getAsBoolean()) {
            action.run();
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = getX();
        int y = getY();
        boolean on = usable.getAsBoolean();
        boolean hover = isHovered() && on;
        g.fill(x, y, x + SIZE, y + SIZE, 0xFF000000);
        g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, hover ? 0xFFDDE4FF : 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + SIZE - 1, y + 2, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 2, y + SIZE - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + SIZE - 2, x + SIZE - 1, y + SIZE - 1, hover ? 0xFF7A83B8 : 0xFF555555);
        g.fill(x + SIZE - 2, y + 2, x + SIZE - 1, y + SIZE - 1, hover ? 0xFF7A83B8 : 0xFF555555);
        g.blit(RenderPipelines.GUI_TEXTURED, icon, x + 3, y + 3, 0.0F, 0.0F, 10, 10, 10, 10, 10, 10, on ? 0xFFFFFFFF : 0x66FFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
