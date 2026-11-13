package com.itmecasey.barelycrafting.script;

import java.util.List;

/**
 * A read-only snapshot of one station taken when an event fires: where it is, the nine grid slots (row by row, each "N namespace:item"
 * or "" when empty), the result the grid makes and the storages next to it.
 */
public final class StationInfo {
    private final String dimension;
    private final int x;
    private final int y;
    private final int z;
    private final List<String> grid;
    private final String result;
    private final List<StorageInfo> storages;

    public StationInfo(String dimension, int x, int y, int z, List<String> grid, String result, List<StorageInfo> storages) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.grid = List.copyOf(grid);
        this.result = result;
        this.storages = List.copyOf(storages);
    }

    public String dimension() {
        return dimension;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /** The nine grid slots, row by row. */
    public List<String> grid() {
        return grid;
    }

    /** One grid slot (0 to 8), or "" when out of range or empty. */
    public String slot(int index) {
        return index >= 0 && index < grid.size() ? grid.get(index) : "";
    }

    /** What the grid crafts right now ("N namespace:item"), or "" when it matches no recipe. */
    public String result() {
        return result;
    }

    public List<StorageInfo> storages() {
        return storages;
    }
}
