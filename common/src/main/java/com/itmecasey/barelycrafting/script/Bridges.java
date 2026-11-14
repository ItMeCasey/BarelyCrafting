package com.itmecasey.barelycrafting.script;

import java.nio.file.Path;

/** Installs the optional script bridges (ModJS and KubeJS). Called once from {@code BarelyCrafting.init}. */
public final class Bridges {
    /** The single API object every bridge hands to scripts. */
    public static final BarelyCraftingApi API = new BarelyCraftingApi();

    private Bridges() {
    }

    public static void install(Path gameDir) {
        ModJsBridge.install(API);
        KubeJsBridge.install(gameDir);
    }
}
