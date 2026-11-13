package com.itmecasey.barelycrafting;

import org.jetbrains.annotations.Nullable;

/** A slot-based inventory seen as plain keys and counts, so the pull and consumption rules need no game classes. */
public interface Slots<K> {
    int size();

    /** What the slot holds, or {@code null} when it is empty. */
    @Nullable
    K key(int slot);

    int count(int slot);

    /** Removes up to {@code amount} items from a slot and returns how many were actually removed. */
    int extract(int slot, int amount);
}
