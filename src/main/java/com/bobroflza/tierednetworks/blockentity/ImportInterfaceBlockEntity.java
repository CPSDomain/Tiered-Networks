package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.storage.FluidKey;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
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

    /** Lets pipes (fluid or Tiered Industries gas pipes) and tanks push straight into the network. */
    private final IFluidHandler fluidInput = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return acceptsFluid(FluidKey.of(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            MinecraftServer server = server();
            if (resource.isEmpty() || server == null || networkId == null || !isOnline()) return 0;
            FluidKey key = FluidKey.of(resource);
            if (!acceptsFluid(key)) return 0;
            return (int) NetworkStorage.insertFluid(server, networkId, key, resource.getAmount(), action.simulate());
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluidInput);

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

    public boolean acceptsFluid(FluidKey key) {
        List<FluidKey> wanted = filterFluids();
        if (wanted.isEmpty()) return true;
        return wanted.contains(key) != denyList;
    }

    @Override
    protected void transferFluids(MinecraftServer server, UUID id, List<FluidTarget> targets, int budget) {
        for (FluidTarget target : targets) {
            if (budget <= 0) return;
            IFluidHandler source = target.handler();
            for (int tank = 0; tank < source.getTanks() && budget > 0; tank++) {
                FluidStack inTank = source.getFluidInTank(tank);
                if (inTank.isEmpty()) continue;
                FluidKey key = FluidKey.of(inTank);
                if (!acceptsFluid(key)) continue;
                long fits = NetworkStorage.insertFluid(server, id, key, Math.min(budget, inTank.getAmount()), true);
                if (fits <= 0) continue;
                FluidStack drained = source.drain(key.toStack((int) fits), IFluidHandler.FluidAction.EXECUTE);
                if (drained.isEmpty()) continue;
                long stored = NetworkStorage.insertFluid(server, id, key, drained.getAmount(), false);
                if (stored < drained.getAmount()) {
                    // Should not happen: put the rest back.
                    source.fill(key.toStack((int) (drained.getAmount() - stored)), IFluidHandler.FluidAction.EXECUTE);
                }
                budget -= (int) stored;
            }
        }
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
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCap.invalidate();
        fluidCap.invalidate();
    }
}
