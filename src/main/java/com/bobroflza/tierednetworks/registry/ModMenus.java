package com.bobroflza.tierednetworks.registry;

import com.bobroflza.tierednetworks.TieredNetworks;
import com.bobroflza.tierednetworks.menu.DriveBayMenu;
import com.bobroflza.tierednetworks.menu.InterfaceMenu;
import com.bobroflza.tierednetworks.menu.LevelEmitterMenu;
import com.bobroflza.tierednetworks.menu.PatternEncoderMenu;
import com.bobroflza.tierednetworks.menu.PatternProviderMenu;
import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TieredNetworks.MODID);

    public static final RegistryObject<MenuType<DriveBayMenu>> DRIVE_BAY = MENUS.register("drive_bay",
            () -> IForgeMenuType.create(DriveBayMenu::new));

    public static final RegistryObject<MenuType<TerminalMenu>> TERMINAL = MENUS.register("terminal",
            () -> IForgeMenuType.create(TerminalMenu::new));

    public static final RegistryObject<MenuType<InterfaceMenu>> INTERFACE = MENUS.register("interface",
            () -> IForgeMenuType.create(InterfaceMenu::new));

    public static final RegistryObject<MenuType<LevelEmitterMenu>> LEVEL_EMITTER = MENUS.register("level_emitter",
            () -> IForgeMenuType.create(LevelEmitterMenu::new));

    public static final RegistryObject<MenuType<PatternEncoderMenu>> PATTERN_ENCODER = MENUS.register("pattern_encoder",
            () -> IForgeMenuType.create(PatternEncoderMenu::new));

    public static final RegistryObject<MenuType<PatternProviderMenu>> PATTERN_PROVIDER = MENUS.register("pattern_provider",
            () -> IForgeMenuType.create(PatternProviderMenu::new));

    private ModMenus() {}
}
