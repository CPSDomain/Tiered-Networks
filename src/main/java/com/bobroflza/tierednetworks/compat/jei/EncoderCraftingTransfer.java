package com.bobroflza.tierednetworks.compat.jei;

import com.bobroflza.tierednetworks.menu.PatternEncoderMenu;
import com.bobroflza.tierednetworks.packet.EncoderFillPacket;
import com.bobroflza.tierednetworks.packet.ModPackets;
import com.bobroflza.tierednetworks.registry.ModMenus;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** "+" on a crafting recipe with the Pattern Encoder open: lays the recipe out in the grid, in Crafting mode. */
public class EncoderCraftingTransfer implements IRecipeTransferHandler<PatternEncoderMenu, CraftingRecipe> {

    @Override
    public Class<? extends PatternEncoderMenu> getContainerClass() {
        return PatternEncoderMenu.class;
    }

    @Override
    public Optional<MenuType<PatternEncoderMenu>> getMenuType() {
        return Optional.of(ModMenus.PATTERN_ENCODER.get());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(PatternEncoderMenu menu, CraftingRecipe recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) return null;
        List<ItemStack> grid = new ArrayList<>(9);
        for (IRecipeSlotView view : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            grid.add(JeiUtil.pick(view));
        }
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        ModPackets.CHANNEL.sendToServer(new EncoderFillPacket(menu.containerId, true, grid.subList(0, 9), List.of()));
        return null;
    }
}
