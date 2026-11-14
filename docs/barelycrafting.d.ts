// Editor typings for the BarelyCrafting global (ModJS and KubeJS server scripts). Copy next to your scripts or into modjs/.
// Java strings come back as Java objects in some script engines: compare them as String(e.player()) == "name".

interface BarelyCraftingStorage {
    /** down, up, north, south, west or east */
    side(): string;
    /** Block id such as minecraft:chest */
    block(): string;
    used(): number;
    total(): number;
}

interface BarelyCraftingStation {
    dimension(): string;
    x(): number;
    y(): number;
    z(): number;
    /** The nine grid slots, row by row, each "N namespace:item" or "" */
    grid(): string[];
    slot(index: number): string;
    /** What the grid crafts now, "N namespace:item", or "" */
    result(): string;
    storages(): BarelyCraftingStorage[];
}

interface BarelyCraftingEvent {
    /** beforeCraft, crafted or button */
    name(): string;
    station(): BarelyCraftingStation;
    player(): string;
    playerId(): string;
    /** Item id of the craft, "" for a button event */
    result(): string;
    /** Items one craft makes, 0 for a button event */
    count(): number;
    /** clear, rotate, mirror, balance, refill or craft_all; "" for a craft event */
    button(): string;
    /** Stops a beforeCraft or button event; does nothing on crafted */
    cancel(): void;
    isCancelled(): boolean;
}

interface BarelyCraftingApi {
    onBeforeCraft(listener: (event: BarelyCraftingEvent) => void): void;
    onCrafted(listener: (event: BarelyCraftingEvent) => void): void;
    onButton(listener: (event: BarelyCraftingEvent) => void): void;
    denyRecipe(recipeId: string): void;
    denyResult(itemId: string): void;
    allowOnly(recipeIds: string[]): void;
    /** Reads one value of barelycrafting.properties as text, "" for an unknown key */
    setting(key: string): string;
    /** Removes every listener and recipe rule scripts registered */
    reset(): void;
}

declare const BarelyCrafting: BarelyCraftingApi;
