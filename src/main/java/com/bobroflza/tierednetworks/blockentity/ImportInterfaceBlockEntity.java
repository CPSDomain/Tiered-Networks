package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Pulls items from neighbouring inventories into the network, and accepts anything pushed into it
 * (hoppers, pipes, auto-ejecting machines). The filter applies to both.
 * Empty filter: everything. Allow list: only the listed items. Deny list: everything except them.
 */
public class ImportInterfaceBlockEntity extends InterfaceBlockEntity {

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
            MinecraftServer server = server();
            if (stack.isEmpty() || server == null || networkId == null || !isOnline() || !accepts(stack)) return stack;
            return NetworkStorage.insert(server, networkId, stack, simulate);
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
            return accepts(stack);
        }
    };
    private final LazyOptional<IItemHandler> inputCap = LazyOptional.of(() -> networkInput);

    public ImportInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMPORT_INTERFACE.get(), pos, state);
    }

    @Override
    public boolean isExport() {
        return false;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.IMPORT;
    }

    public boolean accepts(ItemStack stack) {
        if (filterItems().isEmpty()) return true;
        return filterContains(stack) != denyList;
    }

    @Override
    protected void transfer(MinecraftServer server, UUID id, List<Target> targets, int budget) {
        for (Target target : targets) {
            IItemHandler source = target.handler();
            for (int slot = 0; slot < source.getSlots() && budget > 0; slot++) {
                ItemStack inSlot = source.getStackInSlot(slot);
                if (inSlot.isEmpty() || !accepts(inSlot)) continue;

                ItemStack peek = source.extractItem(slot, budget, true);
                if (peek.isEmpty()) continue;
                ItemStack wouldRemain = NetworkStorage.insert(server, id, peek, true);
                int fits = peek.getCount() - wouldRemain.getCount();
                if (fits <= 0) continue;

                ItemStack taken = source.extractItem(slot, fits, false);
                if (taken.isEmpty()) continue;
                ItemStack leftover = NetworkStorage.insert(server, id, taken, false);
                if (!leftover.isEmpty()) {
                    // Should not happen, but never lose items: put them back.
                    ItemStack back = source.insertItem(slot, leftover, false);
                    if (!back.isEmpty()) {
                        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                                worldPosition.getZ() + 0.5, back);
                    }
                }
                budget -= taken.getCount() - leftover.getCount();
            }
            if (budget <= 0) return;
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return inputCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCap.invalidate();
    }
}
