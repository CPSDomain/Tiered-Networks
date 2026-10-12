package com.bobroflza.tierednetworks.client;

import com.bobroflza.tierednetworks.crafting.Pattern;
import com.bobroflza.tierednetworks.menu.PatternEncoderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PatternEncoderScreen extends AbstractContainerScreen<PatternEncoderMenu> {
    private Button modeButton;
    private List<ItemStack> lastGrid;
    private ItemStack preview = ItemStack.EMPTY;

    public PatternEncoderScreen(PatternEncoderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = PatternEncoderMenu.INV_Y + 82;
        this.inventoryLabelY = PatternEncoderMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(modeLabel(), b -> press(PatternEncoderMenu.BUTTON_MODE))
                .bounds(leftPos + 8, topPos + 74, 66, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.tierednetworks.encoder_mode_help"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tierednetworks.clear"), b -> press(PatternEncoderMenu.BUTTON_CLEAR))
                .bounds(leftPos + 78, topPos + 74, 40, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tierednetworks.encode"), b -> press(PatternEncoderMenu.BUTTON_ENCODE))
                .bounds(leftPos + 122, topPos + 74, 46, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.tierednetworks.encode_help"))).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component modeLabel() {
        return Component.translatable(menu.isCrafting() ? "gui.tierednetworks.mode_crafting" : "gui.tierednetworks.mode_processing");
    }

    private void refreshPreview() {
        if (!menu.isCrafting() || minecraft == null || minecraft.level == null) {
            preview = ItemStack.EMPTY;
            lastGrid = null;
            return;
        }
        List<ItemStack> grid = menu.gridStacks();
        if (lastGrid != null && sameGrid(lastGrid, grid)) return;
        lastGrid = grid.stream().map(ItemStack::copy).toList();
        preview = Pattern.craftingResult(minecraft.level, grid);
    }

    private static boolean sameGrid(List<ItemStack> a, List<ItemStack> b) {
        for (int i = 0; i < 9; i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel());
        refreshPreview();
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        if (menu.isCrafting() && !preview.isEmpty()) {
            int x = leftPos + PatternEncoderMenu.OUT_X, y = topPos + PatternEncoderMenu.OUT_Y + 18;
            g.renderItem(preview, x, y);
            g.renderItemDecorations(font, preview, x, y);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) g.renderTooltip(font, preview, mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < 9; i++) {
            GuiDraw.slot(g, leftPos + PatternEncoderMenu.GRID_X + (i % 3) * 18, topPos + PatternEncoderMenu.GRID_Y + (i / 3) * 18);
        }
        // Arrow from the grid to the outputs.
        int ax = leftPos + 68, ay = topPos + PatternEncoderMenu.GRID_Y + 22;
        g.fill(ax, ay + 3, ax + 18, ay + 6, 0xFF8B8B8B);
        for (int i = 0; i < 5; i++) g.fill(ax + 18 + i, ay - 1 + i, ax + 19 + i, ay + 10 - i, 0xFF8B8B8B);

        if (menu.isCrafting()) {
            GuiDraw.slot(g, leftPos + PatternEncoderMenu.OUT_X, topPos + PatternEncoderMenu.OUT_Y + 18);
        } else {
            for (int i = 0; i < 3; i++) GuiDraw.slot(g, leftPos + PatternEncoderMenu.OUT_X, topPos + PatternEncoderMenu.OUT_Y + i * 18);
        }
        GuiDraw.slot(g, leftPos + PatternEncoderMenu.BLANK_X, topPos + PatternEncoderMenu.BLANK_Y);
        GuiDraw.slot(g, leftPos + PatternEncoderMenu.RESULT_X, topPos + PatternEncoderMenu.RESULT_Y);
        // Down arrow from blank to encoded.
        int dx = leftPos + PatternEncoderMenu.BLANK_X + 5, dy = topPos + PatternEncoderMenu.BLANK_Y + 20;
        g.fill(dx + 2, dy, dx + 4, dy + 8, 0xFF8B8B8B);
        for (int i = 0; i < 4; i++) g.fill(dx - 1 + i, dy + 8 + i, dx + 7 - i, dy + 9 + i, 0xFF8B8B8B);

        GuiDraw.playerInventory(g, leftPos, topPos, 8, PatternEncoderMenu.INV_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, GuiDraw.LABEL, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiDraw.LABEL, false);
    }
}
