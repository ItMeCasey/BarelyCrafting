package com.itmecasey.barelycrafting.compat;

import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.SmwtiPlugin;

/**
 * The Crafting Station lines in Show Me What This Is: connected storages and the last craft. SMWTI finds this class by its
 * annotation, so nothing here is loaded unless SMWTI is installed.
 */
@SmwtiPlugin
public class StationSmwtiPlugin implements ISmwtiPlugin {
    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> LookInfo.lines(level, entity).forEach(tooltip::add));
    }
}
