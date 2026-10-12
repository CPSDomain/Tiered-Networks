package com.bobroflza.tierednetworks.menu;

import com.bobroflza.tierednetworks.blockentity.InterfaceBlockEntity;
import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public class InterfaceMenu extends AbstractContainerMenu {
    public static final int FILTER_X = 8;
    public static final int FILTER_Y = 20;
    public static final int BUTTON_MODE = 0;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final boolean export;
    private final ContainerData data;
    private final IItemHandler filter;

    /** Client side. */
    public InterfaceMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos(), buf.readBoolean(), new SimpleContainerData(2),
                new ItemStackHandler(InterfaceBlockEntity.FILTER_SLOTS));
    }

    /** Server side. */
    public InterfaceMenu(int id, Inventory inv, InterfaceBlockEntity be) {
        this(id, inv, be, be.getBlockPos(), be.isExport(), be.getData(), be.getFilter());
    }

    private InterfaceMenu(int id, Inventory inv, @Nullable InterfaceBlockEntity be, BlockPos pos, boolean export,
                          ContainerData data, IItemHandler filter) {
        super(ModMenus.INTERFACE.get(), id);
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
        this.pos = pos;
        this.export = export;
        this.data = data;
        this.filter = filter;

        for (int i = 0; i < InterfaceBlockEntity.FILTER_SLOTS; i++) {
            addSlot(new GhostSlot(filter, i, FILTER_X + (i % 3) * 18, FILTER_Y + (i / 3) * 18));
        }
        MenuHelper.addPlayerInventory(this::addSlot, inv, 8, 84);
        addDataSlots(data);
    }

    public boolean isExport() {
        return export;
    }

    public boolean isDenyList() {
        return data.get(0) != 0;
    }

    public NetworkStatus getStatus() {
        int i = data.get(1);
        NetworkStatus[] values = NetworkStatus.values();
        return i >= 0 && i < values.length ? values[i] : NetworkStatus.UNLINKED;
    }

    private void setGhost(int slot, ItemStack stack) {
        if (filter instanceof ItemStackHandler h) {
            h.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
    }

    private boolean filterHas(ItemStack stack) {
        for (int i = 0; i < InterfaceBlockEntity.FILTER_SLOTS; i++) {
            ItemStack s = filter.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == stack.getItem()) return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < InterfaceBlockEntity.FILTER_SLOTS) {
            // Ghost slot: copy what's on the cursor, or clear it. Never takes the real item.
            if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) {
                setGhost(slotId, type == ClickType.QUICK_MOVE ? ItemStack.EMPTY : getCarried());
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-click in your inventory: add that item to the filter. Your item stays put.
        if (index < InterfaceBlockEntity.FILTER_SLOTS) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || filterHas(stack)) return ItemStack.EMPTY;
        for (int i = 0; i < InterfaceBlockEntity.FILTER_SLOTS; i++) {
            if (filter.getStackInSlot(i).isEmpty()) {
                setGhost(i, stack);
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof GhostSlot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof GhostSlot);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_MODE && !export) {
            if (player.level().getBlockEntity(pos) instanceof InterfaceBlockEntity io) {
                io.toggleMode();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, p) -> level.getBlockEntity(p) instanceof InterfaceBlockEntity
                && player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= 64.0, true);
    }

    /** A filter slot: shows a copy of an item, never holds a real one. */
    public static class GhostSlot extends SlotItemHandler {
        public GhostSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
