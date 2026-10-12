package com.bobroflza.tierednetworks.menu;

import com.bobroflza.tierednetworks.blockentity.LevelEmitterBlockEntity;
import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class LevelEmitterMenu extends AbstractContainerMenu {
    public static final int FILTER_X = 26;
    public static final int FILTER_Y = 24;
    public static final int BUTTON_MODE = 0;

    @Nullable private final LevelEmitterBlockEntity emitter;
    private final BlockPos pos;
    private final ContainerData data;
    private final IItemHandler filter;
    private final long initialThreshold;

    /** Client side. */
    public LevelEmitterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos(), buf.readVarLong(), new SimpleContainerData(6), new ItemStackHandler(1));
    }

    /** Server side. */
    public LevelEmitterMenu(int id, Inventory inv, LevelEmitterBlockEntity be) {
        this(id, inv, be, be.getBlockPos(), be.getThreshold(), be.getData(), be.getFilter());
    }

    private LevelEmitterMenu(int id, Inventory inv, @Nullable LevelEmitterBlockEntity be, BlockPos pos, long threshold,
                             ContainerData data, IItemHandler filter) {
        super(ModMenus.LEVEL_EMITTER.get(), id);
        this.emitter = be;
        this.pos = pos;
        this.data = data;
        this.filter = filter;
        this.initialThreshold = threshold;
        addSlot(new InterfaceMenu.GhostSlot(filter, 0, FILTER_X, FILTER_Y));
        MenuHelper.addPlayerInventory(this::addSlot, inv, 8, 84);
        addDataSlots(data);
    }

    public long getInitialThreshold() { return initialThreshold; }

    public boolean isBelowMode() { return data.get(0) != 0; }

    public NetworkStatus getStatus() {
        int i = data.get(1);
        NetworkStatus[] values = NetworkStatus.values();
        return i >= 0 && i < values.length ? values[i] : NetworkStatus.UNLINKED;
    }

    public long getCount() {
        long v = 0;
        for (int i = 0; i < 4; i++) v |= ((long) (data.get(2 + i) & 0xFFFF)) << (16 * i);
        return v;
    }

    /** Server side, from the threshold packet. */
    public void setThreshold(long value) {
        if (emitter != null) emitter.setThreshold(value);
    }

    private void setGhost(ItemStack stack) {
        if (filter instanceof ItemStackHandler h) h.setStackInSlot(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId == 0) {
            if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) {
                setGhost(type == ClickType.QUICK_MOVE ? ItemStack.EMPTY : getCarried());
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index > 0) {
            ItemStack stack = slots.get(index).getItem();
            if (!stack.isEmpty()) setGhost(stack);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof InterfaceMenu.GhostSlot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof InterfaceMenu.GhostSlot);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_MODE && emitter != null) {
            emitter.toggleMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return emitter == null || (!emitter.isRemoved()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0);
    }
}
