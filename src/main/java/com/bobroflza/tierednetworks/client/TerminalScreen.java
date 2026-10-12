package com.bobroflza.tierednetworks.client;

import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.menu.TerminalMenu;
import com.bobroflza.tierednetworks.packet.GridExtractPacket;
import com.bobroflza.tierednetworks.packet.GridInsertPacket;
import com.bobroflza.tierednetworks.packet.ModPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int SCROLL_X = TerminalMenu.GRID_X + TerminalMenu.GRID_COLS * 18 + 4;
    private static final int SCROLL_W = 12;
    private static final int GRID_H = TerminalMenu.GRID_ROWS * 18;

    private EditBox search;
    private int scrollRow;
    private List<TerminalMenu.GridEntry> view = new ArrayList<>();
    private List<TerminalMenu.GridEntry> lastSource;
    private String lastQuery = "";

    public TerminalScreen(TerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 195;
        this.imageHeight = TerminalMenu.INV_Y + 82;
        this.inventoryLabelY = TerminalMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 82, topPos + 5, 90, 12, Component.translatable("gui.tierednetworks.search"));
        search.setMaxLength(50);
        search.setBordered(true);
        search.setHint(Component.translatable("gui.tierednetworks.search").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(search);
    }

    // ---------------------------------------------------------------- view

    private void refreshView() {
        List<TerminalMenu.GridEntry> source = menu.getClientEntries();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (source == lastSource && query.equals(lastQuery)) return;
        lastSource = source;
        lastQuery = query;

        List<TerminalMenu.GridEntry> out = new ArrayList<>();
        for (TerminalMenu.GridEntry e : source) {
            if (matches(e.stack(), query)) out.add(e);
        }
        Comparator<TerminalMenu.GridEntry> byCount = Comparator.comparingLong(TerminalMenu.GridEntry::count);
        Comparator<TerminalMenu.GridEntry> byName = Comparator.comparing(e -> e.stack().getHoverName().getString());
        out.sort(byCount.reversed().thenComparing(byName));
        view = out;
        scrollRow = Mth.clamp(scrollRow, 0, maxScroll());
    }

    private static boolean matches(ItemStack stack, String query) {
        if (query.isEmpty()) return true;
        if (query.startsWith("@")) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id != null && id.getNamespace().contains(query.substring(1));
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private int totalRows() {
        return (view.size() + TerminalMenu.GRID_COLS - 1) / TerminalMenu.GRID_COLS;
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - TerminalMenu.GRID_ROWS);
    }

    /** Index into {@link #view} under the mouse, -1 if outside the grid, -2 if on an empty grid cell. */
    private int gridIndexAt(double mouseX, double mouseY) {
        int gx = (int) mouseX - leftPos - TerminalMenu.GRID_X;
        int gy = (int) mouseY - topPos - TerminalMenu.GRID_Y;
        if (gx < 0 || gy < 0 || gx >= TerminalMenu.GRID_COLS * 18 || gy >= GRID_H) return -1;
        int idx = (scrollRow + gy / 18) * TerminalMenu.GRID_COLS + gx / 18;
        return idx < view.size() ? idx : -2;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        refreshView();
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderGrid(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);

        int idx = gridIndexAt(mouseX, mouseY);
        if (idx >= 0 && menu.getCarried().isEmpty()) {
            TerminalMenu.GridEntry e = view.get(idx);
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, e.stack()));
            lines.add(Component.translatable("gui.tierednetworks.stored", String.format("%,d", e.count())).withStyle(ChatFormatting.AQUA));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        int hovered = gridIndexAt(mouseX, mouseY);
        for (int row = 0; row < TerminalMenu.GRID_ROWS; row++) {
            for (int col = 0; col < TerminalMenu.GRID_COLS; col++) {
                int idx = (scrollRow + row) * TerminalMenu.GRID_COLS + col;
                int x = leftPos + TerminalMenu.GRID_X + col * 18;
                int y = topPos + TerminalMenu.GRID_Y + row * 18;
                if (idx < view.size()) {
                    TerminalMenu.GridEntry e = view.get(idx);
                    g.renderItem(e.stack(), x, y);
                    drawCount(g, GuiDraw.shortCount(e.count()), x, y);
                }
                if (hovered != -1 && mouseInCell(mouseX, mouseY, x, y)) {
                    g.fill(x, y, x + 16, y + 16, 400, 0x80FFFFFF);
                }
            }
        }
    }

    private static boolean mouseInCell(int mx, int my, int x, int y) {
        return mx >= x - 1 && mx < x + 17 && my >= y - 1 && my < y + 17;
    }

    private void drawCount(GuiGraphics g, String text, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        int tx = (x + 16) * 2 - font.width(text) - 1;
        int ty = (y + 16) * 2 - 8;
        g.drawString(font, text, tx, ty, 0xFFFFFF, true);
        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int row = 0; row < TerminalMenu.GRID_ROWS; row++) {
            for (int col = 0; col < TerminalMenu.GRID_COLS; col++) {
                GuiDraw.slot(g, leftPos + TerminalMenu.GRID_X + col * 18, topPos + TerminalMenu.GRID_Y + row * 18);
            }
        }
        GuiDraw.playerInventory(g, leftPos, topPos, TerminalMenu.GRID_X, TerminalMenu.INV_Y);

        // Scrollbar track and thumb.
        int sx = leftPos + SCROLL_X;
        int sy = topPos + TerminalMenu.GRID_Y - 1;
        g.fill(sx, sy, sx + SCROLL_W, sy + GRID_H, 0xFF373737);
        int thumbH = 15;
        int max = maxScroll();
        int thumbY = max == 0 ? 0 : (GRID_H - thumbH) * scrollRow / max;
        int thumbColor = max == 0 ? 0xFF8B8B8B : 0xFFC6C6C6;
        g.fill(sx + 1, sy + thumbY + 1, sx + SCROLL_W - 1, sy + thumbY + thumbH - 1, thumbColor);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, GuiDraw.LABEL, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiDraw.LABEL, false);

        NetworkStatus status = menu.getClientStatus();
        Component line;
        if (status == NetworkStatus.ONLINE) {
            line = Component.translatable("gui.tierednetworks.usage",
                    GuiDraw.shortCount(menu.getClientUsed()), GuiDraw.shortCount(menu.getClientCapacity()), view.size());
        } else {
            line = status.display();
        }
        g.drawString(font, line, 8, 19, GuiDraw.LABEL, false);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int idx = gridIndexAt(mouseX, mouseY);
        if (idx != -1 && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            if (!menu.getCarried().isEmpty()) {
                ModPackets.CHANNEL.sendToServer(new GridInsertPacket(menu.containerId, button == GLFW.GLFW_MOUSE_BUTTON_RIGHT));
            } else if (idx >= 0) {
                int mode = hasShiftDown() ? TerminalMenu.MODE_TO_INVENTORY
                        : button == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? TerminalMenu.MODE_HALF : TerminalMenu.MODE_STACK;
                ModPackets.CHANNEL.sendToServer(new GridExtractPacket(menu.containerId, view.get(idx).stack(), mode));
            }
            return true;
        }
        if (search != null && !search.isMouseOver(mouseX, mouseY)) {
            search.setFocused(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = maxScroll();
        if (max > 0) {
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(delta), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (search != null && search.isFocused()) {
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
