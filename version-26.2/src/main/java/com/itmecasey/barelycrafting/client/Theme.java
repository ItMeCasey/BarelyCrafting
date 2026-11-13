package com.itmecasey.barelycrafting.client;

import com.itmecasey.barelycrafting.Settings;

/** The two looks of the Crafting Station screens, picked by the {@code ui.theme} setting: "minecraft" (the default) or "modern". */
record Theme(boolean modern, int text, int title, int muted, int panelFace, int panelEdge, int panelHi, int panelLo, int slotFace, int slotDark, int slotLight,
        int btnFace, int btnHover, int btnEdge, int btnHoverEdge, int tabSel, int tabUnsel, int tabSelBorder, int tabBorder,
        int barTrack, int barFace, int barBorder, int barHi, int arrow, String iconSuffix) {
    static final Theme MINECRAFT = new Theme(false, 0xFF404040, 0xFF404040, 0xFF6A6A6A, 0xFFC6C6C6, 0xFF000000, 0xFFFFFFFF, 0xFF555555,
            0xFF8B8B8B, 0xFF373737, 0xFFFFFFFF, 0xFFC6C6C6, 0xFFDDE4FF, 0xFF000000, 0xFF000000, 0xFFC6C6C6, 0xFF8B8B8B, 0xFF000000, 0xFF000000,
            0xFF373737, 0xFFC6C6C6, 0xFF000000, 0xFFFFFFFF, 0xFF8B8B8B, "");
    static final Theme MODERN = new Theme(true, 0xFFF4F2FF, 0xFFF2B33D, 0xFFA9A4C8, 0xFF1A1830, 0xFF2E2A46, 0xFF25223D, 0xFF0F0E1A,
            0xFF0B0A14, 0xFF2E2A46, 0xFF2E2A46, 0xFF1A1830, 0xFF25223D, 0xFF2E2A46, 0xFFF2B33D, 0xFF25223D, 0xFF1A1830, 0xFFF2B33D, 0xFF2E2A46,
            0xFF0B0A14, 0xFF2E2A46, 0xFF2E2A46, 0xFFF2B33D, 0xFF3A3560, "_light");

    static Theme get() {
        return "modern".equalsIgnoreCase(Settings.getString("ui.theme")) ? MODERN : MINECRAFT;
    }
}
