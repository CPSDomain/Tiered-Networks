package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.Config;
import com.bobroflza.tierednetworks.grid.NetworkTracker;
import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class ControllerBlockEntity extends NetworkNodeBlockEntity {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 10_000;
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_ENERGY = "Energy";

    @Nullable private UUID owner;
    private boolean active;
    private int tickCounter;
    private int lastCost;

    /** Accepts FE from outside but never gives it back; the controller drains it internally. */
    private final class ControllerEnergy extends EnergyStorage {
        ControllerEnergy() {
            super(CAPACITY, MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int got = super.receiveEnergy(maxReceive, simulate);
            if (got > 0 && !simulate) setChanged();
            return got;
        }

        boolean drain(int amount) {
            if (energy < amount) return false;
            energy -= amount;
            return true;
        }
    }

    private final ControllerEnergy energy = new ControllerEnergy();
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    public ControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROLLER.get(), pos, state);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.CONTROLLER;
    }

    @Override
    public int getPowerCost() {
        return Config.CONTROLLER_BASE_COST.get();
    }

    /** Called when placed. Keeps an existing network (controller picked up and moved), otherwise starts a new one. */
    public void initNetwork(@Nullable Player placer) {
        if (networkId == null) networkId = UUID.randomUUID();
        if (owner == null && placer != null) owner = placer.getUUID();
        setChanged();
        refreshRegistration();
    }

    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    public boolean isActive() {
        return active;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getLastCost() {
        return lastCost;
    }

    public void serverTick() {
        if (networkId == null) return;

        if (tickCounter++ % 20 == 0) {
            NetworkTracker.rebuildMesh(networkId);
        }

        int cost = getPowerCost() + NetworkTracker.devicePowerCost(networkId);
        lastCost = cost;

        boolean nowActive;
        if (!Config.REQUIRE_POWER.get()) {
            nowActive = true;
        } else {
            nowActive = energy.drain(cost);
            if (nowActive && cost > 0) setChanged();
        }
        if (nowActive != active) {
            active = nowActive;
            setChanged();
        }
    }

    public Component describeController() {
        if (networkId == null) return Component.translatable("status.tierednetworks.unlinked");
        Component state = Component.translatable(active ? "status.tierednetworks.online" : "status.tierednetworks.no_power");
        return Component.translatable("message.tierednetworks.controller_status",
                networkId.toString().substring(0, 8),
                NetworkTracker.nodeCount(networkId),
                NetworkTracker.activeAccessPointCount(networkId),
                energy.getEnergyStored(), CAPACITY, lastCost, state);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        if (tag.contains(TAG_ENERGY)) energy.deserializeNBT(tag.get(TAG_ENERGY));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) tag.putUUID(TAG_OWNER, owner);
        tag.put(TAG_ENERGY, energy.serializeNBT());
    }
}
