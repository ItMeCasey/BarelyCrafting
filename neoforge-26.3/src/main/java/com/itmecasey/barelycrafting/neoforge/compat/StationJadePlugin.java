package com.itmecasey.barelycrafting.neoforge.compat;

import com.itmecasey.barelycrafting.BarelyCrafting;
import com.itmecasey.barelycrafting.block.StationBlock;
import com.itmecasey.barelycrafting.compat.LookInfo;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade: the lines of the Crafting Station (connected storages, last craft). The server works the lines out (see {@link LookInfo})
 * and sends them with the data of Jade; the client only prints them. Jade finds this class by its annotation and only when Jade
 * is installed, so the mod runs the same without it.
 */
@WailaPlugin
public class StationJadePlugin implements IWailaPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(BarelyCrafting.MODID, "look_info");
    private static final Identifier DATA_UID = Identifier.fromNamespaceAndPath(BarelyCrafting.MODID, "look_info_data");
    private static final String TAG = "barelycrafting_lines";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, StationBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Tooltip.INSTANCE, StationBlock.class);
    }

    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity entity = accessor.getBlockEntity();
            if (entity != null && accessor.getLevel() instanceof ServerLevel level) {
                data.store(TAG, ComponentSerialization.CODEC.listOf(), LookInfo.lines(level, entity));
            }
        }

        @Override
        public Identifier getUid() {
            return DATA_UID;
        }
    }

    private enum Tooltip implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            List<Component> lines = accessor.getServerData().read(TAG, ComponentSerialization.CODEC.listOf()).orElse(List.of());
            lines.forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
