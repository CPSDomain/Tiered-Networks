package com.bobroflza.tierednetworks.compat.jei;

import com.bobroflza.tierednetworks.TieredNetworks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI's "+" button:
 * <ul>
 *   <li>Pattern Encoder, crafting recipe: fills the 3x3 grid in Crafting mode.</li>
 *   <li>Pattern Encoder, any other recipe (furnace, machines, other mods): fills a Processing pattern with amounts.</li>
 *   <li>Network Terminal or Wireless Terminal, any recipe: asks how many to autocraft of what it makes.</li>
 * </ul>
 * Only loaded by JEI itself, so nothing here runs without JEI installed.
 */
@JeiPlugin
public class TieredNetworksJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(TieredNetworks.MODID, "jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new EncoderCraftingTransfer(), RecipeTypes.CRAFTING);
        registration.addUniversalRecipeTransferHandler(new EncoderProcessingTransfer());
        registration.addUniversalRecipeTransferHandler(new TerminalCraftTransfer(registration.getTransferHelper()));
    }
}
