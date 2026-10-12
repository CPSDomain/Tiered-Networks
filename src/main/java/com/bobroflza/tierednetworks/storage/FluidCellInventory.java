package com.bobroflza.tierednetworks.storage;

import com.bobroflza.tierednetworks.item.FluidCellItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads and writes the fluids (or gases) stored in a fluid or gas cell's NBT. */
public final class FluidCellInventory {
    private static final String TAG_FLUIDS = "StoredFluids";
    private static final String TAG_AMOUNT = "amount";

    private final ItemStack cell;
    private final FluidCellItem item;
    private final Map<FluidKey, Long> fluids = new LinkedHashMap<>();
    private long used;

    private FluidCellInventory(ItemStack cell, FluidCellItem item) {
        this.cell = cell;
        this.item = item;
        load();
    }

    public static FluidCellInventory of(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FluidCellItem f)) return null;
        return new FluidCellInventory(stack, f);
    }

    private void load() {
        fluids.clear();
        used = 0;
        CompoundTag tag = cell.getTag();
        if (tag == null || !tag.contains(TAG_FLUIDS, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(TAG_FLUIDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            FluidKey key = FluidKey.load(e);
            long amount = e.getLong(TAG_AMOUNT);
            if (key != null && amount > 0) {
                fluids.merge(key, amount, Long::sum);
                used += amount;
            }
        }
    }

    private void save() {
        ListTag list = new ListTag();
        fluids.forEach((k, v) -> {
            CompoundTag e = k.save();
            e.putLong(TAG_AMOUNT, v);
            list.add(e);
        });
        if (list.isEmpty()) {
            CompoundTag tag = cell.getTag();
            if (tag != null) {
                tag.remove(TAG_FLUIDS);
                if (tag.isEmpty()) cell.setTag(null);
            }
        } else {
            cell.getOrCreateTag().put(TAG_FLUIDS, list);
        }
    }

    public Map<FluidKey, Long> getFluids() { return Collections.unmodifiableMap(fluids); }
    public long getUsed() { return used; }
    public long getCapacity() { return item.getCapacity(); }
    public int getTypeCount() { return fluids.size(); }
    public boolean contains(FluidKey key) { return fluids.containsKey(key); }

    /** Gas cells only take gases; fluid cells only take liquids. */
    public boolean accepts(FluidKey key) {
        return key.isGas() == item.isGas();
    }

    public long insert(FluidKey key, long amount, boolean simulate) {
        if (amount <= 0 || !accepts(key)) return 0;
        if (!fluids.containsKey(key) && fluids.size() >= FluidCellItem.MAX_TYPES) return 0;
        long n = Math.min(amount, getCapacity() - used);
        if (n <= 0) return 0;
        if (!simulate) {
            fluids.merge(key, n, Long::sum);
            used += n;
            save();
        }
        return n;
    }

    public long extract(FluidKey key, long amount, boolean simulate) {
        Long have = fluids.get(key);
        if (have == null || amount <= 0) return 0;
        long n = Math.min(have, amount);
        if (!simulate) {
            if (have - n <= 0) fluids.remove(key); else fluids.put(key, have - n);
            used -= n;
            save();
        }
        return n;
    }
}
