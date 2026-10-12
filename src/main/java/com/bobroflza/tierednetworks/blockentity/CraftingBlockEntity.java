package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.block.CraftingBlock;
import com.bobroflza.tierednetworks.crafting.CpuPart;
import com.bobroflza.tierednetworks.crafting.CraftingJob;
import com.bobroflza.tierednetworks.crafting.CraftingManager;
import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One block of a Crafting CPU. Touching CPU blocks form one CPU. A running job is kept by
 * whichever block of the CPU started it.
 */
public class CraftingBlockEntity extends NetworkNodeBlockEntity {
    @Nullable private CraftingJob job;
    private boolean unloading;
    private int tick;

    public CraftingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRAFTING_CPU.get(), pos, state);
    }

    public CpuPart getPart() {
        return getBlockState().getBlock() instanceof CraftingBlock b ? b.getPart() : CpuPart.UNIT;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.CRAFTING;
    }

    @Override
    public int getPowerCost() {
        return getPart().powerCost();
    }

    @Nullable
    public CraftingJob getJob() {
        return job;
    }

    public void setJob(@Nullable CraftingJob job) {
        if (this.job != null) CraftingManager.untrackJob(this);
        this.job = job;
        if (job != null && level != null && !level.isClientSide) CraftingManager.trackJob(this);
        setChanged();
        // Light up (a Crafting Monitor shows it best) while running a job.
        BlockState state = getBlockState();
        if (level != null && !level.isClientSide && !isRemoved() && state.hasProperty(CraftingBlock.BUSY)
                && state.getValue(CraftingBlock.BUSY) != (job != null)) {
            level.setBlock(worldPosition, state.setValue(CraftingBlock.BUSY, job != null), 3);
        }
    }

    public void serverTick() {
        if (job == null) return;
        if (++tick % 5 == 0) CraftingManager.runJob(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (job != null && level != null && !level.isClientSide) CraftingManager.trackJob(this);
    }

    @Override
    public void onChunkUnloaded() {
        unloading = true;
        if (job != null) CraftingManager.untrackJob(this);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        if (!unloading && job != null && level != null && !level.isClientSide) {
            // The block holding the job was broken: cancel it and hand everything back.
            CraftingManager.cancel(this, true);
        }
        if (job != null) CraftingManager.untrackJob(this);
        super.setRemoved();
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        job = tag.contains("Job") ? CraftingJob.load(tag.getCompound("Job")) : null;
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        if (job != null) tag.put("Job", job.save());
    }
}
