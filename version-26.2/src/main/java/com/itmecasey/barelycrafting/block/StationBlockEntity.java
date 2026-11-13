package com.itmecasey.barelycrafting.block;

import com.itmecasey.barelycrafting.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds the 3x3 grid. The items stay here when the screen closes and drop only when the block is broken. */
public class StationBlockEntity extends BlockEntity {
    public final GridContainer grid = new GridContainer(this::setChanged);

    public StationBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.STATION, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // Every slot is written, empty ones included: the stock helper skips empty slots, which would shuffle a crafting pattern.
        ValueOutput.TypedOutputList<ItemStack> list = output.list("grid", ItemStack.OPTIONAL_CODEC);
        for (int i = 0; i < grid.getContainerSize(); i++) {
            list.add(grid.getItem(i));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int slot = 0;
        for (ItemStack stack : input.listOrEmpty("grid", ItemStack.OPTIONAL_CODEC)) {
            if (slot < grid.getContainerSize()) {
                grid.setItem(slot++, stack);
            }
        }
        for (; slot < grid.getContainerSize(); slot++) {
            grid.setItem(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, grid);
        }
    }
}
