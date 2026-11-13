package com.itmecasey.barelycrafting.forge;

import com.itmecasey.barelycrafting.BarelyCrafting;
import com.itmecasey.barelycrafting.ExternalInventory;
import com.itmecasey.barelycrafting.Inventories;
import com.itmecasey.barelycrafting.ModContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.RegisterEvent;

/** Forge entry point for Minecraft 26.x. Registers the content and teaches the station to read Forge item handlers. */
@Mod(BarelyCrafting.MODID)
public final class ForgeBarelyCrafting {
    public ForgeBarelyCrafting(FMLJavaModLoadingContext context) {
        BarelyCrafting.init(FMLPaths.CONFIGDIR::get);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgeBarelyCrafting::register);
        ForgeClient.install(context);
        Inventories.loaderFinder = (level, pos, face) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                return null;
            }
            IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, face).orElse(null);
            return handler == null ? null : new HandlerView(handler);
        };
        com.itmecasey.barelycrafting.Dev.call("com.itmecasey.barelycrafting.forge.dev.ForgeDev", "install", context);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModContent.registerBlockEntities((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.MENU, helper -> ModContent.registerMenus((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    /** A Forge item handler seen as an {@link ExternalInventory}. */
    private record HandlerView(IItemHandler handler) implements ExternalInventory {
        @Override
        public int slots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack get(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return ItemHandlerHelper.insertItem(handler, stack, simulate);
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }
    }
}
