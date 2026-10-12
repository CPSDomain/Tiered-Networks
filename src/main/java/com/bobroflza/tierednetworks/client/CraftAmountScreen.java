package com.bobroflza.tierednetworks.client;

import com.bobroflza.tierednetworks.packet.CraftRequestPacket;
import com.bobroflza.tierednetworks.packet.ModPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Asks how many to craft, then goes back to the terminal it came from. */
public class CraftAmountScreen extends Screen {
    private static final int W = 176, H = 92;

    private final Screen parent;
    private final int containerId;
    private final ItemStack item;
    private EditBox amount;
    private int left, top;

    public CraftAmountScreen(Screen parent, int containerId, ItemStack item) {
        super(Component.translatable("gui.tierednetworks.craft_title"));
        this.parent = parent;
        this.containerId = containerId;
        this.item = item.copyWithCount(1);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        amount = new EditBox(font, left + 30, top + 30, 116, 16, Component.translatable("gui.tierednetworks.threshold"));
        amount.setMaxLength(7);
        amount.setFilter(s -> s.matches("\\d{0,7}"));
        amount.setValue("1");
        addRenderableWidget(amount);
        setInitialFocus(amount);

        int[] steps = {1, 8, 64};
        for (int i = 0; i < steps.length; i++) {
            int step = steps[i];
            addRenderableWidget(Button.builder(Component.literal("+" + step), b -> bump(step)).bounds(left + 8 + i * 28, top + 52, 26, 14).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.tierednetworks.craft_start"), b -> start())
                .bounds(left + 96, top + 50, 72, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> back())
                .bounds(left + 96, top + 70, 72, 16).build());
    }

    private void bump(int step) {
        long v = parse();
        amount.setValue(Long.toString(Math.min(9_999_999L, (v == 1 && step > 1 ? 0 : v) + step)));
    }

    private long parse() {
        String s = amount.getValue();
        return s.isEmpty() ? 0 : Long.parseLong(s);
    }

    private void start() {
        long v = parse();
        if (v > 0) ModPackets.CHANNEL.sendToServer(new CraftRequestPacket(containerId, item, v));
        back();
    }

    private void back() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        back();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            start();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        GuiDraw.panel(g, left, top, W, H);
        g.drawString(font, title, left + 8, top + 7, GuiDraw.LABEL, false);
        GuiDraw.slot(g, left + 8, top + 30);
        g.renderItem(item, left + 8, top + 30);
        g.drawString(font, item.getHoverName(), left + 30, top + 18, GuiDraw.LABEL, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
