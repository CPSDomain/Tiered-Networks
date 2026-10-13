package com.bobroflza.tierednetworks.compat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.world.item.ItemStack;

final class JeiUtil {
    private JeiUtil() {}

    /** The item a recipe slot shows right now (its first option if it isn't cycling), or EMPTY. */
    static ItemStack pick(IRecipeSlotView view) {
        return view.getDisplayedItemStack()
                .or(() -> view.getItemStacks().filter(s -> !s.isEmpty()).findFirst())
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }
}
