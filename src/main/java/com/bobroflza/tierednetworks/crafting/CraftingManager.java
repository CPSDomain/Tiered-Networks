package com.bobroflza.tierednetworks.crafting;

import com.bobroflza.tierednetworks.blockentity.CraftingBlockEntity;
import com.bobroflza.tierednetworks.blockentity.PatternProviderBlockEntity;
import com.bobroflza.tierednetworks.grid.NetworkTracker;
import com.bobroflza.tierednetworks.grid.NodeKind;
import com.bobroflza.tierednetworks.storage.ItemKey;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Plans, starts and runs autocrafting jobs. */
public final class CraftingManager {
    private static final int MAX_CPU_BLOCKS = 512;
    private static final int MAX_DEPTH = 32;

    /** Blocks currently keeping a job, by network, so returning items can be caught. */
    private static final Map<UUID, Set<CraftingBlockEntity>> ACTIVE = new HashMap<>();

    private CraftingManager() {}

    public static void clear() {
        ACTIVE.clear();
    }

    public static void trackJob(CraftingBlockEntity be) {
        if (be.getNetworkId() != null) ACTIVE.computeIfAbsent(be.getNetworkId(), k -> new LinkedHashSet<>()).add(be);
    }

    public static void untrackJob(CraftingBlockEntity be) {
        for (Set<CraftingBlockEntity> set : ACTIVE.values()) set.remove(be);
        ACTIVE.values().removeIf(Set::isEmpty);
    }

    // ------------------------------------------------------------ catching returns

    /** Called for every item entering a network. Returns how many a running job keeps. */
    public static long offer(UUID network, ItemKey key, long amount, boolean simulate) {
        Set<CraftingBlockEntity> holders = ACTIVE.get(network);
        if (holders == null || holders.isEmpty()) return 0;
        long kept = 0;
        for (CraftingBlockEntity be : holders) {
            CraftingJob job = be.getJob();
            if (job == null) continue;
            long k = job.offer(key, amount - kept, simulate);
            if (k > 0) {
                kept += k;
                if (!simulate) be.setChanged();
            }
            if (kept >= amount) break;
        }
        return kept;
    }

    // ------------------------------------------------------------ patterns

    public static List<PatternProviderBlockEntity> providers(MinecraftServer server, UUID id) {
        List<PatternProviderBlockEntity> out = new ArrayList<>();
        for (GlobalPos pos : NetworkTracker.nodesOfKind(id, NodeKind.PROVIDER)) {
            if (!NetworkTracker.isOnline(server, id, pos)) continue;
            if (NetworkTracker.getLoadedBlockEntity(server, pos) instanceof PatternProviderBlockEntity p) out.add(p);
        }
        return out;
    }

    /** The first pattern on the network that makes each item. */
    public static Map<ItemKey, Pattern> patternsByOutput(MinecraftServer server, UUID id) {
        Map<ItemKey, Pattern> out = new LinkedHashMap<>();
        for (PatternProviderBlockEntity p : providers(server, id)) {
            for (Pattern pattern : p.getPatterns()) {
                for (ItemKey k : pattern.outputTotals().keySet()) out.putIfAbsent(k, pattern);
            }
        }
        return out;
    }

    // ------------------------------------------------------------ CPUs

    public static final class Cpu {
        public final List<CraftingBlockEntity> blocks = new ArrayList<>();
        public long storage;
        public int coprocessors;
        @Nullable public CraftingBlockEntity jobHolder;

        public boolean busy() { return jobHolder != null; }
        public int speed() { return 1 + coprocessors; }

        /** The block that should keep a new job: a monitor if there is one. */
        public CraftingBlockEntity preferredHolder() {
            for (CraftingBlockEntity b : blocks) if (b.getPart() == CpuPart.MONITOR) return b;
            return blocks.get(0);
        }
    }

    /** All CPU blocks touching this one (including itself). */
    public static Cpu cpuOf(CraftingBlockEntity start) {
        Cpu cpu = new Cpu();
        Level level = start.getLevel();
        if (level == null) return cpu;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.getBlockPos());
        seen.add(start.getBlockPos());
        while (!queue.isEmpty() && cpu.blocks.size() < MAX_CPU_BLOCKS) {
            BlockPos pos = queue.poll();
            if (!(level.getBlockEntity(pos) instanceof CraftingBlockEntity be)) continue;
            cpu.blocks.add(be);
            cpu.storage += be.getPart().storage();
            cpu.coprocessors += be.getPart().coprocessors();
            if (be.getJob() != null) cpu.jobHolder = be;
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (seen.add(next) && level.isLoaded(next) && level.getBlockEntity(next) instanceof CraftingBlockEntity) queue.add(next);
            }
        }
        return cpu;
    }

    public static List<Cpu> cpus(MinecraftServer server, UUID id) {
        List<Cpu> out = new ArrayList<>();
        Set<CraftingBlockEntity> covered = new HashSet<>();
        for (GlobalPos pos : NetworkTracker.nodesOfKind(id, NodeKind.CRAFTING)) {
            if (!(NetworkTracker.getLoadedBlockEntity(server, pos) instanceof CraftingBlockEntity be) || covered.contains(be)) continue;
            Cpu cpu = cpuOf(be);
            covered.addAll(cpu.blocks);
            out.add(cpu);
        }
        return out;
    }

    // ------------------------------------------------------------ planning

    public static final class Plan {
        final Map<ItemKey, Long> extract = new LinkedHashMap<>();
        final Map<ItemKey, Long> missing = new LinkedHashMap<>();
        final Map<Pattern, Long> batches = new LinkedHashMap<>();
        long items;

        public long storageNeeded() {
            long n = items;
            for (long v : extract.values()) n += v;
            return n;
        }
    }

    private static final class Planner {
        final Map<ItemKey, Long> stored;
        final Map<ItemKey, Long> surplus = new HashMap<>();
        final Map<ItemKey, Pattern> patterns;
        final Set<ItemKey> resolving = new HashSet<>();
        final Plan plan = new Plan();

        Planner(Map<ItemKey, Long> stored, Map<ItemKey, Pattern> patterns) {
            this.stored = new HashMap<>(stored);
            this.patterns = patterns;
        }

        void need(ItemKey key, long amount, int depth, boolean top) {
            long left = amount;
            if (!top) {
                long fromSurplus = Math.min(left, surplus.getOrDefault(key, 0L));
                if (fromSurplus > 0) {
                    surplus.merge(key, -fromSurplus, Long::sum);
                    left -= fromSurplus;
                }
                long fromStore = Math.min(left, stored.getOrDefault(key, 0L));
                if (fromStore > 0) {
                    stored.merge(key, -fromStore, Long::sum);
                    plan.extract.merge(key, fromStore, Long::sum);
                    left -= fromStore;
                }
            }
            if (left <= 0) return;

            Pattern p = patterns.get(key);
            if (p == null || depth > MAX_DEPTH || resolving.contains(key)) {
                plan.missing.merge(key, left, Long::sum);
                return;
            }
            long per = p.outputTotals().get(key);
            long batches = (left + per - 1) / per;
            resolving.add(key);
            for (Map.Entry<ItemKey, Long> in : p.inputTotals().entrySet()) {
                need(in.getKey(), in.getValue() * batches, depth + 1, false);
            }
            resolving.remove(key);
            plan.batches.merge(p, batches, Long::sum);
            for (Map.Entry<ItemKey, Long> out : p.outputTotals().entrySet()) {
                long made = out.getValue() * batches;
                plan.items += made;
                long spare = out.getKey().equals(key) ? made - left : made;
                if (spare > 0) surplus.merge(out.getKey(), spare, Long::sum);
            }
        }
    }

    public static Plan plan(MinecraftServer server, UUID id, ItemKey key, long amount) {
        Planner planner = new Planner(NetworkStorage.listItems(server, id), patternsByOutput(server, id));
        planner.need(key, amount, 0, true);
        return planner.plan;
    }

    // ------------------------------------------------------------ requests

    public static Component request(MinecraftServer server, UUID id, ItemKey key, long amount, ServerPlayer player) {
        Component itemName = key.toStack(1).getHoverName();
        if (amount <= 0) return Component.translatable("message.tierednetworks.craft_bad_amount").withStyle(ChatFormatting.RED);
        if (!patternsByOutput(server, id).containsKey(key)) {
            return Component.translatable("message.tierednetworks.craft_no_pattern", itemName).withStyle(ChatFormatting.RED);
        }
        Plan plan = plan(server, id, key, amount);
        if (!plan.missing.isEmpty()) {
            MutableComponent msg = Component.translatable("message.tierednetworks.craft_missing", amount, itemName).withStyle(ChatFormatting.RED);
            int shown = 0;
            for (Map.Entry<ItemKey, Long> e : plan.missing.entrySet()) {
                if (shown++ == 6) {
                    msg.append(Component.literal("\n  ..."));
                    break;
                }
                msg.append(Component.literal("\n  " + String.format("%,d", e.getValue()) + "x ").append(e.getKey().toStack(1).getHoverName()));
            }
            return msg;
        }

        long needed = plan.storageNeeded();
        List<Cpu> cpus = cpus(server, id);
        if (cpus.isEmpty()) return Component.translatable("message.tierednetworks.craft_no_cpu").withStyle(ChatFormatting.RED);
        Cpu best = null;
        for (Cpu cpu : cpus) {
            if (cpu.busy() || cpu.storage < needed || cpu.blocks.isEmpty() || !cpu.preferredHolder().isOnline()) continue;
            if (best == null || cpu.storage < best.storage) best = cpu;
        }
        if (best == null) {
            return Component.translatable("message.tierednetworks.craft_no_free_cpu", String.format("%,d", needed)).withStyle(ChatFormatting.RED);
        }

        // Take every ingredient out of storage now so nothing else can use it.
        CraftingJob job = new CraftingJob(key, amount, player.getGameProfile().getName());
        for (Map.Entry<ItemKey, Long> e : plan.extract.entrySet()) {
            long want = e.getValue();
            while (want > 0) {
                int chunk = (int) Math.min(want, Integer.MAX_VALUE);
                ItemStack got = NetworkStorage.extract(server, id, e.getKey(), chunk);
                if (got.isEmpty()) break;
                job.addStock(got);
                want -= got.getCount();
            }
            if (want > 0) {
                returnStock(server, id, job, null);
                return Component.translatable("message.tierednetworks.craft_stock_changed").withStyle(ChatFormatting.RED);
            }
        }
        for (Map.Entry<Pattern, Long> e : plan.batches.entrySet()) {
            job.steps.add(new CraftingJob.Step(e.getKey(), e.getValue(), e.getValue()));
        }
        best.preferredHolder().setJob(job);
        return Component.translatable("message.tierednetworks.craft_started", String.format("%,d", amount), itemName,
                String.format("%,d", best.storage), best.coprocessors).withStyle(ChatFormatting.GREEN);
    }

    /** Every item any pattern on the network can make. */
    public static Set<ItemKey> craftables(MinecraftServer server, UUID id) {
        return patternsByOutput(server, id).keySet();
    }

    // ------------------------------------------------------------ running

    public static void runJob(CraftingBlockEntity holder) {
        CraftingJob job = holder.getJob();
        Level level = holder.getLevel();
        if (job == null || level == null || level.getServer() == null || holder.getNetworkId() == null) return;
        if (!holder.isOnline()) return;
        MinecraftServer server = level.getServer();
        UUID id = holder.getNetworkId();

        int budget = cpuOf(holder).speed();
        List<PatternProviderBlockEntity> providers = providers(server, id);
        boolean changed = false;
        for (CraftingJob.Step step : job.steps) {
            while (step.remaining > 0 && budget > 0 && job.hasInputsFor(step.pattern)) {
                PatternProviderBlockEntity provider = null;
                for (PatternProviderBlockEntity p : providers) {
                    if (p.holds(step.pattern.tag()) && p.canRun(step.pattern)) {
                        provider = p;
                        break;
                    }
                }
                if (provider == null) break;
                job.takeInputsFor(step.pattern);
                for (ItemStack left : provider.run(step.pattern)) job.addStock(left);
                job.expectOutputsOf(step.pattern);
                step.remaining--;
                budget--;
                changed = true;
            }
            if (budget <= 0) break;
        }

        if (job.isDone()) {
            holder.setJob(null);
            returnStock(server, id, job, holder);
            ServerPlayer player = server.getPlayerList().getPlayerByName(job.requester);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.tierednetworks.craft_done",
                        String.format("%,d", job.finalAmount), job.finalKey.toStack(1).getHoverName()).withStyle(ChatFormatting.GREEN), false);
            }
        } else if (changed) {
            holder.setChanged();
        }
    }

    /** Stops the job on this CPU and puts everything it holds back in storage (or on the ground). */
    public static void cancel(CraftingBlockEntity holder, boolean blockBroken) {
        CraftingJob job = holder.getJob();
        if (job == null) return;
        Level level = holder.getLevel();
        // Stop catching items first, or the returned stock would be caught straight back.
        untrackJob(holder);
        if (!blockBroken) holder.setJob(null);
        if (level == null || level.getServer() == null) return;
        returnStock(level.getServer(), holder.getNetworkId(), job, holder);
    }

    private static void returnStock(MinecraftServer server, @Nullable UUID id, CraftingJob job, @Nullable BlockEntity dropAt) {
        for (Map.Entry<ItemKey, Long> e : new ArrayList<>(job.stock.entrySet())) {
            long left = e.getValue();
            int max = e.getKey().maxStackSize();
            while (left > 0) {
                int n = (int) Math.min(left, max);
                ItemStack rest = e.getKey().toStack(n);
                if (id != null) rest = NetworkStorage.insert(server, id, rest);
                if (!rest.isEmpty() && dropAt != null && dropAt.getLevel() != null) {
                    BlockPos p = dropAt.getBlockPos();
                    Containers.dropItemStack(dropAt.getLevel(), p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, rest);
                }
                left -= n;
            }
        }
        job.stock.clear();
    }

    // ------------------------------------------------------------ status

    public static Component describe(CraftingBlockEntity be) {
        Cpu cpu = cpuOf(be);
        MutableComponent msg = Component.translatable("message.tierednetworks.cpu_info",
                cpu.blocks.size(), String.format("%,d", cpu.storage), cpu.coprocessors, be.getStatus().display());
        CraftingJob job = cpu.jobHolder == null ? null : cpu.jobHolder.getJob();
        if (job == null) {
            return msg.append(Component.literal("\n")).append(Component.translatable("message.tierednetworks.cpu_idle").withStyle(ChatFormatting.GRAY));
        }
        msg.append(Component.literal("\n")).append(Component.translatable("message.tierednetworks.cpu_job",
                String.format("%,d", job.finalAmount), job.finalKey.toStack(1).getHoverName(), job.requester,
                job.batchesTotal() - job.batchesLeft(), job.batchesTotal(),
                String.format("%,d", job.finalAmount - job.finalNeeded)).withStyle(ChatFormatting.AQUA));
        int shown = 0;
        for (Map.Entry<ItemKey, Long> e : job.expected.entrySet()) {
            if (shown++ == 4) break;
            msg.append(Component.literal("\n  ")).append(Component.translatable("message.tierednetworks.cpu_waiting",
                    String.format("%,d", e.getValue()), e.getKey().toStack(1).getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        return msg;
    }
}
