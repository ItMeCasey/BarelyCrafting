package com.itmecasey.barelycrafting;

/** Row arithmetic of the Connected panel's scrolling window, kept free of game classes so it can be tested. */
public final class ViewMath {
    private ViewMath() {
    }

    /** The last row the window can start at: total slots in rows of {@code cols}, minus the {@code rows} that fit. Never negative. */
    public static int maxRow(int total, int cols, int rows) {
        int all = (Math.max(0, total) + cols - 1) / cols;
        return Math.max(0, all - rows);
    }

    /** Keeps a requested first row inside 0 to {@link #maxRow}. */
    public static int clampRow(int row, int total, int cols, int rows) {
        return Math.max(0, Math.min(row, maxRow(total, cols, rows)));
    }
}
