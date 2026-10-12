package com.bobroflza.tierednetworks.grid;

import net.minecraft.ChatFormatting;

/**
 * Wireless Access Point tiers. A range of {@link #DIMENSION_WIDE} covers the AP's whole dimension,
 * {@link #ANY_DIMENSION} covers every dimension.
 */
public enum AccessPointTier {
    BASIC("basic", 16, 1, ChatFormatting.GRAY),
    ADVANCED("advanced", 64, 4, ChatFormatting.GOLD),
    ELITE("elite", 256, 16, ChatFormatting.AQUA),
    DIMENSIONAL("dimensional", -1, 32, ChatFormatting.LIGHT_PURPLE),
    QUANTUM("quantum", -2, 64, ChatFormatting.DARK_PURPLE);

    public static final int DIMENSION_WIDE = -1;
    public static final int ANY_DIMENSION = -2;

    private final String name;
    private final int range;
    private final int powerCost;
    private final ChatFormatting color;

    AccessPointTier(String name, int range, int powerCost, ChatFormatting color) {
        this.name = name;
        this.range = range;
        this.powerCost = powerCost;
        this.color = color;
    }

    public String getName() { return name; }
    public int getRange() { return range; }
    public int getPowerCost() { return powerCost; }
    public ChatFormatting getColor() { return color; }

    public String rangeDescription() {
        if (range == ANY_DIMENSION) return "All dimensions";
        if (range == DIMENSION_WIDE) return "Whole dimension";
        return range + " blocks";
    }
}
