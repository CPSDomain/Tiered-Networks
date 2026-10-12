package com.bobroflza.tierednetworks.client;

import com.bobroflza.tierednetworks.blockentity.DriveBayBlockEntity;
import com.bobroflza.tierednetworks.menu.DriveBayMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DriveBayScreen extends AbstractContainerScreen<DriveBayMenu> {

    public DriveBayScreen(DriveBayMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < DriveBayBlockEntity.SLOTS; i++) {
            GuiDraw.slot(g, leftPos + DriveBayMenu.CELL_X + 9 + 18 * (i % 5), topPos + DriveBayMenu.CELL_Y + 18 * (i / 5));
        }
        GuiDraw.playerInventory(g, leftPos, topPos, 8, 84);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, GuiDraw.LABEL, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiDraw.LABEL, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
