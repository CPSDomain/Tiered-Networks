package com.bobroflza.tierednetworks.registry;

import com.bobroflza.tierednetworks.TieredNetworks;
import com.bobroflza.tierednetworks.block.AccessPointBlock;
import com.bobroflza.tierednetworks.block.ControllerBlock;
import com.bobroflza.tierednetworks.block.DriveBayBlock;
import com.bobroflza.tierednetworks.block.FlatInterfaceBlock;
import com.bobroflza.tierednetworks.block.InterfaceBlock;
import com.bobroflza.tierednetworks.block.LevelEmitterBlock;
import com.bobroflza.tierednetworks.block.NetworkBlock;
import com.bobroflza.tierednetworks.block.TerminalBlock;
import com.bobroflza.tierednetworks.grid.AccessPointTier;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TieredNetworks.MODID);

    public static final RegistryObject<Block> CONTROLLER = BLOCKS.register("network_controller",
            () -> new ControllerBlock(NetworkBlock.defaultProperties()));

    public static final RegistryObject<Block> DRIVE_BAY = BLOCKS.register("drive_bay",
            () -> new DriveBayBlock(NetworkBlock.defaultProperties()));

    public static final RegistryObject<Block> TERMINAL = BLOCKS.register("terminal",
            () -> new TerminalBlock(NetworkBlock.defaultProperties().lightLevel(s -> 6)));

    public static final RegistryObject<Block> IMPORT_INTERFACE = BLOCKS.register("import_interface",
            () -> new InterfaceBlock(NetworkBlock.defaultProperties(), false));
    public static final RegistryObject<Block> FLAT_IMPORT_INTERFACE = BLOCKS.register("flat_import_interface",
            () -> new FlatInterfaceBlock(NetworkBlock.defaultProperties().noOcclusion(), false));
    public static final RegistryObject<Block> EXPORT_INTERFACE = BLOCKS.register("export_interface",
            () -> new InterfaceBlock(NetworkBlock.defaultProperties(), true));
    public static final RegistryObject<Block> FLAT_EXPORT_INTERFACE = BLOCKS.register("flat_export_interface",
            () -> new FlatInterfaceBlock(NetworkBlock.defaultProperties().noOcclusion(), true));

    public static final RegistryObject<Block> LEVEL_EMITTER = BLOCKS.register("level_emitter",
            () -> new LevelEmitterBlock(NetworkBlock.defaultProperties()
                    .lightLevel(s -> s.getValue(LevelEmitterBlock.POWERED) ? 7 : 0)));

    public static final Map<AccessPointTier, RegistryObject<Block>> ACCESS_POINTS = new EnumMap<>(AccessPointTier.class);

    static {
        for (AccessPointTier tier : AccessPointTier.values()) {
            ACCESS_POINTS.put(tier, BLOCKS.register(tier.getName() + "_access_point",
                    () -> new AccessPointBlock(NetworkBlock.defaultProperties().lightLevel(s -> 4), tier)));
        }
    }

    private ModBlocks() {}
}
