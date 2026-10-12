package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.block.LevelEmitterBlock;
import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.menu.LevelEmitterMenu;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.storage.ItemKey;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Gives a redstone signal based on how many of an item the network holds.
 * Empty filter: counts every item stored. Mode "at least": on when count >= threshold. Mode "below": on when count < threshold.
 */
public class LevelEmitterBlockEntity extends NetworkNodeBlockEntity implements MenuProvider {
    public static final int INTERVAL = 10;

    private final ItemStackHandler filter = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            cooldown = 0;
        }
    };

    private long threshold = 64;
    private boolean below;
    private long lastCount;
    private int cooldown;

    /** 0 mode, 1 status, 2-5 the current count split into 16-bit pieces (menu data is sent as shorts). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> below ? 1 : 0;
                case 1 -> getStatus().ordinal();
                case 2, 3, 4, 5 -> (int) ((lastCount >>> (16 * (index - 2))) & 0xFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public LevelEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEVEL_EMITTER.get(), pos, state);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.EMITTER;
    }

    @Override
    public int getPowerCost() {
        return 1;
    }

    public ItemStackHandler getFilter() { return filter; }
    public ContainerData getData() { return data; }
    public long getThreshold() { return threshold; }

    public void setThreshold(long value) {
        threshold = Math.max(0, value);
        cooldown = 0;
        setChanged();
    }

    public void toggleMode() {
        below = !below;
        cooldown = 0;
        setChanged();
    }

    public void serverTick() {
        if (--cooldown > 0) return;
        cooldown = INTERVAL;

        boolean on = false;
        MinecraftServer server = level.getServer();
        if (server != null && networkId != null && isOnline()) {
            lastCount = count(server);
            on = below ? lastCount < threshold : lastCount >= threshold;
        } else {
            lastCount = 0;
        }
        BlockState state = getBlockState();
        if (state.getValue(LevelEmitterBlock.POWERED) != on) {
            level.setBlock(worldPosition, state.setValue(LevelEmitterBlock.POWERED, on), 3);
        }
    }

    private long count(MinecraftServer server) {
        ItemStack want = filter.getStackInSlot(0);
        long total = 0;
        for (Map.Entry<ItemKey, Long> e : NetworkStorage.listItems(server, networkId).entrySet()) {
            if (want.isEmpty() || e.getKey().item() == want.getItem()) total += e.getValue();
        }
        return total;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tierednetworks.level_emitter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new LevelEmitterMenu(id, inv, this);
    }

    public void writeOpenData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        buf.writeVarLong(threshold);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Filter")) filter.deserializeNBT(tag.getCompound("Filter"));
        threshold = tag.contains("Threshold") ? tag.getLong("Threshold") : 64;
        below = tag.getBoolean("Below");
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Filter", filter.serializeNBT());
        tag.putLong("Threshold", threshold);
        tag.putBoolean("Below", below);
    }
}
