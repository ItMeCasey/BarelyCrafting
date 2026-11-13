package com.itmecasey.barelycrafting;

import com.itmecasey.barelycrafting.script.StationInfo;
import com.itmecasey.barelycrafting.script.StorageInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Builds the read-only {@link StationInfo} that ModJS script events carry. */
final class StationSnapshot {
    private StationSnapshot() {
    }

    static String describe(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    static StationInfo of(ServerLevel level, BlockPos pos, Container grid) {
        List<String> cells = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            cells.add(describe(grid.getItem(i)));
        }
        List<StorageInfo> storages = new ArrayList<>();
        for (Neighbors.Found f : Neighbors.scanFull(level, pos)) {
            ExternalInventory inv = f.inventory();
            int used = 0;
            for (int i = 0; i < inv.slots(); i++) {
                if (!inv.get(i).isEmpty()) {
                    used++;
                }
            }
            storages.add(new StorageInfo(f.side().name().toLowerCase(Locale.ROOT),
                    BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.relative(f.side())).getBlock()).toString(), used, inv.slots()));
        }
        return new StationInfo(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), cells,
                describe(Crafter.preview(level, grid)), storages);
    }
}
