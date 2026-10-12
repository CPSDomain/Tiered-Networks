package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server → client: the full item list of the network the open terminal is on. */
public class GridSyncPacket {
    final int containerId;
    final int status;
    final long used;
    final long capacity;
    final List<TerminalMenu.GridEntry> entries;

    public GridSyncPacket(int containerId, int status, long used, long capacity, List<TerminalMenu.GridEntry> entries) {
        this.containerId = containerId;
        this.status = status;
        this.used = used;
        this.capacity = capacity;
        this.entries = entries;
    }

    public GridSyncPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.status = buf.readVarInt();
        this.used = buf.readVarLong();
        this.capacity = buf.readVarLong();
        int size = buf.readVarInt();
        this.entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new TerminalMenu.GridEntry(buf.readItem(), buf.readVarLong(), buf.readBoolean()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(status);
        buf.writeVarLong(used);
        buf.writeVarLong(capacity);
        buf.writeVarInt(entries.size());
        for (TerminalMenu.GridEntry e : entries) {
            buf.writeItem(e.stack());
            buf.writeVarLong(e.count());
            buf.writeBoolean(e.craftable());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleGridSync(this));
    }
}
