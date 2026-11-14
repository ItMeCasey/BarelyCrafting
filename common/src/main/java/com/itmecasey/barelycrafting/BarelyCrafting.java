package com.itmecasey.barelycrafting;

import java.nio.file.Path;
import java.util.function.Supplier;

/** Entry point shared by every loader layer. The layers call {@link #init} from their mod constructor. */
public final class BarelyCrafting {
    public static final String MODID = "barelycrafting";

    private BarelyCrafting() {
    }

    /**
     * Imports the values of the old settings file into the toml config files (only when those do not exist yet), then installs the
     * optional script bridges. The loader registers its config specs after this returns.
     */
    public static void init(Supplier<Path> configDir, boolean client, String modVersion, java.util.function.Consumer<String> log) {
        Path config = configDir.get();
        for (String line : ConfigMigration.migrate(config, client, modVersion)) {
            log.accept("Config migration: " + line);
        }
        com.itmecasey.barelycrafting.script.Bridges.install(config.getParent());
    }
}
