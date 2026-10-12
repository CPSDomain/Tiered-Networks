package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: take an item type out of the network. */
public class GridExtractPacket {
    private final int containerId;
    private final ItemStack request;
    private final int mode;

    public GridExtractPacket(int containerId, ItemStack request, int mode) {
        this.containerId = containerId;
        this.request = request;
        this.mode = mode;
    }

    public GridExtractPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.request = buf.readItem();
        this.mode = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeItem(request);
        buf.writeVarInt(mode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == containerId) {
            menu.handleExtract(player, request, mode);
        }
    }
}
