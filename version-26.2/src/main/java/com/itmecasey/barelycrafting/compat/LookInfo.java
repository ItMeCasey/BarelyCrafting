package com.itmecasey.barelycrafting.compat;

import com.itmecasey.barelycrafting.Neighbors;
import com.itmecasey.barelycrafting.block.StationBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The lines the Crafting Station adds to look-at tooltips (Show Me What This Is and Jade), worked out on the server where the real
 * state lives: how many storages are connected and what the station crafted last. State only.
 */
public final class LookInfo {
    private LookInfo() {
    }

    /** The lines for a block entity; empty when it is not a Crafting Station. */
    public static List<Component> lines(ServerLevel level, BlockEntity entity) {
        List<Component> lines = new ArrayList<>();
        if (entity instanceof StationBlockEntity station) {
            lines.add(Component.translatable("tooltip.barelycrafting.look.storages", Neighbors.scanFull(level, station.getBlockPos()).size()));
            ItemStack last = station.lastCrafted;
            if (!last.isEmpty()) {
                lines.add(Component.translatable("tooltip.barelycrafting.look.last_craft", last.getCount(), last.getHoverName()));
            }
        }
        return lines;
    }
}
