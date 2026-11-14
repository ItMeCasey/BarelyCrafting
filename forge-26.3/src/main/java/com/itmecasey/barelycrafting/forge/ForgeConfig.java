package com.itmecasey.barelycrafting.forge;

import com.itmecasey.barelycrafting.Settings;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * The Forge config files, built from the one key list in {@link Settings#KEYS}: {@code barelycrafting-common.toml} (everything the
 * server decides) and {@code barelycrafting-client.toml} (the screen theme). A loaded or reloaded config pushes its values into
 * {@link Settings}, which the game code reads.
 */
final class ForgeConfig {
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> COMMON = new LinkedHashMap<>();
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> CLIENT = new LinkedHashMap<>();
    private static ForgeConfigSpec commonSpec;
    private static ForgeConfigSpec clientSpec;

    private ForgeConfig() {
    }

    static void register(FMLJavaModLoadingContext context, boolean client) {
        commonSpec = build(false, COMMON);
        context.registerConfig(ModConfig.Type.COMMON, commonSpec, "barelycrafting-common.toml");
        if (client) {
            clientSpec = build(true, CLIENT);
            context.registerConfig(ModConfig.Type.CLIENT, clientSpec, "barelycrafting-client.toml");
        }
        ModConfigEvent.Loading.getBus(context.getModBusGroup()).addListener(e -> push(e.getConfig()));
        ModConfigEvent.Reloading.getBus(context.getModBusGroup()).addListener(e -> push(e.getConfig()));
    }

    private static ForgeConfigSpec build(boolean clientKeys, Map<String, ForgeConfigSpec.ConfigValue<?>> into) {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        for (Settings.Key k : Settings.KEYS) {
            if (k.client() != clientKeys) {
                continue;
            }
            b.comment(k.describe()).translation("barelycrafting.configuration." + k.path());
            into.put(k.path(), switch (k.kind()) {
                case BOOL -> b.define(k.path(), (boolean) k.def());
                case INT -> b.defineInRange(k.path(), (int) k.def(), k.min(), k.max());
                case CHOICE -> b.defineInList(k.path(), (String) k.def(), k.choices());
                case FACES -> b.define(k.path(), (String) k.def(), o -> o instanceof String s && Settings.validFaces(s));
            });
        }
        return b.build();
    }

    private static void push(ModConfig config) {
        Map<String, ForgeConfigSpec.ConfigValue<?>> values = config.getSpec() == commonSpec ? COMMON : config.getSpec() == clientSpec ? CLIENT : null;
        if (values != null) {
            values.forEach((path, value) -> Settings.set(path, value.get()));
        }
    }
}
