package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.menu.TerminalHost;
import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class TerminalBlockEntity extends NetworkNodeBlockEntity implements MenuProvider, TerminalHost {

    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERMINAL.get(), pos, state);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.TERMINAL;
    }

    @Override
    public int getPowerCost() {
        return 1;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tierednetworks.terminal");
    }

    @Nullable
    @Override
    public UUID hostNetwork() {
        return getNetworkId();
    }

    @Override
    public NetworkStatus hostStatus() {
        return getStatus();
    }

    @Nullable
    @Override
    public MinecraftServer hostServer() {
        return level == null ? null : level.getServer();
    }

    @Override
    public boolean hostStillValid(Player player) {
        return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TerminalMenu(id, inv, this);
    }
}
