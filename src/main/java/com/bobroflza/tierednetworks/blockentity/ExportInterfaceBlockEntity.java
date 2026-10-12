package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.storage.FluidKey;
import com.bobroflza.tierednetworks.storage.ItemKey;
import net.minecraftforge.fluids.capability.IFluidHandler;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pushes the items in its filter out of the network into neighbouring inventories.
 * An empty filter exports nothing. Filter entries take turns so one item can't hog every transfer.
 */
public class ExportInterfaceBlockEntity extends InterfaceBlockEntity {
    private int nextFilter;

    public ExportInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPORT_INTERFACE.get(), pos, state);
    }

    @Override
    public boolean isExport() {
        return true;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.EXPORT;
    }

    @Override
    protected void transfer(MinecraftServer server, UUID id, List<Target> targets, int budget) {
        List<ItemStack> wanted = filterItems();
        if (wanted.isEmpty()) return;
        Map<ItemKey, Long> stored = NetworkStorage.listItems(server, id);
        if (stored.isEmpty()) return;

        int start = nextFilter % wanted.size();
        nextFilter = start + 1;
        for (int n = 0; n < wanted.size() && budget > 0; n++) {
            ItemStack want = wanted.get((start + n) % wanted.size());
            for (Map.Entry<ItemKey, Long> entry : stored.entrySet()) {
                if (budget <= 0) break;
                ItemKey key = entry.getKey();
                if (key.item() != want.getItem()) continue;
                long have = entry.getValue();
                for (Target target : targets) {
                    if (budget <= 0 || have <= 0) break;
                    budget -= pushInto(server, id, key, target.handler(), (int) Math.min(budget, Math.min(have, key.maxStackSize())));
                    have = NetworkStorage.listItems(server, id).getOrDefault(key, 0L);
                }
            }
        }
    }

    @Override
    protected void transferFluids(MinecraftServer server, UUID id, List<FluidTarget> targets, int budget) {
        List<FluidKey> wanted = filterFluids();
        if (wanted.isEmpty()) return;
        Map<FluidKey, Long> stored = NetworkStorage.listFluids(server, id);
        for (FluidKey key : wanted) {
            if (budget <= 0) return;
            long have = stored.getOrDefault(key, 0L);
            for (FluidTarget target : targets) {
                if (budget <= 0 || have <= 0) break;
                int offer = (int) Math.min(budget, have);
                int room = target.handler().fill(key.toStack(offer), IFluidHandler.FluidAction.SIMULATE);
                if (room <= 0) continue;
                long got = NetworkStorage.extractFluid(server, id, key, room, false);
                if (got <= 0) continue;
                int filled = target.handler().fill(key.toStack((int) got), IFluidHandler.FluidAction.EXECUTE);
                if (filled < got) NetworkStorage.insertFluid(server, id, key, got - filled, false);
                budget -= filled;
                have -= filled;
            }
        }
    }

    /** Moves up to {@code amount} of one item into one inventory. Returns how many moved. */
    private int pushInto(MinecraftServer server, UUID id, ItemKey key, IItemHandler dest, int amount) {
        if (amount <= 0) return 0;
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(dest, key.toStack(amount), true);
        int fits = amount - remainder.getCount();
        if (fits <= 0) return 0;

        ItemStack pulled = NetworkStorage.extract(server, id, key, fits);
        if (pulled.isEmpty()) return 0;
        int pulledCount = pulled.getCount();
        ItemStack left = ItemHandlerHelper.insertItemStacked(dest, pulled, false);
        if (!left.isEmpty()) {
            // The inventory took less than it promised: put the rest back in the network.
            ItemStack back = NetworkStorage.insert(server, id, left, false);
            if (!back.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                        worldPosition.getZ() + 0.5, back);
            }
        }
        return pulledCount - left.getCount();
    }
}
