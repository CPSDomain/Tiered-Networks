package com.bobroflza.tierednetworks.menu;

import com.bobroflza.tierednetworks.blockentity.TerminalBlockEntity;
import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.packet.GridSyncPacket;
import com.bobroflza.tierednetworks.packet.ModPackets;
import com.bobroflza.tierednetworks.registry.ModBlocks;
import com.bobroflza.tierednetworks.registry.ModMenus;
import com.bobroflza.tierednetworks.storage.ItemKey;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TerminalMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8;
    public static final int GRID_Y = 30;
    public static final int GRID_COLS = 9;
    public static final int GRID_ROWS = 6;
    public static final int INV_Y = 152;

    /** Extract modes sent by the client. */
    public static final int MODE_STACK = 0;
    public static final int MODE_HALF = 1;
    public static final int MODE_TO_INVENTORY = 2;

    public record GridEntry(ItemStack stack, long count) {}

    private final ContainerLevelAccess access;
    @Nullable private final TerminalBlockEntity terminal;
    @Nullable private final Player player;

    // Server-side sync state.
    private int syncTimer;
    private int lastSyncHash = Integer.MIN_VALUE;

    // Client-side view of the network.
    private List<GridEntry> clientEntries = new ArrayList<>();
    private NetworkStatus clientStatus = NetworkStatus.UNLINKED;
    private long clientUsed;
    private long clientCapacity;

    public TerminalMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos());
    }

    public TerminalMenu(int id, Inventory inv, TerminalBlockEntity be) {
        this(id, inv, be, be.getBlockPos());
    }

    private TerminalMenu(int id, Inventory inv, @Nullable TerminalBlockEntity be, BlockPos pos) {
        super(ModMenus.TERMINAL.get(), id);
        this.terminal = be;
        this.player = be != null ? inv.player : null;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
        MenuHelper.addPlayerInventory(this::addSlot, inv, GRID_X, INV_Y);
    }

    // ---------------------------------------------------------------- server

    @Nullable
    private UUID onlineNetwork() {
        if (terminal == null || !terminal.isOnline()) return null;
        return terminal.getNetworkId();
    }

    @Nullable
    private MinecraftServer server() {
        return terminal != null && terminal.getLevel() != null ? terminal.getLevel().getServer() : null;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer sp) || terminal == null) return;
        if (syncTimer-- > 0) return;
        syncTimer = 10;
        sendGrid(sp, false);
    }

    private void sendGrid(ServerPlayer sp, boolean force) {
        MinecraftServer server = server();
        if (server == null) return;
        NetworkStatus status = terminal.getStatus();
        List<GridEntry> entries = new ArrayList<>();
        long used = 0, cap = 0;
        UUID id = status == NetworkStatus.ONLINE ? terminal.getNetworkId() : null;
        if (id != null) {
            for (Map.Entry<ItemKey, Long> e : NetworkStorage.listItems(server, id).entrySet()) {
                entries.add(new GridEntry(e.getKey().toStack(1), e.getValue()));
            }
            long[] usage = NetworkStorage.usage(server, id);
            used = usage[0];
            cap = usage[1];
        }
        int hash = status.ordinal() * 31 + Long.hashCode(used) * 17 + Long.hashCode(cap);
        for (GridEntry e : entries) {
            hash = hash * 31 + ItemKey.of(e.stack()).hashCode();
            hash = hash * 31 + Long.hashCode(e.count());
        }
        if (!force && hash == lastSyncHash) return;
        lastSyncHash = hash;
        ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                new GridSyncPacket(containerId, status.ordinal(), used, cap, entries));
    }

    private void resync(ServerPlayer sp) {
        syncTimer = 10;
        sendGrid(sp, true);
    }

    /** Client asked to take an item out of the network. */
    public void handleExtract(ServerPlayer sp, ItemStack request, int mode) {
        UUID id = onlineNetwork();
        MinecraftServer server = server();
        if (id == null || server == null || request.isEmpty()) return;
        ItemKey key = ItemKey.of(request);
        int max = key.maxStackSize();

        if (mode == MODE_TO_INVENTORY) {
            ItemStack got = NetworkStorage.extract(server, id, key, max);
            if (got.isEmpty()) return;
            sp.getInventory().add(got);
            if (!got.isEmpty()) {
                ItemStack back = NetworkStorage.insert(server, id, got);
                if (!back.isEmpty()) sp.drop(back, false);
            }
        } else {
            if (!getCarried().isEmpty()) return;
            int amount = max;
            if (mode == MODE_HALF) {
                long available = NetworkStorage.listItems(server, id).getOrDefault(key, 0L);
                amount = (int) Math.max(1, Math.min(max, available) / 2 + Math.min(max, available) % 2);
            }
            ItemStack got = NetworkStorage.extract(server, id, key, amount);
            if (got.isEmpty()) return;
            setCarried(got);
        }
        resync(sp);
        broadcastChanges();
    }

    /** Client clicked the grid while holding an item: put it into the network. */
    public void handleInsertCarried(ServerPlayer sp, boolean single) {
        UUID id = onlineNetwork();
        MinecraftServer server = server();
        ItemStack carried = getCarried();
        if (id == null || server == null || carried.isEmpty()) return;
        if (single) {
            ItemStack one = carried.copyWithCount(1);
            ItemStack rest = NetworkStorage.insert(server, id, one);
            if (rest.isEmpty()) carried.shrink(1);
        } else {
            setCarried(NetworkStorage.insert(server, id, carried));
        }
        resync(sp);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        // Shift-click from the player inventory sends the stack into the network.
        if (!(p instanceof ServerPlayer sp)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        UUID id = onlineNetwork();
        MinecraftServer server = server();
        if (id == null || server == null) return ItemStack.EMPTY;
        ItemStack rest = NetworkStorage.insert(server, id, slot.getItem());
        slot.set(rest);
        resync(sp);
        // Returning EMPTY stops vanilla from repeating the move.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModBlocks.TERMINAL.get());
    }

    // ---------------------------------------------------------------- client

    public void receiveGrid(int statusOrdinal, long used, long capacity, List<GridEntry> entries) {
        NetworkStatus[] values = NetworkStatus.values();
        this.clientStatus = statusOrdinal >= 0 && statusOrdinal < values.length ? values[statusOrdinal] : NetworkStatus.UNLINKED;
        this.clientUsed = used;
        this.clientCapacity = capacity;
        this.clientEntries = entries;
    }

    public List<GridEntry> getClientEntries() { return clientEntries; }
    public NetworkStatus getClientStatus() { return clientStatus; }
    public long getClientUsed() { return clientUsed; }
    public long getClientCapacity() { return clientCapacity; }
}
