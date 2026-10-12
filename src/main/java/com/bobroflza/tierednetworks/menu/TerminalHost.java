package com.bobroflza.tierednetworks.menu;

import com.bobroflza.tierednetworks.grid.NetworkStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

/** Whatever a terminal screen is opened from: the Network Terminal block or a Wireless Terminal in hand. */
public interface TerminalHost {
    @Nullable
    UUID hostNetwork();

    NetworkStatus hostStatus();

    @Nullable
    MinecraftServer hostServer();

    boolean hostStillValid(Player player);
}
