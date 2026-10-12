package com.bobroflza.tierednetworks.block;

import com.bobroflza.tierednetworks.blockentity.ExportInterfaceBlockEntity;
import com.bobroflza.tierednetworks.blockentity.ImportInterfaceBlockEntity;
import com.bobroflza.tierednetworks.blockentity.InterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/** Full-block Import or Export Interface: works with inventories on all six sides. */
public class InterfaceBlock extends NetworkBlock {
    private final boolean export;

    public InterfaceBlock(Properties props, boolean export) {
        super(props);
        this.export = export;
    }

    public boolean isExport() {
        return export;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createInterface(export, pos, state);
    }

    static BlockEntity createInterface(boolean export, BlockPos pos, BlockState state) {
        return export ? new ExportInterfaceBlockEntity(pos, state) : new ImportInterfaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ticker(level);
    }

    static <T extends BlockEntity> BlockEntityTicker<T> ticker(Level level) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof InterfaceBlockEntity io) io.serverTick();
        };
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    static InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof InterfaceBlockEntity io) {
            NetworkHooks.openScreen(sp, io, buf -> {
                buf.writeBlockPos(pos);
                buf.writeBoolean(io.isExport());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip(tooltip, export, false);
    }

    static void tooltip(List<Component> tooltip, boolean export, boolean flat) {
        String dir = export ? "export" : "import";
        tooltip.add(Component.translatable("tooltip.tierednetworks." + dir + (flat ? "_flat" : "_full")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tierednetworks." + dir + "_filter").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tierednetworks.power_cost", 2).withStyle(ChatFormatting.DARK_GRAY));
    }
}
