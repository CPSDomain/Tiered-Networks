package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.block.AccessPointBlock;
import com.bobroflza.tierednetworks.grid.AccessPointTier;
import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AccessPointBlockEntity extends NetworkNodeBlockEntity {

    public AccessPointBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACCESS_POINT.get(), pos, state);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.ACCESS_POINT;
    }

    @Override
    public AccessPointTier getAccessPointTier() {
        if (getBlockState().getBlock() instanceof AccessPointBlock ap) return ap.getTier();
        return AccessPointTier.BASIC;
    }

    @Override
    public int getPowerCost() {
        return getAccessPointTier().getPowerCost();
    }
}
