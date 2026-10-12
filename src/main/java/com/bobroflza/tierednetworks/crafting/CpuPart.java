package com.bobroflza.tierednetworks.crafting;

/** The blocks a Crafting CPU is built from. */
public enum CpuPart {
    UNIT("crafting_unit", 0, 0, 1),
    STORAGE_1K("crafting_storage_1k", 1_024, 0, 1),
    STORAGE_4K("crafting_storage_4k", 4_096, 0, 2),
    STORAGE_16K("crafting_storage_16k", 16_384, 0, 3),
    STORAGE_64K("crafting_storage_64k", 65_536, 0, 4),
    CO_PROCESSOR("crafting_coprocessor", 0, 1, 4),
    MONITOR("crafting_monitor", 0, 0, 1);

    private final String id;
    private final long storage;
    private final int coprocessors;
    private final int powerCost;

    CpuPart(String id, long storage, int coprocessors, int powerCost) {
        this.id = id;
        this.storage = storage;
        this.coprocessors = coprocessors;
        this.powerCost = powerCost;
    }

    public String id() { return id; }
    /** Items this block lets a job hold. */
    public long storage() { return storage; }
    public int coprocessors() { return coprocessors; }
    public int powerCost() { return powerCost; }
}
