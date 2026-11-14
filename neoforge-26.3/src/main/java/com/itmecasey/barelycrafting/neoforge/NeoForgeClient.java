package com.itmecasey.barelycrafting.neoforge;

import com.itmecasey.barelycrafting.BarelyCrafting;
import com.itmecasey.barelycrafting.ModContent;
import com.itmecasey.barelycrafting.client.StationScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only setup: the screen. Never loaded on a dedicated server. */
@EventBusSubscriber(modid = BarelyCrafting.MODID, value = Dist.CLIENT)
public final class NeoForgeClient {
    private NeoForgeClient() {
    }

    /** The in-game config screen of the loader (Mods list, Config button). */
    static void installConfigScreen(net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, net.neoforged.neoforge.client.gui.ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(ModContent.STATION_MENU, StationScreen::new);
    }
}
