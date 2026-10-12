package com.bobroflza.tierednetworks.storage;

import com.bobroflza.tierednetworks.blockentity.DriveBayBlockEntity;
import com.bobroflza.tierednetworks.crafting.CraftingManager;
import com.bobroflza.tierednetworks.grid.NetworkTracker;
import com.bobroflza.tierednetworks.grid.NodeKind;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The combined item storage of every online Drive Bay on a network. */
public final class NetworkStorage {

    private record CellRef(DriveBayBlockEntity drive, CellInventory cell) {}

    private NetworkStorage() {}

    private static List<CellRef> onlineCells(MinecraftServer server, UUID id) {
        List<CellRef> out = new ArrayList<>();
        for (GlobalPos pos : NetworkTracker.nodesOfKind(id, NodeKind.DRIVE)) {
            if (!NetworkTracker.isOnline(server, id, pos)) continue;
            BlockEntity be = NetworkTracker.getLoadedBlockEntity(server, pos);
            if (!(be instanceof DriveBayBlockEntity drive)) continue;
            for (int i = 0; i < drive.getCells().getSlots(); i++) {
                CellInventory cell = CellInventory.of(drive.getCells().getStackInSlot(i));
                if (cell != null) out.add(new CellRef(drive, cell));
            }
        }
        return out;
    }

    public static Map<ItemKey, Long> listItems(MinecraftServer server, UUID id) {
        Map<ItemKey, Long> totals = new HashMap<>();
        for (CellRef ref : onlineCells(server, id)) {
            ref.cell().getItems().forEach((k, v) -> totals.merge(k, v, Long::sum));
        }
        return totals;
    }

    /** Totals for the terminal's status line: used, capacity. */
    public static long[] usage(MinecraftServer server, UUID id) {
        long used = 0, cap = 0;
        for (CellRef ref : onlineCells(server, id)) {
            used += ref.cell().getUsed();
            cap += ref.cell().getCapacity();
        }
        return new long[]{used, cap};
    }

    /** Inserts as much of the stack as fits. Returns what's left over. */
    public static ItemStack insert(MinecraftServer server, UUID id, ItemStack stack) {
        return insert(server, id, stack, false);
    }

    /** Inserts (or, when simulating, checks) as much of the stack as fits. Returns what's left over. */
    public static ItemStack insert(MinecraftServer server, UUID id, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemKey key = ItemKey.of(stack);
        long left = stack.getCount();
        // A running crafting job waiting for this item gets it first.
        left -= CraftingManager.offer(id, key, left, simulate);
        if (left <= 0) return ItemStack.EMPTY;
        List<CellRef> cells = onlineCells(server, id);

        // First pass: cells that already hold this item. Second pass: any cell with room.
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (CellRef ref : cells) {
                if (left <= 0) break;
                boolean holds = ref.cell().contains(key);
                if ((pass == 0) != holds) continue;
                long done = ref.cell().insert(key, left, simulate);
                if (done > 0) {
                    left -= done;
                    if (!simulate) ref.drive().onCellsChanged();
                }
            }
        }
        if (left <= 0) return ItemStack.EMPTY;
        ItemStack rest = stack.copy();
        rest.setCount((int) left);
        return rest;
    }

    /** Pulls up to {@code amount} of the item out of the network. */
    public static ItemStack extract(MinecraftServer server, UUID id, ItemKey key, int amount) {
        long left = amount;
        for (CellRef ref : onlineCells(server, id)) {
            if (left <= 0) break;
            long done = ref.cell().extract(key, left, false);
            if (done > 0) {
                left -= done;
                ref.drive().onCellsChanged();
            }
        }
        int got = (int) (amount - left);
        return got <= 0 ? ItemStack.EMPTY : key.toStack(got);
    }

    // ------------------------------------------------------------ fluids and gases

    private record FluidCellRef(DriveBayBlockEntity drive, FluidCellInventory cell) {}

    private static List<FluidCellRef> onlineFluidCells(MinecraftServer server, UUID id) {
        List<FluidCellRef> out = new ArrayList<>();
        for (GlobalPos pos : NetworkTracker.nodesOfKind(id, NodeKind.DRIVE)) {
            if (!NetworkTracker.isOnline(server, id, pos)) continue;
            BlockEntity be = NetworkTracker.getLoadedBlockEntity(server, pos);
            if (!(be instanceof DriveBayBlockEntity drive)) continue;
            for (int i = 0; i < drive.getCells().getSlots(); i++) {
                FluidCellInventory cell = FluidCellInventory.of(drive.getCells().getStackInSlot(i));
                if (cell != null) out.add(new FluidCellRef(drive, cell));
            }
        }
        return out;
    }

    /** Every fluid and gas stored, in mB. */
    public static Map<FluidKey, Long> listFluids(MinecraftServer server, UUID id) {
        Map<FluidKey, Long> totals = new HashMap<>();
        for (FluidCellRef ref : onlineFluidCells(server, id)) {
            ref.cell().getFluids().forEach((k, v) -> totals.merge(k, v, Long::sum));
        }
        return totals;
    }

    /** Fluid storage totals in mB: used, capacity. */
    public static long[] fluidUsage(MinecraftServer server, UUID id) {
        long used = 0, cap = 0;
        for (FluidCellRef ref : onlineFluidCells(server, id)) {
            used += ref.cell().getUsed();
            cap += ref.cell().getCapacity();
        }
        return new long[]{used, cap};
    }

    /** Stores (or checks) up to {@code amount} mB. Returns how much fits / was stored. */
    public static long insertFluid(MinecraftServer server, UUID id, FluidKey key, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long left = amount;
        List<FluidCellRef> cells = onlineFluidCells(server, id);
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (FluidCellRef ref : cells) {
                if (left <= 0) break;
                if ((pass == 0) != ref.cell().contains(key)) continue;
                long done = ref.cell().insert(key, left, simulate);
                if (done > 0) {
                    left -= done;
                    if (!simulate) ref.drive().onCellsChanged();
                }
            }
        }
        return amount - left;
    }

    /** Takes (or checks) up to {@code amount} mB. Returns how much came out / would. */
    public static long extractFluid(MinecraftServer server, UUID id, FluidKey key, long amount, boolean simulate) {
        long left = amount;
        for (FluidCellRef ref : onlineFluidCells(server, id)) {
            if (left <= 0) break;
            long done = ref.cell().extract(key, left, simulate);
            if (done > 0) {
                left -= done;
                if (!simulate) ref.drive().onCellsChanged();
            }
        }
        return amount - left;
    }
}
