package com.bobroflza.tierednetworks.item;

import com.bobroflza.tierednetworks.storage.CellTier;
import com.bobroflza.tierednetworks.storage.FluidCellInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Stores liquids (fluid cell) or gases (gas cell) in a Drive Bay. Amounts are in mB. */
public class FluidCellItem extends Item {
    /** mB of storage per "item" of the matching item cell tier: a 1k fluid cell holds 256 buckets. */
    public static final long MB_PER_UNIT = 250;
    public static final int MAX_TYPES = 15;

    private final CellTier tier;
    private final boolean gas;

    public FluidCellItem(Properties props, CellTier tier, boolean gas) {
        super(props.stacksTo(1));
        this.tier = tier;
        this.gas = gas;
    }

    public CellTier getTier() { return tier; }
    public boolean isGas() { return gas; }
    public long getCapacity() { return tier.getCapacity() * MB_PER_UNIT; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        FluidCellInventory inv = FluidCellInventory.of(stack);
        if (inv == null) return;
        tooltip.add(Component.translatable(gas ? "tooltip.tierednetworks.gas_cell" : "tooltip.tierednetworks.fluid_cell")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tierednetworks.fluid_cell_used",
                String.format("%,d", inv.getUsed() / 1000), String.format("%,d", inv.getCapacity() / 1000)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tierednetworks.cell_types", inv.getTypeCount(), MAX_TYPES).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        FluidCellInventory inv = FluidCellInventory.of(stack);
        return inv != null && inv.getUsed() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FluidCellInventory inv = FluidCellInventory.of(stack);
        if (inv == null || inv.getCapacity() == 0) return 0;
        return (int) Math.round(13.0 * inv.getUsed() / inv.getCapacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return gas ? 0xC8E6FF : 0x3F76E4;
    }
}
