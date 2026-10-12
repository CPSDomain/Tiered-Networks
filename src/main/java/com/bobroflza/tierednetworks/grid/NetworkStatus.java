package com.bobroflza.tierednetworks.grid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum NetworkStatus {
    UNLINKED("unlinked", ChatFormatting.GRAY),
    NO_CONTROLLER("no_controller", ChatFormatting.RED),
    NO_POWER("no_power", ChatFormatting.RED),
    OUT_OF_RANGE("out_of_range", ChatFormatting.YELLOW),
    ONLINE("online", ChatFormatting.GREEN);

    private final String key;
    private final ChatFormatting color;

    NetworkStatus(String key, ChatFormatting color) {
        this.key = key;
        this.color = color;
    }

    public Component display() {
        return Component.translatable("status.tierednetworks." + key).withStyle(color);
    }
}
