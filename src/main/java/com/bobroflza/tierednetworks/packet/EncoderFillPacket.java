package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.menu.PatternEncoderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Client → server: JEI's "+" filled the Pattern Encoder with a recipe. */
public class EncoderFillPacket {
    private final int containerId;
    private final boolean crafting;
    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;

    public EncoderFillPacket(int containerId, boolean crafting, List<ItemStack> inputs, List<ItemStack> outputs) {
        this.containerId = containerId;
        this.crafting = crafting;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public EncoderFillPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.crafting = buf.readBoolean();
        this.inputs = readList(buf, 9);
        this.outputs = readList(buf, 3);
    }

    private static List<ItemStack> readList(FriendlyByteBuf buf, int max) {
        int n = Math.min(buf.readVarInt(), max);
        List<ItemStack> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(buf.readItem());
        return out;
    }

    private static void writeList(FriendlyByteBuf buf, List<ItemStack> list, int max) {
        int n = Math.min(list.size(), max);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) buf.writeItem(list.get(i));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeBoolean(crafting);
        writeList(buf, inputs, 9);
        writeList(buf, outputs, 3);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (player.containerMenu instanceof PatternEncoderMenu menu && menu.containerId == containerId) {
            menu.fillFromRecipe(crafting, inputs, outputs);
        }
    }
}
