package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: put the item on the cursor into the network. */
public class GridInsertPacket {
    private final int containerId;
    private final boolean single;

    public GridInsertPacket(int containerId, boolean single) {
        this.containerId = containerId;
        this.single = single;
    }

    public GridInsertPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.single = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeBoolean(single);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == containerId) {
            menu.handleInsertCarried(player, single);
        }
    }
}
