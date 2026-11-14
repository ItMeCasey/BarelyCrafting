package com.itmecasey.barelycrafting;

import java.nio.file.Path;
import java.util.function.Supplier;

/** Entry point shared by every loader layer. The layers call {@link #init} from their mod constructor. */
public final class BarelyCrafting {
    public static final String MODID = "barelycrafting";

    private BarelyCrafting() {
    }

    public static void init(Supplier<Path> configDir) {
        Path config = configDir.get();
        Settings.loadFile(config.resolve(MODID + ".properties"));
        com.itmecasey.barelycrafting.script.Bridges.install(config.getParent());
    }
}
