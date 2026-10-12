package com.bobroflza.tierednetworks.storage;

public enum CellTier {
    K1("1k", 1_024L),
    K4("4k", 4_096L),
    K16("16k", 16_384L),
    K64("64k", 65_536L),
    K256("256k", 262_144L);

    /** Distinct item types any one cell can hold. */
    public static final int MAX_TYPES = 63;

    private final String name;
    private final long capacity;

    CellTier(String name, long capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() { return name; }

    /** Total items the cell can hold across all types. */
    public long getCapacity() { return capacity; }
}
