package com.itmecasey.barelycrafting;

import net.minecraft.world.item.ItemStack;

/** An item with its components and no count: two keys are equal when the stacks would merge. */
public final class StackKey {
    private final ItemStack stack;

    public StackKey(ItemStack stack) {
        this.stack = stack.copyWithCount(1);
    }

    /** A fresh single-item stack of this key. */
    public ItemStack stack() {
        return stack.copy();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StackKey other && ItemStack.isSameItemSameComponents(stack, other.stack);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(stack);
    }
}
