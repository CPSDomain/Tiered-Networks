package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: craft this many of an item, from the open terminal. */
public class CraftRequestPacket {
    private final int containerId;
    private final ItemStack item;
    private final long amount;

    public CraftRequestPacket(int containerId, ItemStack item, long amount) {
        this.containerId = containerId;
        this.item = item;
        this.amount = amount;
    }

    public CraftRequestPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.item = buf.readItem();
        this.amount = buf.readVarLong();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeItem(item);
        buf.writeVarLong(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == containerId) {
            menu.handleCraftRequest(player, item, Math.max(1, Math.min(amount, 1_000_000L)));
        }
    }
}
