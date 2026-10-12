package com.bobroflza.tierednetworks.block;

import com.bobroflza.tierednetworks.blockentity.AccessPointBlockEntity;
import com.bobroflza.tierednetworks.blockentity.NetworkNodeBlockEntity;
import com.bobroflza.tierednetworks.grid.AccessPointTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

public class AccessPointBlock extends NetworkBlock {
    private final AccessPointTier tier;

    public AccessPointBlock(Properties props, AccessPointTier tier) {
        super(props);
        this.tier = tier;
    }

    public AccessPointTier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AccessPointBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof NetworkNodeBlockEntity node) {
            player.displayClientMessage(node.describeNetwork(), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tierednetworks.ap_range", tier.rangeDescription()).withStyle(tier.getColor()));
        tooltip.add(Component.translatable("tooltip.tierednetworks.power_cost", tier.getPowerCost()).withStyle(ChatFormatting.GRAY));
    }
}
