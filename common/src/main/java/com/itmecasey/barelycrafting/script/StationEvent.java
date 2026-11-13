package com.itmecasey.barelycrafting.script;

/**
 * What a script listener receives. {@code beforeCraft} and {@code button} events can be cancelled with {@link #cancel()}; a cancelled
 * craft makes nothing and a cancelled button does nothing.
 */
public final class StationEvent {
    private final String name;
    private final StationInfo station;
    private final String player;
    private final String playerId;
    private final String result;
    private final int count;
    private final String button;
    private boolean cancelled;

    StationEvent(String name, StationInfo station, String player, String playerId, String result, int count, String button) {
        this.name = name;
        this.station = station;
        this.player = player;
        this.playerId = playerId;
        this.result = result;
        this.count = count;
        this.button = button;
    }

    /** beforeCraft, crafted or button. */
    public String name() {
        return name;
    }

    public StationInfo station() {
        return station;
    }

    public String player() {
        return player;
    }

    public String playerId() {
        return playerId;
    }

    /** The item of the craft ("namespace:item"), or "" for a button event. */
    public String result() {
        return result;
    }

    /** How many items one craft makes (the result stack size), or 0 for a button event. */
    public int count() {
        return count;
    }

    /** clear, rotate, mirror, balance, refill or craft_all for a button event, otherwise "". */
    public String button() {
        return button;
    }

    /** Stops the craft or the button press. Does nothing on a {@code crafted} event, which has already happened. */
    public void cancel() {
        if (!name.equals(ScriptEvents.CRAFTED)) {
            cancelled = true;
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
