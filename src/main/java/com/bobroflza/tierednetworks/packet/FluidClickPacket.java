package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: clicked the terminal's fluid grid with a container on the cursor. */
public class FluidClickPacket {
    private final int containerId;
    private final FluidStack target;

    public FluidClickPacket(int containerId, FluidStack target) {
        this.containerId = containerId;
        this.target = target;
    }

    public FluidClickPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.target = buf.readFluidStack();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeFluidStack(target);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == containerId) {
            menu.handleFluidClick(player, target);
        }
    }
}
