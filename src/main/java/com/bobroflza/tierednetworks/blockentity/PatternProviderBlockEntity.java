package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.crafting.Pattern;
import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.item.EncodedPatternItem;
import com.bobroflza.tierednetworks.menu.PatternProviderMenu;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.storage.ItemKey;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holds up to 9 Encoded Patterns and carries out crafting jobs for them:
 * crafting patterns go to an idle Molecular Assembler touching it, processing patterns are pushed into a
 * machine or inventory touching it. Anything pushed into the provider (a machine's output) goes into the network.
 */
public class PatternProviderBlockEntity extends NetworkNodeBlockEntity implements MenuProvider {
    public static final int SLOTS = 9;

    private final ItemStackHandler patterns = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof EncodedPatternItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final IItemHandler returnInput = new IItemHandler() {
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
            MinecraftServer server = level == null ? null : level.getServer();
            if (stack.isEmpty() || server == null || networkId == null || !isOnline()) return stack;
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
            return true;
        }
    };
    private final LazyOptional<IItemHandler> returnCap = LazyOptional.of(() -> returnInput);

    public PatternProviderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PATTERN_PROVIDER.get(), pos, state);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.PROVIDER;
    }

    @Override
    public int getPowerCost() {
        return 2;
    }

    public ItemStackHandler getPatternSlots() {
        return patterns;
    }

    public List<Pattern> getPatterns() {
        List<Pattern> out = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            Pattern p = Pattern.fromStack(patterns.getStackInSlot(i));
            if (p != null) out.add(p);
        }
        return out;
    }

    public boolean holds(CompoundTag patternTag) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = patterns.getStackInSlot(i);
            if (!s.isEmpty() && patternTag.equals(s.getTag())) return true;
        }
        return false;
    }

    // ------------------------------------------------------------ dispatch

    @Nullable
    private AssemblerBlockEntity idleAssembler() {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof AssemblerBlockEntity a && !a.isBusy()) return a;
        }
        return null;
    }

    /** A machine or inventory touching this provider that takes every input of the pattern at once. */
    @Nullable
    private IItemHandler machineFor(Pattern pattern) {
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be == null || be instanceof NetworkNodeBlockEntity || be instanceof AssemblerBlockEntity) continue;
            IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
            if (h == null) continue;
            boolean fits = true;
            for (Map.Entry<ItemKey, Long> in : pattern.inputTotals().entrySet()) {
                ItemStack probe = in.getKey().toStack((int) (long) in.getValue());
                if (!ItemHandlerHelper.insertItemStacked(h, probe, true).isEmpty()) {
                    fits = false;
                    break;
                }
            }
            if (fits) return h;
        }
        return null;
    }

    public boolean canRun(Pattern pattern) {
        if (level == null || !isOnline()) return false;
        return pattern.isCrafting() ? idleAssembler() != null : machineFor(pattern) != null;
    }

    /**
     * Runs one batch of the pattern with ingredients the CPU already took from its own store.
     * Returns whatever could not be handed over (normally nothing) so the CPU can keep it.
     */
    public List<ItemStack> run(Pattern pattern) {
        List<ItemStack> leftovers = new ArrayList<>();
        if (pattern.isCrafting()) {
            AssemblerBlockEntity a = idleAssembler();
            if (a == null || networkId == null) {
                pattern.inputTotals().forEach((k, v) -> leftovers.add(k.toStack((int) (long) v)));
                return leftovers;
            }
            a.start(pattern.gridCopy(), networkId);
            return leftovers;
        }
        IItemHandler h = machineFor(pattern);
        for (Map.Entry<ItemKey, Long> in : pattern.inputTotals().entrySet()) {
            ItemStack stack = in.getKey().toStack((int) (long) in.getValue());
            ItemStack left = h == null ? stack : ItemHandlerHelper.insertItemStacked(h, stack, false);
            if (!left.isEmpty()) leftovers.add(left);
        }
        return leftovers;
    }

    // ------------------------------------------------------------ plumbing

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return returnCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        returnCap.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tierednetworks.pattern_provider");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PatternProviderMenu(id, inv, this);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Patterns")) patterns.deserializeNBT(tag.getCompound("Patterns"));
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Patterns", patterns.serializeNBT());
    }
}
