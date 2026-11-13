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

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(ModContent.STATION_MENU, StationScreen::new);
    }
}
