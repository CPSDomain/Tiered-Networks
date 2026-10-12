package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client-only packet handling. Only referenced through DistExecutor so servers never load it. */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handleGridSync(GridSyncPacket msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == msg.containerId) {
            menu.receiveGrid(msg.status, msg.used, msg.capacity, msg.entries);
        }
    }
}
