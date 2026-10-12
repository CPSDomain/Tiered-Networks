package com.bobroflza.tierednetworks.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.function.Consumer;

final class MenuHelper {
    private MenuHelper() {}

    /** Adds the 27 main inventory slots, then the 9 hotbar slots 58px below the top row. */
    static void addPlayerInventory(Consumer<Slot> add, Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                add.accept(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            add.accept(new Slot(inv, col, x + col * 18, y + 58));
        }
    }
}
