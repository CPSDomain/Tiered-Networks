package com.bobroflza.tierednetworks.registry;

import com.bobroflza.tierednetworks.TieredNetworks;
import com.bobroflza.tierednetworks.grid.AccessPointTier;
import com.bobroflza.tierednetworks.item.NetworkLinkerItem;
import com.bobroflza.tierednetworks.item.StorageCellItem;
import com.bobroflza.tierednetworks.storage.CellTier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TieredNetworks.MODID);

    // Block items, in creative-tab order.
    public static final RegistryObject<Item> CONTROLLER = block(ModBlocks.CONTROLLER);
    public static final Map<AccessPointTier, RegistryObject<Item>> ACCESS_POINTS = new EnumMap<>(AccessPointTier.class);

    static {
        for (AccessPointTier tier : AccessPointTier.values()) {
            ACCESS_POINTS.put(tier, block(ModBlocks.ACCESS_POINTS.get(tier)));
        }
    }

    public static final RegistryObject<Item> DRIVE_BAY = block(ModBlocks.DRIVE_BAY);
    public static final RegistryObject<Item> TERMINAL = block(ModBlocks.TERMINAL);

    // Tools.
    public static final RegistryObject<Item> NETWORK_LINKER = ITEMS.register("network_linker",
            () -> new NetworkLinkerItem(new Item.Properties()));

    // Crafting components.
    public static final RegistryObject<Item> NETWORK_PROCESSOR = ITEMS.register("network_processor",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRELESS_CORE = ITEMS.register("wireless_core",
            () -> new Item(new Item.Properties()));

    public static final Map<CellTier, RegistryObject<Item>> STORAGE_COMPONENTS = new EnumMap<>(CellTier.class);
    public static final Map<CellTier, RegistryObject<Item>> STORAGE_CELLS = new EnumMap<>(CellTier.class);

    static {
        for (CellTier tier : CellTier.values()) {
            STORAGE_COMPONENTS.put(tier, ITEMS.register("storage_component_" + tier.getName(),
                    () -> new Item(new Item.Properties())));
        }
        for (CellTier tier : CellTier.values()) {
            Rarity rarity = tier.ordinal() >= 3 ? Rarity.RARE : tier.ordinal() >= 1 ? Rarity.UNCOMMON : Rarity.COMMON;
            STORAGE_CELLS.put(tier, ITEMS.register("storage_cell_" + tier.getName(),
                    () -> new StorageCellItem(new Item.Properties().rarity(rarity), tier)));
        }
    }

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {}
}
