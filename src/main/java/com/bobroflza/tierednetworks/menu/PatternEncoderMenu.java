package com.bobroflza.tierednetworks.menu;

import com.bobroflza.tierednetworks.blockentity.PatternEncoderBlockEntity;
import com.bobroflza.tierednetworks.item.EncodedPatternItem;
import com.bobroflza.tierednetworks.registry.ModItems;
import com.bobroflza.tierednetworks.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Slots: 0-8 input ghosts (3x3), 9-11 output ghosts (processing only), 12 blank pattern, 13 encoded pattern, 14+ player.
 * Ghost slots: left-click with an item copies it (whole stack in processing mode), right-click with an item adds one,
 * right-click empty-handed takes one away, left-click empty-handed clears.
 */
public class PatternEncoderMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8, GRID_Y = 18;
    public static final int OUT_X = 98, OUT_Y = 18;
    public static final int BLANK_X = 146, BLANK_Y = 18;
    public static final int RESULT_X = 146, RESULT_Y = 54;
    public static final int INV_Y = 102;
    public static final int SLOT_BLANK = 12, SLOT_RESULT = 13, FIRST_PLAYER = 14;
    public static final int BUTTON_MODE = 0, BUTTON_ENCODE = 1, BUTTON_CLEAR = 2;

    @Nullable private final PatternEncoderBlockEntity encoder;
    private final BlockPos pos;
    private final ContainerData data;
    private final IItemHandler inputs;
    private final IItemHandler outputs;

    public PatternEncoderMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos(), new SimpleContainerData(1),
                new ItemStackHandler(PatternEncoderBlockEntity.INPUTS), new ItemStackHandler(PatternEncoderBlockEntity.OUTPUTS),
                new ItemStackHandler(2));
    }

    public PatternEncoderMenu(int id, Inventory inv, PatternEncoderBlockEntity be) {
        this(id, inv, be, be.getBlockPos(), be.getData(), be.getInputs(), be.getOutputs(), be.getPatterns());
    }

    private PatternEncoderMenu(int id, Inventory inv, @Nullable PatternEncoderBlockEntity be, BlockPos pos, ContainerData data,
                               IItemHandler inputs, IItemHandler outputs, IItemHandler patterns) {
        super(ModMenus.PATTERN_ENCODER.get(), id);
        this.encoder = be;
        this.pos = pos;
        this.data = data;
        this.inputs = inputs;
        this.outputs = outputs;

        for (int i = 0; i < PatternEncoderBlockEntity.INPUTS; i++) {
            addSlot(new InterfaceMenu.GhostSlot(inputs, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18));
        }
        for (int i = 0; i < PatternEncoderBlockEntity.OUTPUTS; i++) {
            addSlot(new InterfaceMenu.GhostSlot(outputs, i, OUT_X, OUT_Y + i * 18) {
                @Override
                public boolean isActive() {
                    return !isCrafting();
                }
            });
        }
        addSlot(new SlotItemHandler(patterns, 0, BLANK_X, BLANK_Y));
        addSlot(new SlotItemHandler(patterns, 1, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        MenuHelper.addPlayerInventory(this::addSlot, inv, 8, INV_Y);
        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) != 0;
    }

    public List<ItemStack> gridStacks() {
        List<ItemStack> out = new java.util.ArrayList<>(9);
        for (int i = 0; i < 9; i++) out.add(inputs.getStackInSlot(i));
        return out;
    }

    private void setGhost(IItemHandler handler, int slot, ItemStack stack) {
        if (handler instanceof ItemStackHandler h) h.setStackInSlot(slot, stack);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < SLOT_BLANK) {
            if (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE) return;
            IItemHandler handler = slotId < 9 ? inputs : outputs;
            int index = slotId < 9 ? slotId : slotId - 9;
            ItemStack current = handler.getStackInSlot(index);
            ItemStack carried = getCarried();
            boolean single = isCrafting() && slotId < 9;
            boolean right = button == 1;

            if (type == ClickType.QUICK_MOVE) {
                setGhost(handler, index, ItemStack.EMPTY);
            } else if (carried.isEmpty()) {
                if (right && current.getCount() > 1 && !single) setGhost(handler, index, current.copyWithCount(current.getCount() - 1));
                else setGhost(handler, index, ItemStack.EMPTY);
            } else if (right) {
                boolean same = !current.isEmpty() && ItemStack.isSameItemSameTags(current, carried);
                int count = same && !single ? Math.min(64, current.getCount() + 1) : 1;
                setGhost(handler, index, carried.copyWithCount(count));
            } else {
                setGhost(handler, index, carried.copyWithCount(single ? 1 : Math.min(64, carried.getCount())));
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < SLOT_BLANK) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == SLOT_BLANK || index == SLOT_RESULT) {
            if (!moveItemStackTo(stack, FIRST_PLAYER, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.BLANK_PATTERN.get()) || stack.getItem() instanceof EncodedPatternItem) {
            if (!moveItemStackTo(stack, SLOT_BLANK, SLOT_BLANK + 1, false)) return ItemStack.EMPTY;
        } else {
            // Any other item: drop a copy into the first free input cell. The real item stays.
            for (int i = 0; i < 9; i++) {
                if (inputs.getStackInSlot(i).isEmpty()) {
                    setGhost(inputs, i, stack.copyWithCount(isCrafting() ? 1 : stack.getCount()));
                    break;
                }
            }
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
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
        if (encoder == null) return false;
        switch (id) {
            case BUTTON_MODE -> encoder.toggleMode();
            case BUTTON_CLEAR -> encoder.clear();
            case BUTTON_ENCODE -> {
                String problem = encoder.encode();
                if (problem != null) {
                    player.displayClientMessage(Component.translatable(problem).withStyle(ChatFormatting.RED), true);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return encoder == null || (!encoder.isRemoved()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0);
    }
}
