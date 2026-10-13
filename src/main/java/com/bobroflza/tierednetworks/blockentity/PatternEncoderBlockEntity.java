package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.crafting.Pattern;
import com.bobroflza.tierednetworks.item.EncodedPatternItem;
import com.bobroflza.tierednetworks.menu.PatternEncoderMenu;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Turns Blank Patterns into Encoded Patterns. Not part of any network. */
public class PatternEncoderBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUTS = 9;
    public static final int OUTPUTS = 3;

    private final ItemStackHandler inputs = ghostHandler(INPUTS);
    private final ItemStackHandler outputs = ghostHandler(OUTPUTS);
    /** Slot 0: Blank (or old Encoded) Pattern to write on. Slot 1: the finished pattern. */
    private final ItemStackHandler patterns = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == 1) return stack.getItem() instanceof EncodedPatternItem;
            return stack.is(ModItems.BLANK_PATTERN.get()) || stack.getItem() instanceof EncodedPatternItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private boolean crafting = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return crafting ? 1 : 0;
        }

        @Override
        public void set(int index, int value) {
            crafting = value != 0;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public PatternEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PATTERN_ENCODER.get(), pos, state);
    }

    private ItemStackHandler ghostHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public ItemStackHandler getInputs() { return inputs; }
    public ItemStackHandler getOutputs() { return outputs; }
    public ItemStackHandler getPatterns() { return patterns; }
    public ContainerData getData() { return data; }

    public void toggleMode() {
        crafting = !crafting;
        if (crafting) {
            // Crafting grids hold one of each item.
            for (int i = 0; i < INPUTS; i++) {
                ItemStack s = inputs.getStackInSlot(i);
                if (s.getCount() > 1) inputs.setStackInSlot(i, s.copyWithCount(1));
            }
        }
        setChanged();
    }

    /** Lays out a recipe: crafting keeps the 3x3 positions with one of each, processing keeps the amounts. */
    public void fill(boolean craftingMode, List<ItemStack> in, List<ItemStack> out) {
        crafting = craftingMode;
        clear();
        for (int i = 0; i < INPUTS && i < in.size(); i++) {
            ItemStack s = in.get(i);
            if (s.isEmpty()) continue;
            inputs.setStackInSlot(i, s.copyWithCount(crafting ? 1 : Math.max(1, Math.min(64, s.getCount()))));
        }
        if (!crafting) {
            for (int i = 0; i < OUTPUTS && i < out.size(); i++) {
                ItemStack s = out.get(i);
                if (!s.isEmpty()) outputs.setStackInSlot(i, s.copyWithCount(Math.max(1, Math.min(64, s.getCount()))));
            }
        }
        setChanged();
    }

    public void clear() {
        for (int i = 0; i < INPUTS; i++) inputs.setStackInSlot(i, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUTS; i++) outputs.setStackInSlot(i, ItemStack.EMPTY);
    }

    /** Writes the grid onto a pattern. Returns a message key when it can't. */
    @Nullable
    public String encode() {
        ItemStack source = patterns.getStackInSlot(0);
        if (source.isEmpty()) return "message.tierednetworks.encoder_no_blank";
        if (!patterns.getStackInSlot(1).isEmpty()) return "message.tierednetworks.encoder_output_full";

        List<ItemStack> in = new ArrayList<>();
        for (int i = 0; i < INPUTS; i++) in.add(inputs.getStackInSlot(i).copy());
        List<ItemStack> out = new ArrayList<>();
        if (crafting) {
            ItemStack result = Pattern.craftingResult(level, in);
            if (result.isEmpty()) return "message.tierednetworks.encoder_no_recipe";
            out.add(result);
        } else {
            for (int i = 0; i < OUTPUTS; i++) {
                ItemStack s = outputs.getStackInSlot(i);
                if (!s.isEmpty()) out.add(s.copy());
            }
            if (out.isEmpty() || in.stream().allMatch(ItemStack::isEmpty)) return "message.tierednetworks.encoder_empty";
        }

        ItemStack encoded = new ItemStack(ModItems.ENCODED_PATTERN.get());
        encoded.setTag(Pattern.encode(crafting, in, out));
        source.shrink(1);
        patterns.setStackInSlot(0, source.isEmpty() ? ItemStack.EMPTY : source);
        patterns.setStackInSlot(1, encoded);
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tierednetworks.pattern_encoder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PatternEncoderMenu(id, inv, this);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inputs")) inputs.deserializeNBT(tag.getCompound("Inputs"));
        if (tag.contains("Outputs")) outputs.deserializeNBT(tag.getCompound("Outputs"));
        if (tag.contains("Patterns")) patterns.deserializeNBT(tag.getCompound("Patterns"));
        crafting = !tag.contains("Crafting") || tag.getBoolean("Crafting");
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inputs", inputs.serializeNBT());
        tag.put("Outputs", outputs.serializeNBT());
        tag.put("Patterns", patterns.serializeNBT());
        tag.putBoolean("Crafting", crafting);
    }
}
