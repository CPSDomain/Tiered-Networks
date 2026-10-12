package com.bobroflza.tierednetworks;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_POWER;
    public static final ForgeConfigSpec.IntValue CONTROLLER_RANGE;
    public static final ForgeConfigSpec.IntValue CONTROLLER_BASE_COST;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("network");
        REQUIRE_POWER = b
                .comment("If true, the Network Controller needs FE to run. Set false when playing without any FE-generating mod.")
                .define("requirePower", true);
        CONTROLLER_RANGE = b
                .comment("Built-in wireless range of the Network Controller, in blocks.")
                .defineInRange("controllerRange", 8, 0, 64);
        CONTROLLER_BASE_COST = b
                .comment("FE per tick the controller uses on its own, before devices are added.")
                .defineInRange("controllerBaseCost", 2, 0, 1000);
        b.pop();
        SPEC = b.build();
    }

    private Config() {}
}
