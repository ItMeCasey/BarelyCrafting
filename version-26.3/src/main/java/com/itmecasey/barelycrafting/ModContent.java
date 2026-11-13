package com.itmecasey.barelycrafting;

import com.itmecasey.barelycrafting.block.StationBlock;
import com.itmecasey.barelycrafting.block.StationBlockEntity;
import com.itmecasey.barelycrafting.menu.StationMenu;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Every block, item, block entity type and menu type the mod adds, built once and handed to the loader's registry event. */
public final class ModContent {
    public static final String MODID = BarelyCrafting.MODID;

    public static final Block STATION_BLOCK = new StationBlock(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, id("crafting_station")))
            .mapColor(MapColor.WOOD).strength(2.5F, 3.0F).sound(SoundType.WOOD));
    public static final Item STATION_ITEM = new BlockItem(STATION_BLOCK, new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id("crafting_station"))).useBlockDescriptionPrefix());
    public static final BlockEntityType<StationBlockEntity> STATION = new BlockEntityType<>(StationBlockEntity::new, Set.of(STATION_BLOCK));
    public static final MenuType<StationMenu> STATION_MENU = new MenuType<>(StationMenu::new, FeatureFlags.VANILLA_SET);

    private ModContent() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BarelyCrafting.MODID, path);
    }

    public static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.barelycrafting"))
                .icon(() -> new ItemStack(STATION_ITEM))
                .displayItems((params, output) -> output.accept(STATION_ITEM))
                .build();
    }

    public static void registerBlocks(BiConsumer<String, Block> out) {
        out.accept("crafting_station", STATION_BLOCK);
    }

    public static void registerItems(BiConsumer<String, Item> out) {
        out.accept("crafting_station", STATION_ITEM);
    }

    public static void registerBlockEntities(BiConsumer<String, BlockEntityType<?>> out) {
        out.accept("crafting_station", STATION);
    }

    public static void registerMenus(BiConsumer<String, MenuType<?>> out) {
        out.accept("crafting_station", STATION_MENU);
    }
}
