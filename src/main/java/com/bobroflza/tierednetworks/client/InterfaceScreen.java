package com.bobroflza.tierednetworks.client;

import com.bobroflza.tierednetworks.blockentity.InterfaceBlockEntity;
import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.menu.InterfaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class InterfaceScreen extends AbstractContainerScreen<InterfaceMenu> {
    private static final int INFO_X = 70;

    private Button modeButton;

    public InterfaceScreen(InterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (!menu.isExport()) {
            modeButton = Button.builder(modeLabel(), b -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, InterfaceMenu.BUTTON_MODE);
                        }
                    })
                    .bounds(leftPos + INFO_X, topPos + 34, 98, 16)
                    .tooltip(Tooltip.create(Component.translatable("gui.tierednetworks.mode_help")))
                    .build();
            addRenderableWidget(modeButton);
        }
    }

    private Component modeLabel() {
        return Component.translatable(menu.isDenyList() ? "gui.tierednetworks.mode_deny" : "gui.tierednetworks.mode_allow");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (modeButton != null) modeButton.setMessage(modeLabel());
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < InterfaceBlockEntity.FILTER_SLOTS; i++) {
            GuiDraw.slot(g, leftPos + InterfaceMenu.FILTER_X + (i % 3) * 18, topPos + InterfaceMenu.FILTER_Y + (i / 3) * 18);
        }
        GuiDraw.playerInventory(g, leftPos, topPos, 8, 84);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, GuiDraw.LABEL, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiDraw.LABEL, false);

        NetworkStatus status = menu.getStatus();
        int color = switch (status) {
            case ONLINE -> 0xFF1E7F1E;
            case OUT_OF_RANGE -> 0xFF8A5A00;
            default -> 0xFFA02020;
        };
        g.drawString(font, Component.translatable(status.translationKey()), INFO_X, 21, color, false);

        String hint1 = menu.isExport() ? "gui.tierednetworks.export_hint1" : "gui.tierednetworks.import_hint1";
        String hint2 = menu.isExport() ? "gui.tierednetworks.export_hint2" : "gui.tierednetworks.import_hint2";
        int y = menu.isExport() ? 36 : 54;
        g.drawString(font, Component.translatable(hint1), INFO_X, y, GuiDraw.LABEL, false);
        g.drawString(font, Component.translatable(hint2), INFO_X, y + 10, GuiDraw.LABEL, false);
    }
}
