package com.bobroflza.tierednetworks.item;

import com.bobroflza.tierednetworks.storage.CellInventory;
import com.bobroflza.tierednetworks.storage.CellTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class StorageCellItem extends Item {
    private final CellTier tier;

    public StorageCellItem(Properties props, CellTier tier) {
        super(props.stacksTo(1));
        this.tier = tier;
    }

    public CellTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CellInventory inv = CellInventory.of(stack);
        if (inv == null) return;
        tooltip.add(Component.translatable("tooltip.tierednetworks.cell_used",
                String.format("%,d", inv.getUsed()), String.format("%,d", inv.getCapacity())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tierednetworks.cell_types",
                inv.getTypeCount(), CellTier.MAX_TYPES).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        CellInventory inv = CellInventory.of(stack);
        return inv != null && inv.getUsed() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CellInventory inv = CellInventory.of(stack);
        if (inv == null || inv.getCapacity() == 0) return 0;
        return (int) Math.round(13.0 * inv.getUsed() / inv.getCapacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3FA9F5;
    }
}
