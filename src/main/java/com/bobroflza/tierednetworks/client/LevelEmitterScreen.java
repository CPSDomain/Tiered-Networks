package com.bobroflza.tierednetworks.client;

import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.menu.LevelEmitterMenu;
import com.bobroflza.tierednetworks.packet.EmitterThresholdPacket;
import com.bobroflza.tierednetworks.packet.ModPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class LevelEmitterScreen extends AbstractContainerScreen<LevelEmitterMenu> {
    private static final int RIGHT_X = 62;

    private EditBox amount;
    private Button modeButton;

    public LevelEmitterScreen(LevelEmitterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        amount = new EditBox(font, leftPos + RIGHT_X, topPos + 22, 106, 14, Component.translatable("gui.tierednetworks.threshold"));
        amount.setMaxLength(15);
        amount.setFilter(s -> s.matches("\\d{0,15}"));
        amount.setValue(Long.toString(menu.getInitialThreshold()));
        amount.setResponder(s -> {
            long v = s.isEmpty() ? 0 : Long.parseLong(s);
            ModPackets.CHANNEL.sendToServer(new EmitterThresholdPacket(menu.containerId, v));
        });
        amount.setTooltip(Tooltip.create(Component.translatable("gui.tierednetworks.threshold")));
        addRenderableWidget(amount);

        modeButton = Button.builder(modeLabel(), b -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LevelEmitterMenu.BUTTON_MODE);
                    }
                })
                .bounds(leftPos + RIGHT_X, topPos + 40, 106, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.tierednetworks.emitter_help")))
                .build();
        addRenderableWidget(modeButton);
    }

    private Component modeLabel() {
        return Component.translatable(menu.isBelowMode() ? "gui.tierednetworks.emit_below" : "gui.tierednetworks.emit_at_least");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel());
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        GuiDraw.slot(g, leftPos + LevelEmitterMenu.FILTER_X, topPos + LevelEmitterMenu.FILTER_Y);
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
        Component st = Component.translatable(status.translationKey());
        g.drawString(font, st, imageWidth - 8 - font.width(st), titleLabelY, color, false);

        boolean any = menu.getSlot(0).getItem().isEmpty();
        g.drawString(font, Component.translatable(any ? "gui.tierednetworks.emitter_all" : "gui.tierednetworks.emitter_item"),
                8, 46, GuiDraw.LABEL, false);
        g.drawString(font, Component.translatable("gui.tierednetworks.stored", String.format("%,d", menu.getCount())),
                RIGHT_X, 61, GuiDraw.LABEL, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (amount.isFocused()) {
            amount.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
