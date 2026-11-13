package com.itmecasey.barelycrafting.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.SimpleContainer;

/** The 3x3 grid's container. Open screens register a callback to learn about every change, so each recomputes its own result. */
public class GridContainer extends SimpleContainer {
    private final List<Runnable> listeners = new ArrayList<>();
    private final Runnable owner;

    public GridContainer(Runnable owner) {
        super(9);
        this.owner = owner;
    }

    public void listen(Runnable listener) {
        listeners.add(listener);
    }

    public void unlisten(Runnable listener) {
        listeners.remove(listener);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        owner.run();
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
