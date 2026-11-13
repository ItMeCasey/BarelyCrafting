package com.itmecasey.barelycrafting.forge;

import com.itmecasey.barelycrafting.ModContent;
import com.itmecasey.barelycrafting.client.StationScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup: the screen. The event only fires on a client. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void install(FMLJavaModLoadingContext context) {
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(ForgeClient::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModContent.STATION_MENU, StationScreen::new));
    }
}
