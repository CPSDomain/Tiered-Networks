package com.bobroflza.tierednetworks.registry;

import com.bobroflza.tierednetworks.TieredNetworks;
import com.bobroflza.tierednetworks.blockentity.AccessPointBlockEntity;
import com.bobroflza.tierednetworks.blockentity.ControllerBlockEntity;
import com.bobroflza.tierednetworks.blockentity.DriveBayBlockEntity;
import com.bobroflza.tierednetworks.blockentity.ImportInterfaceBlockEntity;
import com.bobroflza.tierednetworks.blockentity.TerminalBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TieredNetworks.MODID);

    public static final RegistryObject<BlockEntityType<ControllerBlockEntity>> CONTROLLER = BLOCK_ENTITIES.register("network_controller",
            () -> BlockEntityType.Builder.of(ControllerBlockEntity::new, ModBlocks.CONTROLLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<AccessPointBlockEntity>> ACCESS_POINT = BLOCK_ENTITIES.register("access_point",
            () -> BlockEntityType.Builder.of(AccessPointBlockEntity::new,
                    ModBlocks.ACCESS_POINTS.values().stream().map(r -> r.get()).toArray(Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<DriveBayBlockEntity>> DRIVE_BAY = BLOCK_ENTITIES.register("drive_bay",
            () -> BlockEntityType.Builder.of(DriveBayBlockEntity::new, ModBlocks.DRIVE_BAY.get()).build(null));

    public static final RegistryObject<BlockEntityType<TerminalBlockEntity>> TERMINAL = BLOCK_ENTITIES.register("terminal",
            () -> BlockEntityType.Builder.of(TerminalBlockEntity::new, ModBlocks.TERMINAL.get()).build(null));

    public static final RegistryObject<BlockEntityType<ImportInterfaceBlockEntity>> IMPORT_INTERFACE = BLOCK_ENTITIES.register("import_interface",
            () -> BlockEntityType.Builder.of(ImportInterfaceBlockEntity::new, ModBlocks.IMPORT_INTERFACE.get()).build(null));

    private ModBlockEntities() {}
}
