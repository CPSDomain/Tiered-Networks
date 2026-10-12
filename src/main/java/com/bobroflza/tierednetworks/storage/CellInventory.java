package com.bobroflza.tierednetworks.storage;

import com.bobroflza.tierednetworks.item.StorageCellItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads and writes the items stored inside a storage cell ItemStack's NBT. */
public final class CellInventory {
    private static final String TAG_ITEMS = "StoredItems";
    private static final String TAG_COUNT = "count";

    private final ItemStack cell;
    private final CellTier tier;
    private final Map<ItemKey, Long> items = new LinkedHashMap<>();
    private long used;

    private CellInventory(ItemStack cell, CellTier tier) {
        this.cell = cell;
        this.tier = tier;
        load();
    }

    /** Returns null when the stack isn't a storage cell. */
    public static CellInventory of(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof StorageCellItem cellItem)) return null;
        return new CellInventory(stack, cellItem.getTier());
    }

    private void load() {
        items.clear();
        used = 0;
        CompoundTag tag = cell.getTag();
        if (tag == null || !tag.contains(TAG_ITEMS, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ItemKey key = ItemKey.load(entry);
            long count = entry.getLong(TAG_COUNT);
            if (key != null && count > 0) {
                items.merge(key, count, Long::sum);
                used += count;
            }
        }
    }

    private void save() {
        ListTag list = new ListTag();
        for (Map.Entry<ItemKey, Long> e : items.entrySet()) {
            CompoundTag entry = e.getKey().save();
            entry.putLong(TAG_COUNT, e.getValue());
            list.add(entry);
        }
        if (list.isEmpty()) {
            CompoundTag tag = cell.getTag();
            if (tag != null) {
                tag.remove(TAG_ITEMS);
                if (tag.isEmpty()) cell.setTag(null);
            }
        } else {
            cell.getOrCreateTag().put(TAG_ITEMS, list);
        }
    }

    public Map<ItemKey, Long> getItems() { return Collections.unmodifiableMap(items); }
    public CellTier getTier() { return tier; }
    public long getUsed() { return used; }
    public long getCapacity() { return tier.getCapacity(); }
    public int getTypeCount() { return items.size(); }
    public boolean contains(ItemKey key) { return items.containsKey(key); }

    /** Can this cell accept a new item type it doesn't already hold? */
    public boolean canAcceptNewType() { return items.size() < CellTier.MAX_TYPES; }

    /** @return how many were (or would be) inserted. */
    public long insert(ItemKey key, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        if (key.toStack(1).getItem() instanceof StorageCellItem) {
            // No storing cells inside cells.
            return 0;
        }
        if (!items.containsKey(key) && !canAcceptNewType()) return 0;
        long space = getCapacity() - used;
        long toInsert = Math.min(space, amount);
        if (toInsert <= 0) return 0;
        if (!simulate) {
            items.merge(key, toInsert, Long::sum);
            used += toInsert;
            save();
        }
        return toInsert;
    }

    /** @return how many were (or would be) extracted. */
    public long extract(ItemKey key, long amount, boolean simulate) {
        Long have = items.get(key);
        if (have == null || amount <= 0) return 0;
        long toExtract = Math.min(have, amount);
        if (!simulate) {
            long left = have - toExtract;
            if (left <= 0) items.remove(key); else items.put(key, left);
            used -= toExtract;
            save();
        }
        return toExtract;
    }
}
