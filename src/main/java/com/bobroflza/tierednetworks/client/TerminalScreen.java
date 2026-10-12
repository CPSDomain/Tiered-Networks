package com.bobroflza.tierednetworks.client;

import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.menu.TerminalMenu;
import com.bobroflza.tierednetworks.packet.FluidClickPacket;
import com.bobroflza.tierednetworks.packet.GridExtractPacket;
import com.bobroflza.tierednetworks.packet.GridInsertPacket;
import com.bobroflza.tierednetworks.packet.ModPackets;
import com.bobroflza.tierednetworks.storage.FluidKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
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

    /** Remembered between openings, like a creative tab. */
    private static boolean fluidMode;

    private EditBox search;
    private Button tabButton;
    private int scrollRow;
    private List<TerminalMenu.GridEntry> view = new ArrayList<>();
    private List<TerminalMenu.FluidEntry> fluidView = new ArrayList<>();
    private Object lastSource;
    private String lastQuery = "";
    private boolean lastMode;

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
        tabButton = addRenderableWidget(Button.builder(tabLabel(), b -> {
                    fluidMode = !fluidMode;
                    scrollRow = 0;
                    b.setMessage(tabLabel());
                })
                .bounds(leftPos + 140, topPos + 16, 46, 12)
                .tooltip(Tooltip.create(Component.translatable("gui.tierednetworks.tab_help"))).build());
    }

    private Component tabLabel() {
        return Component.translatable(fluidMode ? "gui.tierednetworks.tab_fluids" : "gui.tierednetworks.tab_items");
    }

    // ---------------------------------------------------------------- view

    private void refreshView() {
        Object source = fluidMode ? menu.getClientFluids() : menu.getClientEntries();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (source == lastSource && query.equals(lastQuery) && lastMode == fluidMode) return;
        lastSource = source;
        lastQuery = query;
        lastMode = fluidMode;

        if (fluidMode) {
            List<TerminalMenu.FluidEntry> out = new ArrayList<>();
            for (TerminalMenu.FluidEntry e : menu.getClientFluids()) {
                if (matchesFluid(e.fluid(), query)) out.add(e);
            }
            Comparator<TerminalMenu.FluidEntry> byAmount = Comparator.comparingLong(TerminalMenu.FluidEntry::amount);
            Comparator<TerminalMenu.FluidEntry> byName = Comparator.comparing(e -> e.fluid().getDisplayName().getString());
            out.sort(byAmount.reversed().thenComparing(byName));
            fluidView = out;
        } else {
            List<TerminalMenu.GridEntry> out = new ArrayList<>();
            for (TerminalMenu.GridEntry e : menu.getClientEntries()) {
                if (matches(e.stack(), query)) out.add(e);
            }
            Comparator<TerminalMenu.GridEntry> byCount = Comparator.comparingLong(TerminalMenu.GridEntry::count);
            Comparator<TerminalMenu.GridEntry> byName = Comparator.comparing(e -> e.stack().getHoverName().getString());
            out.sort(byCount.reversed().thenComparing(byName));
            view = out;
        }
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

    private static boolean matchesFluid(FluidStack fluid, String query) {
        if (query.isEmpty()) return true;
        if (query.startsWith("@")) {
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
            return id != null && id.getNamespace().contains(query.substring(1));
        }
        return fluid.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private int viewSize() {
        return fluidMode ? fluidView.size() : view.size();
    }

    private int totalRows() {
        return (viewSize() + TerminalMenu.GRID_COLS - 1) / TerminalMenu.GRID_COLS;
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - TerminalMenu.GRID_ROWS);
    }

    /** Index into the current view under the mouse, -1 if outside the grid, -2 if on an empty grid cell. */
    private int gridIndexAt(double mouseX, double mouseY) {
        int gx = (int) mouseX - leftPos - TerminalMenu.GRID_X;
        int gy = (int) mouseY - topPos - TerminalMenu.GRID_Y;
        if (gx < 0 || gy < 0 || gx >= TerminalMenu.GRID_COLS * 18 || gy >= GRID_H) return -1;
        int idx = (scrollRow + gy / 18) * TerminalMenu.GRID_COLS + gx / 18;
        return idx < viewSize() ? idx : -2;
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
        if (idx < 0) return;
        if (fluidMode) {
            TerminalMenu.FluidEntry e = fluidView.get(idx);
            List<Component> lines = new ArrayList<>();
            lines.add(e.fluid().getDisplayName());
            lines.add(Component.translatable("gui.tierednetworks.stored_mb", String.format("%,d", e.amount())).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable(FluidKey.isGas(e.fluid().getFluid()) ? "gui.tierednetworks.kind_gas" : "gui.tierednetworks.kind_fluid")
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.tierednetworks.fluid_hint").withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (menu.getCarried().isEmpty()) {
            TerminalMenu.GridEntry e = view.get(idx);
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, e.stack()));
            lines.add(Component.translatable("gui.tierednetworks.stored", String.format("%,d", e.count())).withStyle(ChatFormatting.AQUA));
            if (e.craftable()) lines.add(Component.translatable("gui.tierednetworks.craftable_hint").withStyle(ChatFormatting.GOLD));
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
                if (idx < viewSize()) {
                    if (fluidMode) {
                        TerminalMenu.FluidEntry e = fluidView.get(idx);
                        renderFluid(g, e.fluid(), x, y);
                        drawCount(g, fluidAmount(e.amount()), x, y);
                    } else {
                        TerminalMenu.GridEntry e = view.get(idx);
                        g.renderItem(e.stack(), x, y);
                        if (e.count() == 0 && e.craftable()) {
                            drawCount(g, Component.translatable("gui.tierednetworks.craft_label").getString(), x, y);
                        } else {
                            drawCount(g, GuiDraw.shortCount(e.count()), x, y);
                        }
                    }
                }
                if (hovered != -1 && mouseInCell(mouseX, mouseY, x, y)) {
                    g.fill(x, y, x + 16, y + 16, 400, 0x80FFFFFF);
                }
            }
        }
    }

    private static String fluidAmount(long mb) {
        if (mb < 1000) return mb + "mB";
        return GuiDraw.shortCount(mb / 1000) + "B";
    }

    private void renderFluid(GuiGraphics g, FluidStack fluid, int x, int y) {
        if (minecraft == null || fluid.isEmpty()) return;
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = ext.getStillTexture(fluid);
        if (still == null) return;
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        int tint = ext.getTintColor(fluid);
        float r = ((tint >> 16) & 0xFF) / 255F;
        float gr = ((tint >> 8) & 0xFF) / 255F;
        float b = (tint & 0xFF) / 255F;
        g.blit(x, y, 0, 16, 16, sprite, r, gr, b, 1F);
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
        if (status != NetworkStatus.ONLINE) {
            line = status.display();
        } else if (fluidMode) {
            line = Component.translatable("gui.tierednetworks.fluid_usage",
                    GuiDraw.shortCount(menu.getClientFluidUsed() / 1000), GuiDraw.shortCount(menu.getClientFluidCapacity() / 1000));
        } else {
            line = Component.translatable("gui.tierednetworks.usage",
                    GuiDraw.shortCount(menu.getClientUsed()), GuiDraw.shortCount(menu.getClientCapacity()), view.size());
        }
        g.drawString(font, line, 8, 19, GuiDraw.LABEL, false);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int idx = gridIndexAt(mouseX, mouseY);

        if (fluidMode && idx != -1 && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            // Fluids move with the container on the cursor: a full one empties into the network,
            // an empty one fills from the fluid you clicked.
            if (!menu.getCarried().isEmpty()) {
                FluidStack target = idx >= 0 ? fluidView.get(idx).fluid() : FluidStack.EMPTY;
                ModPackets.CHANNEL.sendToServer(new FluidClickPacket(menu.containerId, target));
            }
            return true;
        }

        if (!fluidMode && idx >= 0 && menu.getCarried().isEmpty() && view.get(idx).craftable()) {
            TerminalMenu.GridEntry e = view.get(idx);
            boolean left = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || (left && hasControlDown()) || (left && e.count() == 0)) {
                if (minecraft != null) minecraft.setScreen(new CraftAmountScreen(this, menu.containerId, e.stack()));
                return true;
            }
        }
        if (!fluidMode && idx != -1 && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
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
