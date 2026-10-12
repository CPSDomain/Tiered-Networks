package com.bobroflza.tierednetworks.block;

import com.bobroflza.tierednetworks.blockentity.CraftingBlockEntity;
import com.bobroflza.tierednetworks.crafting.CpuPart;
import com.bobroflza.tierednetworks.crafting.CraftingManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/** A Crafting CPU block. Right-click for the CPU's status; sneak + right-click a Crafting Monitor to cancel its job. */
public class CraftingBlock extends NetworkBlock {
    public static final BooleanProperty BUSY = BooleanProperty.create("busy");

    private final CpuPart part;

    public CraftingBlock(Properties props, CpuPart part) {
        super(props);
        this.part = part;
        registerDefaultState(stateDefinition.any().setValue(BUSY, false));
    }

    public CpuPart getPart() {
        return part;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BUSY);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof CraftingBlockEntity c) c.serverTick();
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CraftingBlockEntity self)) return;
        // Joins the network of a CPU it is placed against, so growing a CPU needs no relinking.
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(pos.relative(d)) instanceof CraftingBlockEntity n && n.getNetworkId() != null) {
                self.setNetworkId(n.getNetworkId());
                return;
            }
        }
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CraftingBlockEntity be)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (part == CpuPart.MONITOR && player.isShiftKeyDown()) {
            CraftingManager.Cpu cpu = CraftingManager.cpuOf(be);
            if (cpu.jobHolder == null) {
                player.displayClientMessage(Component.translatable("message.tierednetworks.cpu_idle"), true);
            } else {
                CraftingManager.cancel(cpu.jobHolder, false);
                player.displayClientMessage(Component.translatable("message.tierednetworks.cpu_cancelled").withStyle(ChatFormatting.YELLOW), false);
            }
        } else {
            player.displayClientMessage(CraftingManager.describe(be), false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (part.storage() > 0) {
            tooltip.add(Component.translatable("tooltip.tierednetworks.cpu_storage", String.format("%,d", part.storage())).withStyle(ChatFormatting.GRAY));
        }
        if (part.coprocessors() > 0) {
            tooltip.add(Component.translatable("tooltip.tierednetworks.cpu_coprocessor").withStyle(ChatFormatting.GRAY));
        }
        if (part == CpuPart.MONITOR) {
            tooltip.add(Component.translatable("tooltip.tierednetworks.cpu_monitor").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.tierednetworks.cpu_part").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.tierednetworks.power_cost", part.powerCost()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
