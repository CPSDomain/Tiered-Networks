package com.bobroflza.tierednetworks.menu;

import com.bobroflza.tierednetworks.blockentity.DriveBayBlockEntity;
import com.bobroflza.tierednetworks.registry.ModBlocks;
import com.bobroflza.tierednetworks.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class DriveBayMenu extends AbstractContainerMenu {
    public static final int CELL_X = 35;
    public static final int CELL_Y = 20;

    private final ContainerLevelAccess access;

    public DriveBayMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    private DriveBayMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof DriveBayBlockEntity d ? d : null, pos);
    }

    public DriveBayMenu(int id, Inventory inv, DriveBayBlockEntity be) {
        this(id, inv, be, be.getBlockPos());
    }

    private DriveBayMenu(int id, Inventory inv, DriveBayBlockEntity be, BlockPos pos) {
        super(ModMenus.DRIVE_BAY.get(), id);
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
        IItemHandler cells = be != null ? be.getCells() : new ItemStackHandler(DriveBayBlockEntity.SLOTS);

        for (int i = 0; i < DriveBayBlockEntity.SLOTS; i++) {
            int col = i % 5;
            int row = i / 5;
            addSlot(new SlotItemHandler(cells, i, CELL_X + 18 * col + 9, CELL_Y + 18 * row));
        }
        MenuHelper.addPlayerInventory(this::addSlot, inv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int cellSlots = DriveBayBlockEntity.SLOTS;
        if (index < cellSlots) {
            if (!moveItemStackTo(stack, cellSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, cellSlots, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DRIVE_BAY.get());
    }
}
