package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.item.StorageCellItem;
import com.bobroflza.tierednetworks.menu.DriveBayMenu;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DriveBayBlockEntity extends NetworkNodeBlockEntity implements MenuProvider {
    public static final int SLOTS = 10;

    private final ItemStackHandler cells = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof StorageCellItem;
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

    public DriveBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRIVE_BAY.get(), pos, state);
    }

    public ItemStackHandler getCells() {
        return cells;
    }

    /** Called after a cell's contents were changed in place. */
    public void onCellsChanged() {
        setChanged();
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.DRIVE;
    }

    @Override
    public int getPowerCost() {
        return 2;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tierednetworks.drive_bay");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DriveBayMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Cells")) cells.deserializeNBT(tag.getCompound("Cells"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Cells", cells.serializeNBT());
    }
}
