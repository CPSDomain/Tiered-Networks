package com.bobroflza.tierednetworks.registry;

import com.bobroflza.tierednetworks.TieredNetworks;
import com.bobroflza.tierednetworks.blockentity.AccessPointBlockEntity;
import com.bobroflza.tierednetworks.blockentity.AssemblerBlockEntity;
import com.bobroflza.tierednetworks.blockentity.CraftingBlockEntity;
import com.bobroflza.tierednetworks.blockentity.PatternEncoderBlockEntity;
import com.bobroflza.tierednetworks.blockentity.PatternProviderBlockEntity;
import com.bobroflza.tierednetworks.blockentity.ControllerBlockEntity;
import com.bobroflza.tierednetworks.blockentity.DriveBayBlockEntity;
import com.bobroflza.tierednetworks.blockentity.ExportInterfaceBlockEntity;
import com.bobroflza.tierednetworks.blockentity.ImportInterfaceBlockEntity;
import com.bobroflza.tierednetworks.blockentity.LevelEmitterBlockEntity;
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
            () -> BlockEntityType.Builder.of(ImportInterfaceBlockEntity::new,
                    ModBlocks.IMPORT_INTERFACE.get(), ModBlocks.FLAT_IMPORT_INTERFACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ExportInterfaceBlockEntity>> EXPORT_INTERFACE = BLOCK_ENTITIES.register("export_interface",
            () -> BlockEntityType.Builder.of(ExportInterfaceBlockEntity::new,
                    ModBlocks.EXPORT_INTERFACE.get(), ModBlocks.FLAT_EXPORT_INTERFACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<LevelEmitterBlockEntity>> LEVEL_EMITTER = BLOCK_ENTITIES.register("level_emitter",
            () -> BlockEntityType.Builder.of(LevelEmitterBlockEntity::new, ModBlocks.LEVEL_EMITTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<PatternEncoderBlockEntity>> PATTERN_ENCODER = BLOCK_ENTITIES.register("pattern_encoder",
            () -> BlockEntityType.Builder.of(PatternEncoderBlockEntity::new, ModBlocks.PATTERN_ENCODER.get()).build(null));

    public static final RegistryObject<BlockEntityType<PatternProviderBlockEntity>> PATTERN_PROVIDER = BLOCK_ENTITIES.register("pattern_provider",
            () -> BlockEntityType.Builder.of(PatternProviderBlockEntity::new, ModBlocks.PATTERN_PROVIDER.get()).build(null));

    public static final RegistryObject<BlockEntityType<AssemblerBlockEntity>> ASSEMBLER = BLOCK_ENTITIES.register("molecular_assembler",
            () -> BlockEntityType.Builder.of(AssemblerBlockEntity::new, ModBlocks.ASSEMBLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<CraftingBlockEntity>> CRAFTING_CPU = BLOCK_ENTITIES.register("crafting_cpu",
            () -> BlockEntityType.Builder.of(CraftingBlockEntity::new,
                    ModBlocks.CPU_PARTS.values().stream().map(r -> r.get()).toArray(Block[]::new)).build(null));

    private ModBlockEntities() {}
}
