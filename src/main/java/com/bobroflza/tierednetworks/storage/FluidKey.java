package com.bobroflza.tierednetworks.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/** A fluid type plus its NBT, ignoring amount. Gases are fluids that are lighter than air (as Tiered Industries does it). */
public record FluidKey(Fluid fluid, @Nullable CompoundTag tag) {

    public static FluidKey of(FluidStack stack) {
        CompoundTag t = stack.getTag();
        return new FluidKey(stack.getFluid(), t == null ? null : t.copy());
    }

    public FluidStack toStack(int amount) {
        FluidStack s = new FluidStack(fluid, amount);
        if (tag != null) s.setTag(tag.copy());
        return s;
    }

    public boolean isGas() {
        return isGas(fluid);
    }

    public static boolean isGas(Fluid fluid) {
        return fluid.getFluidType().isLighterThanAir();
    }

    public CompoundTag save() {
        CompoundTag out = new CompoundTag();
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        out.putString("id", id == null ? "minecraft:empty" : id.toString());
        if (tag != null) out.put("tag", tag.copy());
        return out;
    }

    @Nullable
    public static FluidKey load(CompoundTag in) {
        ResourceLocation id = ResourceLocation.tryParse(in.getString("id"));
        if (id == null) return null;
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        return new FluidKey(fluid, in.contains("tag") ? in.getCompound("tag") : null);
    }
}
