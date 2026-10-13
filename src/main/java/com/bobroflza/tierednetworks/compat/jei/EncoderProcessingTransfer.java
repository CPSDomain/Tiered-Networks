package com.bobroflza.tierednetworks.compat.jei;

import com.bobroflza.tierednetworks.menu.PatternEncoderMenu;
import com.bobroflza.tierednetworks.packet.EncoderFillPacket;
import com.bobroflza.tierednetworks.packet.ModPackets;
import com.bobroflza.tierednetworks.registry.ModMenus;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "+" on any non-crafting recipe (furnace, Tiered Industries machines, other mods) with the Pattern Encoder open:
 * copies the item inputs and outputs, with their amounts, into a Processing pattern.
 */
public class EncoderProcessingTransfer implements IUniversalRecipeTransferHandler<PatternEncoderMenu> {

    @Override
    public Class<? extends PatternEncoderMenu> getContainerClass() {
        return PatternEncoderMenu.class;
    }

    @Override
    public Optional<MenuType<PatternEncoderMenu>> getMenuType() {
        return Optional.of(ModMenus.PATTERN_ENCODER.get());
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(PatternEncoderMenu menu, Object recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) return null;
        List<ItemStack> in = new ArrayList<>();
        List<ItemStack> out = new ArrayList<>();
        for (IRecipeSlotView v : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            ItemStack s = JeiUtil.pick(v);
            if (!s.isEmpty() && in.size() < 9) in.add(s);
        }
        for (IRecipeSlotView v : slots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
            ItemStack s = JeiUtil.pick(v);
            if (!s.isEmpty() && out.size() < 3) out.add(s);
        }
        if (in.isEmpty() || out.isEmpty()) return null;
        ModPackets.CHANNEL.sendToServer(new EncoderFillPacket(menu.containerId, false, in, out));
        return null;
    }
}
