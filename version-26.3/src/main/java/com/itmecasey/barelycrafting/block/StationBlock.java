package com.itmecasey.barelycrafting.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.itmecasey.barelycrafting.menu.StationMenu;
import org.jetbrains.annotations.Nullable;

/** The Crafting Station block: right-click opens the grid screen. */
public class StationBlock extends BaseEntityBlock {
    /** The pedestal as boxes in 1/16 units, matching the model: foot, column, cap, the table slab, and its rim and corner posts. */
    private static final VoxelShape SHAPE = Shapes.or(
            box(1, 0, 1, 15, 3, 15), box(4, 3, 4, 12, 6, 12), box(5, 6, 5, 11, 10, 11), box(2, 10, 2, 14, 12, 14),
            box(0, 12, 0, 16, 15, 16), box(0, 15, 0, 16, 16, 1), box(0, 15, 15, 16, 16, 16), box(0, 15, 1, 1, 16, 15), box(15, 15, 1, 16, 16, 15));

    public StationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StationBlockEntity be) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StationMenu(id, inv, be),
                    Component.translatable("block.barelycrafting.crafting_station")));
        }
        return InteractionResult.SUCCESS;
    }
}
