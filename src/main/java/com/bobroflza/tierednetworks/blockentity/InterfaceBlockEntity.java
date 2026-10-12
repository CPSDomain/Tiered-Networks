package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.block.FlatInterfaceBlock;
import com.bobroflza.tierednetworks.menu.InterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import com.bobroflza.tierednetworks.storage.FluidKey;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared logic for the Import and Export Interfaces, full block and flat.
 * The full block works with inventories on all six sides; the flat one only with the block it is attached to.
 * Each has a 9-slot filter: ghost copies of items, matched by item type (NBT ignored).
 */
public abstract class InterfaceBlockEntity extends NetworkNodeBlockEntity implements MenuProvider {
    public static final int FILTER_SLOTS = 9;
    /** Ticks between transfers. */
    public static final int INTERVAL = 10;
    /** Most items moved per transfer. */
    public static final int AMOUNT = 64;

    /** mB of fluid or gas moved per transfer. */
    public static final int FLUID_AMOUNT = 8_000;

    /** A neighbouring inventory, seen from the side that touches this interface. */
    protected record Target(IItemHandler handler) {}

    /** A neighbouring tank (or gas tank, or pipe), seen from the side that touches this interface. */
    protected record FluidTarget(IFluidHandler handler) {}

    private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** false = allow list, true = deny list. Only used by the Import Interface. */
    protected boolean denyList;
    private int cooldown;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> denyList ? 1 : 0;
                case 1 -> getStatus().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) denyList = value != 0;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    protected InterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract boolean isExport();

    /** Moves up to {@code budget} items. */
    protected abstract void transfer(MinecraftServer server, UUID id, List<Target> targets, int budget);

    public ItemStackHandler getFilter() {
        return filter;
    }

    public ContainerData getData() {
        return data;
    }

    public void toggleMode() {
        denyList = !denyList;
        setChanged();
    }

    @Override
    public int getPowerCost() {
        return 2;
    }

    /**
     * Filter entries that name items. Filled fluid containers (buckets, gas cylinders) name a fluid instead,
     * so they are left out here and used by {@link #filterFluids()}.
     */
    protected List<ItemStack> filterItems() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack s = filter.getStackInSlot(i);
            if (!s.isEmpty() && !namesFluid(s)) out.add(s);
        }
        return out;
    }

    private static boolean namesFluid(ItemStack s) {
        return FluidUtil.getFluidContained(s).map(f -> !f.isEmpty()).orElse(false);
    }

    /** The fluids named by filled buckets, gas cylinders or other containers in the filter. */
    protected List<FluidKey> filterFluids() {
        List<FluidKey> out = new ArrayList<>();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack s = filter.getStackInSlot(i);
            if (s.isEmpty()) continue;
            FluidUtil.getFluidContained(s).ifPresent(f -> {
                if (!f.isEmpty()) out.add(FluidKey.of(f));
            });
        }
        return out;
    }

    /** Moves up to {@code budget} mB of fluids and gases. Interfaces that don't handle fluids leave this alone. */
    protected void transferFluids(MinecraftServer server, UUID id, List<FluidTarget> targets, int budget) {
    }

    protected boolean filterContains(ItemStack stack) {
        for (ItemStack s : filterItems()) {
            if (s.getItem() == stack.getItem()) return true;
        }
        return false;
    }

    @Nullable
    protected MinecraftServer server() {
        return level == null ? null : level.getServer();
    }

    /** Sides this interface works with: the attached face for the flat one, every face for the full block. */
    protected List<Direction> sides() {
        BlockState state = getBlockState();
        if (state.hasProperty(FlatInterfaceBlock.FACING)) return List.of(state.getValue(FlatInterfaceBlock.FACING));
        return List.of(Direction.values());
    }

    private List<Target> targets() {
        List<Target> out = new ArrayList<>();
        for (Direction dir : sides()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            // Never move items between two network blocks - that would just loop.
            if (be == null || be instanceof NetworkNodeBlockEntity) continue;
            be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(h -> out.add(new Target(h)));
        }
        return out;
    }

    private List<FluidTarget> fluidTargets() {
        List<FluidTarget> out = new ArrayList<>();
        for (Direction dir : sides()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be == null || be instanceof NetworkNodeBlockEntity) continue;
            be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(h -> out.add(new FluidTarget(h)));
        }
        return out;
    }

    public void serverTick() {
        if (--cooldown > 0) return;
        cooldown = INTERVAL;
        if (!isOnline() || networkId == null) return;
        MinecraftServer server = server();
        if (server == null) return;
        List<Target> targets = targets();
        if (!targets.isEmpty()) transfer(server, networkId, targets, AMOUNT);
        List<FluidTarget> fluidTargets = fluidTargets();
        if (!fluidTargets.isEmpty()) transferFluids(server, networkId, fluidTargets, FLUID_AMOUNT);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new InterfaceMenu(id, inv, this);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Filter")) filter.deserializeNBT(tag.getCompound("Filter"));
        denyList = tag.getBoolean("DenyList");
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Filter", filter.serializeNBT());
        tag.putBoolean("DenyList", denyList);
    }
}
