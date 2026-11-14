package com.itmecasey.barelycrafting.neoforge;

import com.itmecasey.barelycrafting.Settings;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The NeoForge config files, built from the one key list in {@link Settings#KEYS}: {@code barelycrafting-common.toml} (everything the
 * server decides) and {@code barelycrafting-client.toml} (the screen theme). A loaded or reloaded config pushes its values into
 * {@link Settings}, which the game code reads.
 */
final class NeoForgeConfig {
    private static final Map<String, ModConfigSpec.ConfigValue<?>> COMMON = new LinkedHashMap<>();
    private static final Map<String, ModConfigSpec.ConfigValue<?>> CLIENT = new LinkedHashMap<>();
    private static ModConfigSpec commonSpec;
    private static ModConfigSpec clientSpec;

    private NeoForgeConfig() {
    }

    static void register(ModContainer container, IEventBus modBus, boolean client) {
        commonSpec = build(false, COMMON);
        container.registerConfig(ModConfig.Type.COMMON, commonSpec, "barelycrafting-common.toml");
        if (client) {
            clientSpec = build(true, CLIENT);
            container.registerConfig(ModConfig.Type.CLIENT, clientSpec, "barelycrafting-client.toml");
        }
        modBus.addListener(ModConfigEvent.Loading.class, e -> push(e.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, e -> push(e.getConfig()));
    }

    private static ModConfigSpec build(boolean clientKeys, Map<String, ModConfigSpec.ConfigValue<?>> into) {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
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
        Map<String, ModConfigSpec.ConfigValue<?>> values = config.getSpec() == commonSpec ? COMMON : config.getSpec() == clientSpec ? CLIENT : null;
        if (values != null) {
            values.forEach((path, value) -> Settings.set(path, value.get()));
        }
    }
}
