package com.bobroflza.tierednetworks.client;

import com.bobroflza.tierednetworks.TieredNetworks;
import com.bobroflza.tierednetworks.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TieredNetworks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.DRIVE_BAY.get(), DriveBayScreen::new);
            MenuScreens.register(ModMenus.TERMINAL.get(), TerminalScreen::new);
            MenuScreens.register(ModMenus.INTERFACE.get(), InterfaceScreen::new);
            MenuScreens.register(ModMenus.LEVEL_EMITTER.get(), LevelEmitterScreen::new);
            MenuScreens.register(ModMenus.PATTERN_ENCODER.get(), PatternEncoderScreen::new);
            MenuScreens.register(ModMenus.PATTERN_PROVIDER.get(), PatternProviderScreen::new);
        });
    }
}
