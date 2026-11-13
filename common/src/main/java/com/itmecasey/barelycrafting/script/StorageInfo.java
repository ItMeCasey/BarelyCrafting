package com.itmecasey.barelycrafting.script;

/** One storage next to a station, as scripts see it (read only). */
public final class StorageInfo {
    private final String side;
    private final String block;
    private final int used;
    private final int total;

    public StorageInfo(String side, String block, int used, int total) {
        this.side = side;
        this.block = block;
        this.used = used;
        this.total = total;
    }

    /** down, up, north, south, west or east. */
    public String side() {
        return side;
    }

    /** The storage block's id, for example minecraft:chest. */
    public String block() {
        return block;
    }

    /** Slots that hold something. */
    public int used() {
        return used;
    }

    /** Slots in all. */
    public int total() {
        return total;
    }
}
