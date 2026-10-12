package com.bobroflza.tierednetworks.crafting;

import com.bobroflza.tierednetworks.item.EncodedPatternItem;
import com.bobroflza.tierednetworks.storage.ItemKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A decoded Encoded Pattern.
 * Crafting patterns keep the 3x3 grid (one item per cell) and run in a Molecular Assembler.
 * Processing patterns keep a list of inputs and outputs with counts and run in any machine next to a Pattern Provider.
 */
public final class Pattern {
    public static final String TAG_CRAFTING = "Crafting";
    public static final String TAG_INPUTS = "Inputs";
    public static final String TAG_OUTPUTS = "Outputs";

    private final CompoundTag tag;
    private final boolean crafting;
    /** Crafting: exactly 9 entries, may be empty. Processing: the non-empty inputs. */
    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;
    private final Map<ItemKey, Long> inputTotals;
    private final Map<ItemKey, Long> outputTotals;

    private Pattern(CompoundTag tag, boolean crafting, List<ItemStack> inputs, List<ItemStack> outputs) {
        this.tag = tag;
        this.crafting = crafting;
        this.inputs = inputs;
        this.outputs = outputs;
        this.inputTotals = totals(inputs);
        this.outputTotals = totals(outputs);
    }

    private static Map<ItemKey, Long> totals(List<ItemStack> stacks) {
        Map<ItemKey, Long> out = new LinkedHashMap<>();
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) out.merge(ItemKey.of(s), (long) s.getCount(), Long::sum);
        }
        return Collections.unmodifiableMap(out);
    }

    @Nullable
    public static Pattern fromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EncodedPatternItem) || stack.getTag() == null) return null;
        return fromTag(stack.getTag());
    }

    @Nullable
    public static Pattern fromTag(CompoundTag tag) {
        if (!tag.contains(TAG_INPUTS, Tag.TAG_LIST) || !tag.contains(TAG_OUTPUTS, Tag.TAG_LIST)) return null;
        boolean crafting = tag.getBoolean(TAG_CRAFTING);
        List<ItemStack> in = readList(tag.getList(TAG_INPUTS, Tag.TAG_COMPOUND));
        List<ItemStack> out = readList(tag.getList(TAG_OUTPUTS, Tag.TAG_COMPOUND));
        if (crafting && in.size() != 9) return null;
        if (out.stream().allMatch(ItemStack::isEmpty) || in.stream().allMatch(ItemStack::isEmpty)) return null;
        return new Pattern(tag.copy(), crafting, in, out);
    }

    /** Builds the NBT for an Encoded Pattern. Crafting grids keep empty cells so the shape survives. */
    public static CompoundTag encode(boolean crafting, List<ItemStack> inputs, List<ItemStack> outputs) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_CRAFTING, crafting);
        tag.put(TAG_INPUTS, writeList(inputs, crafting));
        tag.put(TAG_OUTPUTS, writeList(outputs, false));
        return tag;
    }

    private static ListTag writeList(List<ItemStack> stacks, boolean keepEmpty) {
        ListTag list = new ListTag();
        for (ItemStack s : stacks) {
            if (s.isEmpty() && !keepEmpty) continue;
            list.add(s.save(new CompoundTag()));
        }
        return list;
    }

    private static List<ItemStack> readList(ListTag list) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(ItemStack.of(list.getCompound(i)));
        return out;
    }

    public CompoundTag tag() { return tag; }
    public boolean isCrafting() { return crafting; }
    public List<ItemStack> inputs() { return inputs; }
    public List<ItemStack> outputs() { return outputs; }
    public Map<ItemKey, Long> inputTotals() { return inputTotals; }
    public Map<ItemKey, Long> outputTotals() { return outputTotals; }

    /** The 9 grid stacks of a crafting pattern, each a fresh copy. */
    public List<ItemStack> gridCopy() {
        List<ItemStack> out = new ArrayList<>(9);
        for (ItemStack s : inputs) out.add(s.copy());
        return out;
    }

    // ------------------------------------------------------------ crafting-table helpers

    private static final AbstractContainerMenu DUMMY_MENU = new AbstractContainerMenu(null, -1) {
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void slotsChanged(net.minecraft.world.Container container) {
        }
    };

    public static CraftingContainer container(List<ItemStack> grid) {
        CraftingContainer c = new TransientCraftingContainer(DUMMY_MENU, 3, 3);
        for (int i = 0; i < 9 && i < grid.size(); i++) c.setItem(i, grid.get(i).isEmpty() ? ItemStack.EMPTY : grid.get(i).copyWithCount(1));
        return c;
    }

    public static Optional<CraftingRecipe> findRecipe(Level level, CraftingContainer container) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level);
    }

    /** What a crafting table makes from this grid, or EMPTY. */
    public static ItemStack craftingResult(Level level, List<ItemStack> grid) {
        CraftingContainer c = container(grid);
        return findRecipe(level, c).map(r -> r.assemble(c, level.registryAccess())).orElse(ItemStack.EMPTY);
    }
}
