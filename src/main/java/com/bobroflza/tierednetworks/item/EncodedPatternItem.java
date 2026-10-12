package com.bobroflza.tierednetworks.item;

import com.bobroflza.tierednetworks.crafting.Pattern;
import com.bobroflza.tierednetworks.storage.ItemKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** A pattern made in the Pattern Encoder. Sneak + right-click in the air to wipe it back to a Blank Pattern. */
public class EncodedPatternItem extends Item {
    private final Supplier<Item> blank;

    public EncodedPatternItem(Properties props, Supplier<Item> blank) {
        super(props.stacksTo(1));
        this.blank = blank;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            player.setItemInHand(hand, new ItemStack(blank.get()));
            player.displayClientMessage(Component.translatable("message.tierednetworks.pattern_cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        Pattern p = Pattern.fromStack(stack);
        if (p == null) return super.getName(stack);
        ItemStack first = p.outputs().get(0);
        return Component.translatable("item.tierednetworks.encoded_pattern.named", first.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Pattern p = Pattern.fromStack(stack);
        if (p == null) {
            tooltip.add(Component.translatable("tooltip.tierednetworks.pattern_invalid").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable(p.isCrafting() ? "tooltip.tierednetworks.pattern_crafting" : "tooltip.tierednetworks.pattern_processing")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tierednetworks.pattern_makes").withStyle(ChatFormatting.GRAY));
        lines(tooltip, p.outputTotals());
        tooltip.add(Component.translatable("tooltip.tierednetworks.pattern_uses").withStyle(ChatFormatting.GRAY));
        lines(tooltip, p.inputTotals());
    }

    private static void lines(List<Component> tooltip, Map<ItemKey, Long> totals) {
        totals.forEach((k, v) -> tooltip.add(Component.literal("  " + v + "x ").append(k.toStack(1).getHoverName())
                .withStyle(ChatFormatting.DARK_GRAY)));
    }
}
