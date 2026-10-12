package com.bobroflza.tierednetworks.crafting;

import com.bobroflza.tierednetworks.storage.ItemKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One running autocrafting job, kept by a block of the Crafting CPU running it.
 * <ul>
 *   <li>{@code stock}: items the job holds - raw ingredients taken from storage at the start and
 *       intermediate results caught on their way back.</li>
 *   <li>{@code expected}: outputs of batches already sent out and not back yet.</li>
 *   <li>The requested item is let through to storage when it arrives; everything else expected is caught.</li>
 * </ul>
 */
public final class CraftingJob {

    public static final class Step {
        final Pattern pattern;
        long remaining;
        final long total;

        Step(Pattern pattern, long total, long remaining) {
            this.pattern = pattern;
            this.total = total;
            this.remaining = remaining;
        }

        public Pattern pattern() { return pattern; }
        public long remaining() { return remaining; }
        public long total() { return total; }
    }

    final ItemKey finalKey;
    final long finalAmount;
    long finalNeeded;
    final Map<ItemKey, Long> stock = new LinkedHashMap<>();
    final Map<ItemKey, Long> expected = new LinkedHashMap<>();
    final List<Step> steps = new ArrayList<>();
    final String requester;

    CraftingJob(ItemKey finalKey, long finalAmount, String requester) {
        this.finalKey = finalKey;
        this.finalAmount = finalAmount;
        this.finalNeeded = finalAmount;
        this.requester = requester;
    }

    public ItemKey finalKey() { return finalKey; }
    public long finalAmount() { return finalAmount; }
    public long finalNeeded() { return finalNeeded; }
    public String requester() { return requester; }
    public List<Step> steps() { return steps; }
    public Map<ItemKey, Long> stock() { return stock; }
    public Map<ItemKey, Long> expected() { return expected; }

    public long batchesTotal() {
        long n = 0;
        for (Step s : steps) n += s.total;
        return n;
    }

    public long batchesLeft() {
        long n = 0;
        for (Step s : steps) n += s.remaining;
        return n;
    }

    public boolean isDone() {
        return batchesLeft() == 0 && finalNeeded <= 0;
    }

    boolean hasInputsFor(Pattern p) {
        for (Map.Entry<ItemKey, Long> in : p.inputTotals().entrySet()) {
            if (stock.getOrDefault(in.getKey(), 0L) < in.getValue()) return false;
        }
        return true;
    }

    void takeInputsFor(Pattern p) {
        for (Map.Entry<ItemKey, Long> in : p.inputTotals().entrySet()) {
            stock.merge(in.getKey(), -in.getValue(), Long::sum);
            if (stock.get(in.getKey()) <= 0) stock.remove(in.getKey());
        }
    }

    void addStock(ItemStack stack) {
        if (!stack.isEmpty()) stock.merge(ItemKey.of(stack), (long) stack.getCount(), Long::sum);
    }

    void expectOutputsOf(Pattern p) {
        p.outputTotals().forEach((k, v) -> expected.merge(k, v, Long::sum));
    }

    /**
     * An item is entering the network. Returns how many of it this job keeps.
     * The requested item passes through (counts as delivered); other expected items are kept as stock.
     */
    long offer(ItemKey key, long amount, boolean simulate) {
        long exp = expected.getOrDefault(key, 0L);
        if (exp <= 0 || amount <= 0) return 0;
        long n = Math.min(amount, exp);
        long pass = key.equals(finalKey) ? Math.min(n, finalNeeded) : 0;
        long keep = n - pass;
        if (!simulate) {
            if (exp - n <= 0) expected.remove(key); else expected.put(key, exp - n);
            finalNeeded -= pass;
            if (keep > 0) stock.merge(key, keep, Long::sum);
        }
        return keep;
    }

    // ------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Final", finalKey.save());
        tag.putLong("FinalAmount", finalAmount);
        tag.putLong("FinalNeeded", finalNeeded);
        tag.putString("Requester", requester);
        tag.put("Stock", saveMap(stock));
        tag.put("Expected", saveMap(expected));
        ListTag list = new ListTag();
        for (Step s : steps) {
            CompoundTag st = new CompoundTag();
            st.put("Pattern", s.pattern.tag());
            st.putLong("Total", s.total);
            st.putLong("Remaining", s.remaining);
            list.add(st);
        }
        tag.put("Steps", list);
        return tag;
    }

    @Nullable
    public static CraftingJob load(CompoundTag tag) {
        ItemKey key = ItemKey.load(tag.getCompound("Final"));
        if (key == null) return null;
        CraftingJob job = new CraftingJob(key, tag.getLong("FinalAmount"), tag.getString("Requester"));
        job.finalNeeded = tag.getLong("FinalNeeded");
        loadMap(tag.getList("Stock", Tag.TAG_COMPOUND), job.stock);
        loadMap(tag.getList("Expected", Tag.TAG_COMPOUND), job.expected);
        ListTag list = tag.getList("Steps", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag st = list.getCompound(i);
            Pattern p = Pattern.fromTag(st.getCompound("Pattern"));
            if (p != null) job.steps.add(new Step(p, st.getLong("Total"), st.getLong("Remaining")));
        }
        return job;
    }

    private static ListTag saveMap(Map<ItemKey, Long> map) {
        ListTag list = new ListTag();
        map.forEach((k, v) -> {
            CompoundTag e = k.save();
            e.putLong("count", v);
            list.add(e);
        });
        return list;
    }

    private static void loadMap(ListTag list, Map<ItemKey, Long> into) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ItemKey k = ItemKey.load(e);
            long v = e.getLong("count");
            if (k != null && v > 0) into.merge(k, v, Long::sum);
        }
    }
}
