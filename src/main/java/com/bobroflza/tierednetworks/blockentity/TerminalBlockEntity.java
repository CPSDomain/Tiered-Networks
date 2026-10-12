package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.menu.TerminalMenu;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class TerminalBlockEntity extends NetworkNodeBlockEntity implements MenuProvider {

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
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TerminalMenu(id, inv, this);
    }
}
