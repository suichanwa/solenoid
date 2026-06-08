package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.Solenoid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration hub for the EMF energy blocks, their block items, and their block entity types, plus
 * the {@code Capabilities.Energy.BLOCK} capability wiring. EMF is the player-facing label; the
 * capability is native NeoForge Forge Energy, so these are RF/FE-compatible with other mods.
 */
public final class EmfBlocks {
    private EmfBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Solenoid.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Solenoid.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Solenoid.MODID);

    // ---- Blocks ----

    public static final DeferredBlock<EmfSourceBlock> EMF_SOURCE = BLOCKS.registerBlock(
            "emf_source",
            EmfSourceBlock::new,
            p -> p.strength(2.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<CopperCableBlock> COPPER_CABLE = BLOCKS.registerBlock(
            "copper_cable",
            CopperCableBlock::new,
            p -> p.strength(1.0f).sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<EmfSinkBlock> EMF_SINK = BLOCKS.registerBlock(
            "emf_sink",
            EmfSinkBlock::new,
            p -> p.strength(2.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 7 : 0));

    public static final DeferredBlock<HandCrankGeneratorBlock> HAND_CRANK_GENERATOR = BLOCKS.registerBlock(
            "hand_crank_generator",
            HandCrankGeneratorBlock::new,
            p -> p.strength(2.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<CrusherBlock> CRUSHER = BLOCKS.registerBlock(
            "crusher",
            CrusherBlock::new,
            p -> p.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<SeparatorBlock> SEPARATOR = BLOCKS.registerBlock(
            "separator",
            SeparatorBlock::new,
            p -> p.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<InductionFurnaceBlock> INDUCTION_FURNACE = BLOCKS.registerBlock(
            "induction_furnace",
            InductionFurnaceBlock::new,
            p -> p.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<CapacitorBlock> CAPACITOR = BLOCKS.registerBlock(
            "capacitor",
            CapacitorBlock::new,
            p -> p.strength(3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<ChemicalReactorBlock> CHEMICAL_REACTOR = BLOCKS.registerBlock(
            "chemical_reactor",
            ChemicalReactorBlock::new,
            p -> p.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<DigesterBlock> DIGESTER = BLOCKS.registerBlock(
            "digester",
            DigesterBlock::new,
            p -> p.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<CentrifugeBlock> CENTRIFUGE = BLOCKS.registerBlock(
            "centrifuge",
            CentrifugeBlock::new,
            p -> p.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    // ---- Block items (with EMF role tooltips) ----

    public static final DeferredItem<BlockItem> EMF_SOURCE_ITEM = ITEMS.registerItem(
            "emf_source",
            props -> new EmfBlockItem(EMF_SOURCE.get(), props, "tooltip.solenoid.emf_source"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> COPPER_CABLE_ITEM = ITEMS.registerItem(
            "copper_cable",
            props -> new EmfBlockItem(COPPER_CABLE.get(), props, "tooltip.solenoid.copper_cable"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> EMF_SINK_ITEM = ITEMS.registerItem(
            "emf_sink",
            props -> new EmfBlockItem(EMF_SINK.get(), props,
                    "tooltip.solenoid.emf_sink", "tooltip.solenoid.emf_sink.capacity"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> HAND_CRANK_GENERATOR_ITEM = ITEMS.registerItem(
            "hand_crank_generator",
            props -> new EmfBlockItem(HAND_CRANK_GENERATOR.get(), props,
                    "tooltip.solenoid.hand_crank_generator", "tooltip.solenoid.hand_crank_generator.capacity"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> CRUSHER_ITEM = ITEMS.registerItem(
            "crusher",
            props -> new EmfBlockItem(CRUSHER.get(), props, "tooltip.solenoid.crusher"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> SEPARATOR_ITEM = ITEMS.registerItem(
            "separator",
            props -> new EmfBlockItem(SEPARATOR.get(), props, "tooltip.solenoid.separator"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> INDUCTION_FURNACE_ITEM = ITEMS.registerItem(
            "induction_furnace",
            props -> new EmfBlockItem(INDUCTION_FURNACE.get(), props, "tooltip.solenoid.induction_furnace"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> CAPACITOR_ITEM = ITEMS.registerItem(
            "capacitor",
            props -> new EmfBlockItem(CAPACITOR.get(), props,
                    "tooltip.solenoid.capacitor", "tooltip.solenoid.capacitor.capacity"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> CHEMICAL_REACTOR_ITEM = ITEMS.registerItem(
            "chemical_reactor",
            props -> new EmfBlockItem(CHEMICAL_REACTOR.get(), props, "tooltip.solenoid.chemical_reactor"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> DIGESTER_ITEM = ITEMS.registerItem(
            "digester",
            props -> new EmfBlockItem(DIGESTER.get(), props, "tooltip.solenoid.digester"),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredItem<BlockItem> CENTRIFUGE_ITEM = ITEMS.registerItem(
            "centrifuge",
            props -> new EmfBlockItem(CENTRIFUGE.get(), props, "tooltip.solenoid.centrifuge"),
            p -> p.useBlockDescriptionPrefix());

    // ---- Block entity types ----

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmfSourceBlockEntity>> EMF_SOURCE_BE =
            BLOCK_ENTITIES.register("emf_source",
                    () -> new BlockEntityType<>(EmfSourceBlockEntity::new, EMF_SOURCE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperCableBlockEntity>> COPPER_CABLE_BE =
            BLOCK_ENTITIES.register("copper_cable",
                    () -> new BlockEntityType<>(CopperCableBlockEntity::new, COPPER_CABLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmfSinkBlockEntity>> EMF_SINK_BE =
            BLOCK_ENTITIES.register("emf_sink",
                    () -> new BlockEntityType<>(EmfSinkBlockEntity::new, EMF_SINK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HandCrankGeneratorBlockEntity>> HAND_CRANK_GENERATOR_BE =
            BLOCK_ENTITIES.register("hand_crank_generator",
                    () -> new BlockEntityType<>(HandCrankGeneratorBlockEntity::new, HAND_CRANK_GENERATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER_BE =
            BLOCK_ENTITIES.register("crusher",
                    () -> new BlockEntityType<>(CrusherBlockEntity::new, CRUSHER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeparatorBlockEntity>> SEPARATOR_BE =
            BLOCK_ENTITIES.register("separator",
                    () -> new BlockEntityType<>(SeparatorBlockEntity::new, SEPARATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InductionFurnaceBlockEntity>> INDUCTION_FURNACE_BE =
            BLOCK_ENTITIES.register("induction_furnace",
                    () -> new BlockEntityType<>(InductionFurnaceBlockEntity::new, INDUCTION_FURNACE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CapacitorBlockEntity>> CAPACITOR_BE =
            BLOCK_ENTITIES.register("capacitor",
                    () -> new BlockEntityType<>(CapacitorBlockEntity::new, CAPACITOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalReactorBlockEntity>> CHEMICAL_REACTOR_BE =
            BLOCK_ENTITIES.register("chemical_reactor",
                    () -> new BlockEntityType<>(ChemicalReactorBlockEntity::new, CHEMICAL_REACTOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DigesterBlockEntity>> DIGESTER_BE =
            BLOCK_ENTITIES.register("digester",
                    () -> new BlockEntityType<>(DigesterBlockEntity::new, DIGESTER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_BE =
            BLOCK_ENTITIES.register("centrifuge",
                    () -> new BlockEntityType<>(CentrifugeBlockEntity::new, CENTRIFUGE.get()));

    /** Wire the deferred registers to the mod event bus. Called from the main mod constructor. */
    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }

    /**
     * Registers the Forge Energy (EMF) capability for each energy block entity. Side-aware: the
     * provider receives the queried face and returns the same handler for every side.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, EMF_SOURCE_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, COPPER_CABLE_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, EMF_SINK_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, HAND_CRANK_GENERATOR_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CRUSHER_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, SEPARATOR_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, INDUCTION_FURNACE_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CAPACITOR_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CHEMICAL_REACTOR_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, DIGESTER_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CENTRIFUGE_BE.get(),
                (be, side) -> be.getEnergyHandler(side));

        event.registerBlockEntity(Capabilities.Item.BLOCK, CRUSHER_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, SEPARATOR_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, INDUCTION_FURNACE_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, CHEMICAL_REACTOR_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, DIGESTER_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, CENTRIFUGE_BE.get(),
                (be, side) -> be.getItemHandler(side));
    }
}
