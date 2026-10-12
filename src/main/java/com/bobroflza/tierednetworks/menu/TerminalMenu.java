package com.bobroflza.tierednetworks.menu;

import com.bobroflza.tierednetworks.crafting.CraftingManager;
import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.packet.GridSyncPacket;
import com.bobroflza.tierednetworks.packet.ModPackets;
import com.bobroflza.tierednetworks.registry.ModMenus;
import com.bobroflza.tierednetworks.storage.FluidKey;
import com.bobroflza.tierednetworks.storage.ItemKey;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

    public record GridEntry(ItemStack stack, long count, boolean craftable) {}

    /** A stored fluid or gas: {@code fluid} has amount 1000 and only names the type; {@code amount} is the real total in mB. */
    public record FluidEntry(FluidStack fluid, long amount) {}

    /** Null on the client. */
    @Nullable private final TerminalHost host;
    @Nullable private final Player player;

    // Server-side sync state.
    private int syncTimer;
    private int lastSyncHash = Integer.MIN_VALUE;

    // Client-side view of the network.
    private List<GridEntry> clientEntries = new ArrayList<>();
    private NetworkStatus clientStatus = NetworkStatus.UNLINKED;
    private long clientUsed;
    private long clientCapacity;
    private List<FluidEntry> clientFluids = new ArrayList<>();
    private long clientFluidUsed;
    private long clientFluidCapacity;

    /** Client side: the server sends no extra data. */
    public TerminalMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TerminalHost) null);
    }

    /** Server side. */
    public TerminalMenu(int id, Inventory inv, @Nullable TerminalHost host) {
        super(ModMenus.TERMINAL.get(), id);
        this.host = host;
        this.player = host != null ? inv.player : null;
        MenuHelper.addPlayerInventory(this::addSlot, inv, GRID_X, INV_Y);
    }

    // ---------------------------------------------------------------- server

    @Nullable
    private UUID onlineNetwork() {
        if (host == null || host.hostStatus() != NetworkStatus.ONLINE) return null;
        return host.hostNetwork();
    }

    @Nullable
    private MinecraftServer server() {
        return host == null ? null : host.hostServer();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer sp) || host == null) return;
        if (syncTimer-- > 0) return;
        syncTimer = 10;
        sendGrid(sp, false);
    }

    private void sendGrid(ServerPlayer sp, boolean force) {
        MinecraftServer server = server();
        if (server == null) return;
        NetworkStatus status = host.hostStatus();
        List<GridEntry> entries = new ArrayList<>();
        List<FluidEntry> fluids = new ArrayList<>();
        long used = 0, cap = 0, fUsed = 0, fCap = 0;
        UUID id = status == NetworkStatus.ONLINE ? host.hostNetwork() : null;
        if (id != null) {
            java.util.Set<ItemKey> craftable = new java.util.HashSet<>(CraftingManager.craftables(server, id));
            for (Map.Entry<ItemKey, Long> e : NetworkStorage.listItems(server, id).entrySet()) {
                entries.add(new GridEntry(e.getKey().toStack(1), e.getValue(), craftable.remove(e.getKey())));
            }
            // Craftable items with none in stock still show, so they can be requested.
            for (ItemKey k : craftable) entries.add(new GridEntry(k.toStack(1), 0, true));
            long[] usage = NetworkStorage.usage(server, id);
            used = usage[0];
            cap = usage[1];
            for (Map.Entry<FluidKey, Long> e : NetworkStorage.listFluids(server, id).entrySet()) {
                fluids.add(new FluidEntry(e.getKey().toStack(1000), e.getValue()));
            }
            long[] fUsage = NetworkStorage.fluidUsage(server, id);
            fUsed = fUsage[0];
            fCap = fUsage[1];
        }
        int hash = status.ordinal() * 31 + Long.hashCode(used) * 17 + Long.hashCode(cap);
        for (GridEntry e : entries) {
            hash = hash * 31 + ItemKey.of(e.stack()).hashCode();
            hash = hash * 31 + Long.hashCode(e.count()) + (e.craftable() ? 7 : 0);
        }
        hash = hash * 31 + Long.hashCode(fUsed) * 13 + Long.hashCode(fCap);
        for (FluidEntry e : fluids) {
            hash = hash * 31 + FluidKey.of(e.fluid()).hashCode();
            hash = hash * 31 + Long.hashCode(e.amount());
        }
        if (!force && hash == lastSyncHash) return;
        lastSyncHash = hash;
        ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                new GridSyncPacket(containerId, status.ordinal(), used, cap, entries, fUsed, fCap, fluids));
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

    /**
     * Client clicked the fluid grid holding a container. A container with fluid in it empties into the network;
     * an empty one fills with the clicked fluid. Stacked containers are done one at a time.
     */
    public void handleFluidClick(ServerPlayer sp, FluidStack target) {
        UUID id = onlineNetwork();
        MinecraftServer server = server();
        ItemStack carried = getCarried();
        if (id == null || server == null || carried.isEmpty()) return;
        ItemStack one = carried.copyWithCount(1);
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(one).resolve().orElse(null);
        if (handler == null) return;

        boolean changed = false;
        FluidStack inside = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (!inside.isEmpty()) {
            FluidKey key = FluidKey.of(inside);
            long fits = NetworkStorage.insertFluid(server, id, key, inside.getAmount(), true);
            if (fits > 0) {
                FluidStack drained = handler.drain(key.toStack((int) fits), IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    NetworkStorage.insertFluid(server, id, key, drained.getAmount(), false);
                    changed = true;
                }
            }
        } else if (target != null && !target.isEmpty()) {
            FluidKey key = FluidKey.of(target);
            long have = NetworkStorage.extractFluid(server, id, key, Integer.MAX_VALUE, true);
            int room = have <= 0 ? 0 : handler.fill(key.toStack((int) Math.min(have, Integer.MAX_VALUE)), IFluidHandler.FluidAction.SIMULATE);
            if (room > 0) {
                long got = NetworkStorage.extractFluid(server, id, key, room, false);
                int filled = handler.fill(key.toStack((int) got), IFluidHandler.FluidAction.EXECUTE);
                if (filled < got) NetworkStorage.insertFluid(server, id, key, got - filled, false);
                changed = filled > 0;
            }
        }
        if (!changed) return;
        ItemStack result = handler.getContainer();
        if (carried.getCount() == 1) {
            setCarried(result);
        } else {
            carried.shrink(1);
            if (!sp.getInventory().add(result)) sp.drop(result, false);
        }
        resync(sp);
        broadcastChanges();
    }

    /** Client asked to craft some of an item. */
    public void handleCraftRequest(ServerPlayer sp, ItemStack request, long amount) {
        UUID id = onlineNetwork();
        MinecraftServer server = server();
        if (id == null || server == null || request.isEmpty()) return;
        sp.displayClientMessage(CraftingManager.request(server, id, ItemKey.of(request), amount, sp), false);
        resync(sp);
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
        return host == null || host.hostStillValid(p);
    }

    // ---------------------------------------------------------------- client

    public void receiveGrid(int statusOrdinal, long used, long capacity, List<GridEntry> entries,
                            long fluidUsed, long fluidCapacity, List<FluidEntry> fluids) {
        this.clientFluidUsed = fluidUsed;
        this.clientFluidCapacity = fluidCapacity;
        this.clientFluids = fluids;
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
    public List<FluidEntry> getClientFluids() { return clientFluids; }
    public long getClientFluidUsed() { return clientFluidUsed; }
    public long getClientFluidCapacity() { return clientFluidCapacity; }
}
