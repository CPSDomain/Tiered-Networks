package com.bobroflza.tierednetworks.menu;

import com.bobroflza.tierednetworks.blockentity.PatternProviderBlockEntity;
import com.bobroflza.tierednetworks.item.EncodedPatternItem;
import com.bobroflza.tierednetworks.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public class PatternProviderMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 62, SLOT_Y = 17;

    @Nullable private final PatternProviderBlockEntity provider;
    private final BlockPos pos;

    public PatternProviderMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos(), new ItemStackHandler(PatternProviderBlockEntity.SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof EncodedPatternItem;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        });
    }

    public PatternProviderMenu(int id, Inventory inv, PatternProviderBlockEntity be) {
        this(id, inv, be, be.getBlockPos(), be.getPatternSlots());
    }

    private PatternProviderMenu(int id, Inventory inv, @Nullable PatternProviderBlockEntity be, BlockPos pos, IItemHandler slots) {
        super(ModMenus.PATTERN_PROVIDER.get(), id);
        this.provider = be;
        this.pos = pos;
        for (int i = 0; i < PatternProviderBlockEntity.SLOTS; i++) {
            addSlot(new SlotItemHandler(slots, i, SLOT_X + (i % 3) * 18, SLOT_Y + (i / 3) * 18));
        }
        MenuHelper.addPlayerInventory(this::addSlot, inv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = PatternProviderBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, n, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return provider == null || (!provider.isRemoved()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0);
    }
}
