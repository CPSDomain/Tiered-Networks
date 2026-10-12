package com.bobroflza.tierednetworks.grid;

import com.bobroflza.tierednetworks.Config;
import com.bobroflza.tierednetworks.blockentity.ControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side, in-memory record of which loaded blocks belong to which network.
 * Block entities register themselves when they load and unregister when they unload,
 * so nothing here needs saving.
 */
public final class NetworkTracker {

    public record NodeInfo(NodeKind kind, int powerCost, @Nullable AccessPointTier tier) {}

    /** A position that broadcasts the network: the controller or an active access point. */
    private record Broadcaster(GlobalPos pos, int range) {}

    private static final class Network {
        @Nullable GlobalPos controller;
        final Map<GlobalPos, NodeInfo> nodes = new HashMap<>();
        Set<GlobalPos> activeAccessPoints = new HashSet<>();
    }

    private static final Map<UUID, Network> NETWORKS = new HashMap<>();

    private NetworkTracker() {}

    public static void clear() {
        NETWORKS.clear();
    }

    public static void register(UUID id, GlobalPos pos, NodeInfo info) {
        Network net = NETWORKS.computeIfAbsent(id, k -> new Network());
        net.nodes.put(pos, info);
        if (info.kind() == NodeKind.CONTROLLER) {
            net.controller = pos;
        }
    }

    public static void unregister(UUID id, GlobalPos pos) {
        Network net = NETWORKS.get(id);
        if (net == null) return;
        NodeInfo removed = net.nodes.remove(pos);
        net.activeAccessPoints.remove(pos);
        if (removed != null && removed.kind() == NodeKind.CONTROLLER && pos.equals(net.controller)) {
            net.controller = null;
        }
        if (net.nodes.isEmpty()) {
            NETWORKS.remove(id);
        }
    }

    public static List<GlobalPos> nodesOfKind(UUID id, NodeKind kind) {
        Network net = NETWORKS.get(id);
        List<GlobalPos> out = new ArrayList<>();
        if (net == null) return out;
        for (Map.Entry<GlobalPos, NodeInfo> e : net.nodes.entrySet()) {
            if (e.getValue().kind() == kind) out.add(e.getKey());
        }
        return out;
    }

    public static int nodeCount(UUID id) {
        Network net = NETWORKS.get(id);
        return net == null ? 0 : net.nodes.size();
    }

    public static int activeAccessPointCount(UUID id) {
        Network net = NETWORKS.get(id);
        return net == null ? 0 : net.activeAccessPoints.size();
    }

    /** Total FE/t for every node on the network except the controller itself. */
    public static int devicePowerCost(UUID id) {
        Network net = NETWORKS.get(id);
        if (net == null) return 0;
        int total = 0;
        for (NodeInfo info : net.nodes.values()) {
            if (info.kind() != NodeKind.CONTROLLER) total += info.powerCost();
        }
        return total;
    }

    @Nullable
    public static BlockEntity getLoadedBlockEntity(MinecraftServer server, GlobalPos pos) {
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null) return null;
        BlockPos bp = pos.pos();
        if (!level.isLoaded(bp)) return null;
        return level.getBlockEntity(bp);
    }

    @Nullable
    public static ControllerBlockEntity getController(MinecraftServer server, UUID id) {
        Network net = NETWORKS.get(id);
        if (net == null || net.controller == null) return null;
        BlockEntity be = getLoadedBlockEntity(server, net.controller);
        if (be instanceof ControllerBlockEntity c && id.equals(c.getNetworkId())) return c;
        return null;
    }

    /**
     * Recomputes which access points are part of the active mesh: an access point is active
     * when the controller or another active access point covers it. Called by the controller.
     */
    public static void rebuildMesh(UUID id) {
        Network net = NETWORKS.get(id);
        if (net == null) return;
        Set<GlobalPos> active = new HashSet<>();
        if (net.controller == null) {
            net.activeAccessPoints = active;
            return;
        }

        List<Broadcaster> frontier = new ArrayList<>();
        frontier.add(new Broadcaster(net.controller, Config.CONTROLLER_RANGE.get()));

        Map<GlobalPos, AccessPointTier> pending = new HashMap<>();
        for (Map.Entry<GlobalPos, NodeInfo> e : net.nodes.entrySet()) {
            if (e.getValue().kind() == NodeKind.ACCESS_POINT && e.getValue().tier() != null) {
                pending.put(e.getKey(), e.getValue().tier());
            }
        }

        while (!frontier.isEmpty() && !pending.isEmpty()) {
            List<Broadcaster> next = new ArrayList<>();
            var it = pending.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                for (Broadcaster b : frontier) {
                    if (covers(b.pos(), b.range(), e.getKey())) {
                        active.add(e.getKey());
                        next.add(new Broadcaster(e.getKey(), e.getValue().getRange()));
                        it.remove();
                        break;
                    }
                }
            }
            frontier = next;
        }
        net.activeAccessPoints = active;
    }

    public static NetworkStatus status(MinecraftServer server, @Nullable UUID id, GlobalPos pos) {
        if (id == null) return NetworkStatus.UNLINKED;
        ControllerBlockEntity controller = getController(server, id);
        if (controller == null) return NetworkStatus.NO_CONTROLLER;
        if (!controller.isActive()) return NetworkStatus.NO_POWER;
        return isInRange(id, pos) ? NetworkStatus.ONLINE : NetworkStatus.OUT_OF_RANGE;
    }

    public static boolean isOnline(MinecraftServer server, @Nullable UUID id, GlobalPos pos) {
        return status(server, id, pos) == NetworkStatus.ONLINE;
    }

    private static boolean isInRange(UUID id, GlobalPos pos) {
        Network net = NETWORKS.get(id);
        if (net == null || net.controller == null) return false;
        if (pos.equals(net.controller)) return true;
        if (covers(net.controller, Config.CONTROLLER_RANGE.get(), pos)) return true;
        for (GlobalPos ap : net.activeAccessPoints) {
            if (ap.equals(pos)) return true;
            NodeInfo info = net.nodes.get(ap);
            if (info != null && info.tier() != null && covers(ap, info.tier().getRange(), pos)) return true;
        }
        return false;
    }

    private static boolean covers(GlobalPos from, int range, GlobalPos to) {
        if (range == AccessPointTier.ANY_DIMENSION) return true;
        if (!from.dimension().equals(to.dimension())) return false;
        if (range == AccessPointTier.DIMENSION_WIDE) return true;
        return from.pos().distSqr(to.pos()) <= (double) range * range;
    }
}
