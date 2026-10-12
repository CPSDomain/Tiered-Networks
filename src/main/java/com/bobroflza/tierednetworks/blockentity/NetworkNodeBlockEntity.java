package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.grid.AccessPointTier;
import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.grid.NetworkTracker;
import com.bobroflza.tierednetworks.grid.NodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/** Any block that joins a wireless network: controller, access points, drives, terminals. */
public abstract class NetworkNodeBlockEntity extends BlockEntity {
    public static final String TAG_NETWORK = "NetworkId";

    @Nullable protected UUID networkId;
    @Nullable private UUID registeredId;
    @Nullable private GlobalPos registeredPos;

    protected NetworkNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract NodeKind getNodeKind();

    public abstract int getPowerCost();

    @Nullable
    public AccessPointTier getAccessPointTier() {
        return null;
    }

    @Nullable
    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(@Nullable UUID id) {
        this.networkId = id;
        setChanged();
        refreshRegistration();
    }

    public GlobalPos getGlobalPos() {
        return GlobalPos.of(level.dimension(), worldPosition);
    }

    /** Server side: makes the tracker's record match this block's current network. */
    public void refreshRegistration() {
        if (level == null || level.isClientSide) return;
        unregisterNode();
        if (networkId != null) {
            GlobalPos pos = getGlobalPos();
            NetworkTracker.register(networkId, pos,
                    new NetworkTracker.NodeInfo(getNodeKind(), getPowerCost(), getAccessPointTier()));
            registeredId = networkId;
            registeredPos = pos;
        }
    }

    private void unregisterNode() {
        if (registeredId != null && registeredPos != null) {
            NetworkTracker.unregister(registeredId, registeredPos);
        }
        registeredId = null;
        registeredPos = null;
    }

    public NetworkStatus getStatus() {
        if (level == null || level.isClientSide) return NetworkStatus.UNLINKED;
        MinecraftServer server = level.getServer();
        if (server == null) return NetworkStatus.UNLINKED;
        return NetworkTracker.status(server, networkId, getGlobalPos());
    }

    public boolean isOnline() {
        return getStatus() == NetworkStatus.ONLINE;
    }

    public Component describeNetwork() {
        String shortId = networkId == null ? "-" : networkId.toString().substring(0, 8);
        return Component.translatable("message.tierednetworks.node_status", shortId, getStatus().display());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshRegistration();
    }

    @Override
    public void setRemoved() {
        unregisterNode();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregisterNode();
        super.onChunkUnloaded();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        networkId = tag.hasUUID(TAG_NETWORK) ? tag.getUUID(TAG_NETWORK) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (networkId != null) tag.putUUID(TAG_NETWORK, networkId);
    }
}
