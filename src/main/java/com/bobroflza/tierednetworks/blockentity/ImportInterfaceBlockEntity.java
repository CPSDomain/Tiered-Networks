package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.block.ImportInterfaceBlock;
import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Moves items into the network two ways:
 * <ul>
 *   <li>Pulls from the inventory its front faces (a chest, a machine's output).</li>
 *   <li>Accepts anything pushed into it: hoppers, pipes, and machines that auto-eject, like a Digital Miner.</li>
 * </ul>
 */
public class ImportInterfaceBlockEntity extends NetworkNodeBlockEntity {
    /** Ticks between pulls from the inventory in front. */
    public static final int PULL_INTERVAL = 10;
    /** Most items moved per pull. */
    public static final int PULL_AMOUNT = 64;

    private int cooldown;

    /** One-slot handler that feeds straight into the network. Always looks empty, so anything can push into it. */
    private final IItemHandler networkInput = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            UUID id = onlineNetwork();
            MinecraftServer server = server();
            if (id == null || server == null || stack.isEmpty()) return stack;
            return NetworkStorage.insert(server, id, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return true;
        }
    };
    private final LazyOptional<IItemHandler> inputCap = LazyOptional.of(() -> networkInput);

    public ImportInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMPORT_INTERFACE.get(), pos, state);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.IMPORT;
    }

    @Override
    public int getPowerCost() {
        return 2;
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(ImportInterfaceBlock.FACING) ? state.getValue(ImportInterfaceBlock.FACING) : Direction.NORTH;
    }

    @Nullable
    private MinecraftServer server() {
        return level == null ? null : level.getServer();
    }

    @Nullable
    private UUID onlineNetwork() {
        return isOnline() ? networkId : null;
    }

    public void serverTick() {
        if (--cooldown > 0) return;
        cooldown = PULL_INTERVAL;

        UUID id = onlineNetwork();
        MinecraftServer server = server();
        if (id == null || server == null) return;

        Direction front = front();
        BlockEntity target = level.getBlockEntity(worldPosition.relative(front));
        if (target == null || target instanceof NetworkNodeBlockEntity) return;
        IItemHandler source = target.getCapability(ForgeCapabilities.ITEM_HANDLER, front.getOpposite()).orElse(null);
        if (source == null) return;

        int budget = PULL_AMOUNT;
        for (int slot = 0; slot < source.getSlots() && budget > 0; slot++) {
            ItemStack peek = source.extractItem(slot, budget, true);
            if (peek.isEmpty()) continue;

            // How much of it would fit?
            ItemStack wouldRemain = NetworkStorage.insert(server, id, peek, true);
            int fits = peek.getCount() - wouldRemain.getCount();
            if (fits <= 0) continue;

            ItemStack taken = source.extractItem(slot, fits, false);
            if (taken.isEmpty()) continue;
            ItemStack leftover = NetworkStorage.insert(server, id, taken, false);
            if (!leftover.isEmpty()) {
                // Should not happen, but never lose items: put them back where they came from.
                ItemStack back = source.insertItem(slot, leftover, false);
                if (!back.isEmpty()) {
                    Containers.dropItemStack(level,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, back);
                }
            }
            budget -= taken.getCount() - leftover.getCount();
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        // Every side accepts pushed items, the front too: a machine you face it at
        // (a Digital Miner, say) usually ejects straight into this face.
        if (cap == ForgeCapabilities.ITEM_HANDLER) return inputCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCap.invalidate();
    }
}
