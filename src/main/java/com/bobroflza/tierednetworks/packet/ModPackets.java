package com.bobroflza.tierednetworks.packet;

import com.bobroflza.tierednetworks.TieredNetworks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModPackets {
    private static final String PROTOCOL = "5";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TieredNetworks.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    private ModPackets() {}

    public static void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(GridSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GridSyncPacket::encode)
                .decoder(GridSyncPacket::new)
                .consumerMainThread(GridSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(GridExtractPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GridExtractPacket::encode)
                .decoder(GridExtractPacket::new)
                .consumerMainThread(GridExtractPacket::handle)
                .add();
        CHANNEL.messageBuilder(GridInsertPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GridInsertPacket::encode)
                .decoder(GridInsertPacket::new)
                .consumerMainThread(GridInsertPacket::handle)
                .add();
        CHANNEL.messageBuilder(CraftRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CraftRequestPacket::encode)
                .decoder(CraftRequestPacket::new)
                .consumerMainThread(CraftRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(FluidClickPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FluidClickPacket::encode)
                .decoder(FluidClickPacket::new)
                .consumerMainThread(FluidClickPacket::handle)
                .add();
        CHANNEL.messageBuilder(EncoderFillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EncoderFillPacket::encode)
                .decoder(EncoderFillPacket::new)
                .consumerMainThread(EncoderFillPacket::handle)
                .add();
        CHANNEL.messageBuilder(EmitterThresholdPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EmitterThresholdPacket::encode)
                .decoder(EmitterThresholdPacket::new)
                .consumerMainThread(EmitterThresholdPacket::handle)
                .add();
    }
}
