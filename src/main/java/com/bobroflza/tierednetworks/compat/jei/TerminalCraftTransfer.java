package com.bobroflza.tierednetworks.compat.jei;

import com.bobroflza.tierednetworks.client.CraftAmountScreen;
import com.bobroflza.tierednetworks.client.TerminalScreen;
import com.bobroflza.tierednetworks.menu.TerminalMenu;
import com.bobroflza.tierednetworks.registry.ModMenus;
import com.bobroflza.tierednetworks.storage.ItemKey;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * "+" on any recipe with a Network Terminal or Wireless Terminal open: if a pattern on the network makes the
 * recipe's result, asks how many to craft and starts the job. The "+" turns red when nothing can make it.
 */
public class TerminalCraftTransfer implements IUniversalRecipeTransferHandler<TerminalMenu> {
    private final IRecipeTransferHandlerHelper helper;

    public TerminalCraftTransfer(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends TerminalMenu> getContainerClass() {
        return TerminalMenu.class;
    }

    @Override
    public Optional<MenuType<TerminalMenu>> getMenuType() {
        return Optional.of(ModMenus.TERMINAL.get());
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(TerminalMenu menu, Object recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        ItemStack result = ItemStack.EMPTY;
        for (IRecipeSlotView v : slots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
            result = JeiUtil.pick(v);
            if (!result.isEmpty()) break;
        }
        if (result.isEmpty()) {
            return helper.createUserErrorWithTooltip(Component.translatable("jei.tierednetworks.no_output"));
        }
        ItemKey key = ItemKey.of(result);
        boolean craftable = menu.getClientEntries().stream()
                .anyMatch(e -> e.craftable() && ItemKey.of(e.stack()).equals(key));
        if (!craftable) {
            return helper.createUserErrorWithTooltip(Component.translatable("jei.tierednetworks.not_craftable", result.getHoverName()));
        }
        if (!doTransfer) return null;

        ItemStack request = result.copyWithCount(1);
        int amount = maxTransfer ? 64 : Math.max(1, result.getCount());
        int containerId = menu.containerId;
        // JEI goes back to the terminal after this returns; open the amount box once it has.
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            if (mc.screen instanceof TerminalScreen terminal) {
                mc.setScreen(new CraftAmountScreen(terminal, containerId, request, amount));
            }
        });
        return null;
    }
}
