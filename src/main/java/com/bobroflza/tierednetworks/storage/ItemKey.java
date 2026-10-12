package com.bobroflza.tierednetworks.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/** An item type plus its NBT, ignoring count. Used as the key for stored items. */
public record ItemKey(Item item, @Nullable CompoundTag tag) {

    public static ItemKey of(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return new ItemKey(stack.getItem(), t == null ? null : t.copy());
    }

    public ItemStack toStack(int count) {
        ItemStack s = new ItemStack(item, count);
        if (tag != null) s.setTag(tag.copy());
        return s;
    }

    public int maxStackSize() {
        return toStack(1).getMaxStackSize();
    }

    public CompoundTag save() {
        CompoundTag out = new CompoundTag();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        out.putString("id", id == null ? "minecraft:air" : id.toString());
        if (tag != null) out.put("tag", tag.copy());
        return out;
    }

    @Nullable
    public static ItemKey load(CompoundTag in) {
        ResourceLocation id = ResourceLocation.tryParse(in.getString("id"));
        if (id == null) return null;
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) return null;
        CompoundTag t = in.contains("tag") ? in.getCompound("tag") : null;
        return new ItemKey(item, t);
    }
}
