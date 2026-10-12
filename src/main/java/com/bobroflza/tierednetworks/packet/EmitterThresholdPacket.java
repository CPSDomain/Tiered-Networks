package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.menu.LevelEmitterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: the Level Emitter's threshold was typed in. */
public class EmitterThresholdPacket {
    private final int containerId;
    private final long threshold;

    public EmitterThresholdPacket(int containerId, long threshold) {
        this.containerId = containerId;
        this.threshold = threshold;
    }

    public EmitterThresholdPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.threshold = buf.readVarLong();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarLong(threshold);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (player.containerMenu instanceof LevelEmitterMenu menu && menu.containerId == containerId) {
            menu.setThreshold(threshold);
        }
    }
}
