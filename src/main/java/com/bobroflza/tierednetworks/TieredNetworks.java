package com.bobroflza.tierednetworks;

import com.bobroflza.tierednetworks.crafting.CraftingManager;
import com.bobroflza.tierednetworks.grid.NetworkTracker;
import com.bobroflza.tierednetworks.packet.ModPackets;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.registry.ModBlocks;
import com.bobroflza.tierednetworks.registry.ModCreativeTabs;
import com.bobroflza.tierednetworks.registry.ModItems;
import com.bobroflza.tierednetworks.registry.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(TieredNetworks.MODID)
public class TieredNetworks {
    public static final String MODID = "tierednetworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TieredNetworks() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        MinecraftForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> {
            NetworkTracker.clear();
            CraftingManager.clear();
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            NetworkTracker.clear();
            CraftingManager.clear();
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModPackets::register);
    }
}
