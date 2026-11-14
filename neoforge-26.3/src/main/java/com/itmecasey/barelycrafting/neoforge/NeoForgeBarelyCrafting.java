package com.itmecasey.barelycrafting.neoforge;

import com.itmecasey.barelycrafting.BarelyCrafting;
import com.itmecasey.barelycrafting.ExternalInventory;
import com.itmecasey.barelycrafting.Inventories;
import com.itmecasey.barelycrafting.ModContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** NeoForge entry point for Minecraft 26.x. Registers the content and teaches the station to read NeoForge item handlers. */
@Mod(BarelyCrafting.MODID)
public final class NeoForgeBarelyCrafting {
    public NeoForgeBarelyCrafting(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        boolean client = net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT;
        BarelyCrafting.init(FMLPaths.CONFIGDIR::get, client, container.getModInfo().getVersion().toString(), org.slf4j.LoggerFactory.getLogger("BarelyCrafting")::info);
        NeoForgeConfig.register(container, modBus, client);
        if (client) {
            NeoForgeClient.installConfigScreen(container);
        }
        modBus.addListener(NeoForgeBarelyCrafting::register);
        com.itmecasey.barelycrafting.Crafter.craftedHook = net.neoforged.neoforge.event.EventHooks::firePlayerCraftingEvent;
        Inventories.loaderFinder = (level, pos, face) -> {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, face);
            return handler == null ? null : new HandlerView(handler);
        };
        com.itmecasey.barelycrafting.Dev.call("com.itmecasey.barelycrafting.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModContent.registerBlockEntities((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.MENU, helper -> ModContent.registerMenus((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    /** A NeoForge item handler seen as an {@link ExternalInventory}. */
    private record HandlerView(ResourceHandler<ItemResource> handler) implements ExternalInventory {
        @Override
        public int slots() {
            return handler.size();
        }

        @Override
        public ItemStack get(int slot) {
            ItemResource r = handler.getResource(slot);
            int n = handler.getAmountAsInt(slot);
            return r.isEmpty() || n <= 0 ? ItemStack.EMPTY : r.toStack(n);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            try (Transaction tx = Transaction.openRoot()) {
                int in = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
                if (in > 0 && !simulate) {
                    tx.commit();
                }
                return in >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - in);
            }
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            ItemResource r = handler.getResource(slot);
            if (r.isEmpty()) {
                return ItemStack.EMPTY;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int out = handler.extract(slot, r, amount, tx);
                if (out > 0 && !simulate) {
                    tx.commit();
                }
                return out <= 0 ? ItemStack.EMPTY : r.toStack(out);
            }
        }
    }
}
