package com.suiseika.solenoid;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import com.suiseika.solenoid.energy.EmfBlocks;

public class SolenoidDataGenerator {

    public static void gatherData(GatherDataEvent.Client event) {
        var output = event.getGenerator().getPackOutput();
        var lookupProvider = event.getLookupProvider();

        // Language
        event.addProvider(new ModLanguageProvider(output));

        // Loot tables
        event.addProvider(new ModLootTableProvider(output, lookupProvider));

        // Block tags
        var blockTags = new ModBlockTagsProvider(output, lookupProvider);
        event.addProvider(blockTags);

        // Item tags
        event.addProvider(new ModItemTagsProvider(output, lookupProvider, blockTags.contentsGetter()));

        // Item Model Definitions (1.21.2+ style)
        event.addProvider(new ModItemModelDefinitionProvider(output));

        // Worldgen
        event.addProvider(new ModWorldgenProvider(output, lookupProvider));
    }

    // ---- Item Model Definitions ----

    private static class ModItemModelDefinitionProvider implements net.minecraft.data.DataProvider {
        private final net.minecraft.data.PackOutput output;

        public ModItemModelDefinitionProvider(net.minecraft.data.PackOutput output) {
            this.output = output;
        }

        @Override
        public CompletableFuture<?> run(net.minecraft.data.CachedOutput cache) {
            List<CompletableFuture<?>> futures = new java.util.ArrayList<>();
            net.minecraft.data.PackOutput.PathProvider pathProvider = output.createPathProvider(net.minecraft.data.PackOutput.Target.RESOURCE_PACK, "items");

            for (ProcessedOre ore : ProcessedOre.values()) {
                for (ProcessedForm form : ProcessedForm.values()) {
                    String id = form.getPrefix() + ore.getName() + form.getSuffix();
                    
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    com.google.gson.JsonObject model = new com.google.gson.JsonObject();
                    model.addProperty("type", "minecraft:model");
                    model.addProperty("model", "solenoid:item/" + form.getTemplateModel());
                    
                    com.google.gson.JsonArray tints = new com.google.gson.JsonArray();
                    com.google.gson.JsonObject tint = new com.google.gson.JsonObject();
                    tint.addProperty("type", "minecraft:constant");
                    tint.addProperty("value", ore.getColor());
                    tints.add(tint);
                    model.add("tints", tints);
                    
                    json.add("model", model);
                    
                    futures.add(net.minecraft.data.DataProvider.saveStable(cache, json, pathProvider.json(Identifier.fromNamespaceAndPath(Solenoid.MODID, id))));
                }
            }

            // Simple 2D Items
            List.of("machine_frame", "screw", "magnet_charm", "repulsor", "thorium_pellet").forEach(id -> {
                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                com.google.gson.JsonObject model = new com.google.gson.JsonObject();
                model.addProperty("type", "minecraft:model");
                model.addProperty("model", "solenoid:item/" + id);
                json.add("model", model);
                futures.add(net.minecraft.data.DataProvider.saveStable(cache, json, pathProvider.json(Identifier.fromNamespaceAndPath(Solenoid.MODID, id))));
            });

            // Block Items (3D block in hand)
            List.of("crusher", "separator", "induction_furnace", "capacitor", "chemical_reactor", "digester", "centrifuge", "thorium_rtg", "recharger", "mob_magnet").forEach(id -> {
                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                com.google.gson.JsonObject model = new com.google.gson.JsonObject();
                model.addProperty("type", "minecraft:model");
                model.addProperty("model", "solenoid:block/" + id);
                json.add("model", model);
                futures.add(net.minecraft.data.DataProvider.saveStable(cache, json, pathProvider.json(Identifier.fromNamespaceAndPath(Solenoid.MODID, id))));
            });

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }

        @Override
        public String getName() {
            return "Item Model Definitions";
        }
    }

    // ---- Language ----

    private static class ModLanguageProvider extends LanguageProvider {
        public ModLanguageProvider(PackOutput output) {
            super(output, Solenoid.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            addBlock(MagnetiteBlocks.MAGNETITE_ORE, "Magnetite Ore");
            addBlock(MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE, "Deepslate Magnetite Ore");
            addBlock(MagnetiteBlocks.MONAZITE_ORE, "Monazite Ore");
            addItem(MagnetiteItems.RAW_MAGNETITE, "Raw Magnetite");
            addItem(MagnetiteItems.MAGNETITE_INGOT, "Magnetite Ingot");
            addItem(MagnetiteItems.MAGNET, "Magnet");
            addItem(MagnetiteItems.COPPER_COIL, "Copper Coil");
            addItem(MagnetiteItems.GRINDING_GEAR, "Grinding Gear");
            addItem(MagnetiteItems.RAW_MONAZITE, "Raw Monazite");
            addItem(MagnetiteItems.CERIUM_DUST, "Cerium Dust");
            addItem(MagnetiteItems.NEODYMIUM_DUST, "Neodymium Dust");
            addItem(MagnetiteItems.THORIUM_DUST, "Thorium Dust");
            addItem(MagnetiteItems.COPPER_CERIUM_BATTERY, "Copper-Cerium Battery");
            addItem(MagnetiteItems.CERIUM_INGOT, "Cerium Ingot");
            addItem(MagnetiteItems.NEODYMIUM_INGOT, "Neodymium Ingot");
            addItem(MagnetiteItems.THORIUM_INGOT, "Thorium Ingot");
            addItem(MagnetiteItems.THORIUM_PELLET, "Thorium Pellet");

            addItem(MagnetiteItems.MACHINE_FRAME, "Machines Frame");
            addItem(MagnetiteItems.SCREW, "Screw");
            addItem(MagnetiteItems.MAGNET_CHARM, "Magnet Charm");
            addItem(MagnetiteItems.REPULSOR, "Repulsor");
            addItem(MagnetiteItems.FORCE_FIELD_GENERATOR, "Force Field Generator");

            // Monazite processing line intermediates
            addItem(MagnetiteItems.SAWDUST, "Sawdust");
            addItem(MagnetiteItems.LYE, "Lye");
            addItem(MagnetiteItems.CRUSHED_MONAZITE, "Crushed Monazite");
            addItem(MagnetiteItems.MONAZITE_CONCENTRATE, "Monazite Concentrate");
            addItem(MagnetiteItems.RARE_EARTH_CAKE, "Rare Earth Cake");
            addItem(MagnetiteItems.THORIUM_SLUDGE, "Thorium Sludge");
            addItem(MagnetiteItems.PHOSPHATE, "Phosphate");

            // Ore processing items
            for (ProcessedOre ore : ProcessedOre.values()) {
                String oreName = ore.getName().substring(0, 1).toUpperCase() + ore.getName().substring(1);
                for (ProcessedForm form : ProcessedForm.values()) {
                    String id = form.getPrefix() + ore.getName() + form.getSuffix();
                    String name = switch (form) {
                        case CRUSHED -> "Crushed " + oreName;
                        case CONCENTRATE -> oreName + " Concentrate";
                    };
                    add("item." + Solenoid.MODID + "." + id, name);
                }
            }

            add("itemGroup.solenoid", "Solenoid");

            // EMF energy blocks
            addBlock(EmfBlocks.HAND_CRANK_GENERATOR, "Hand-Crank Generator");
            addBlock(EmfBlocks.EMF_SOURCE, "Creative EMF Source");
            addBlock(EmfBlocks.COPPER_CABLE, "Copper Cable");
            addBlock(EmfBlocks.EMF_SINK, "EMF Sink");
            addBlock(EmfBlocks.CRUSHER, "Electromagnetic Crusher");
            addBlock(EmfBlocks.SEPARATOR, "Electromagnetic Separator");
            addBlock(EmfBlocks.INDUCTION_FURNACE, "Induction Furnace");
            addBlock(EmfBlocks.CAPACITOR, "Electromagnetic Capacitor");
            addBlock(EmfBlocks.CHEMICAL_REACTOR, "Electromagnetic Chemical Reactor");
            addBlock(EmfBlocks.DIGESTER, "Electromagnetic Digester");
            addBlock(EmfBlocks.CENTRIFUGE, "Electromagnetic Centrifuge");
            addBlock(EmfBlocks.THORIUM_RTG, "Thorium RTG");
            addBlock(EmfBlocks.RECHARGER, "Recharger");
            addBlock(EmfBlocks.MOB_MAGNET, "Mob Magnet");

            // EMF tooltips (role + sink capacity)
            add("tooltip.solenoid.hand_crank_generator", "Generates EMF when cranked.");
            add("tooltip.solenoid.hand_crank_generator.capacity", "Stores up to 50,000 EMF.");
            add("tooltip.solenoid.emf_source", "Pushes 256 EMF/t into adjacent blocks. Infinite.");
            add("tooltip.solenoid.copper_cable", "Moves EMF between machines.");
            add("tooltip.solenoid.emf_sink", "Receives up to 256 EMF/t. Comparator-readable.");
            add("tooltip.solenoid.emf_sink.capacity", "Stores up to 100,000 EMF");
            add("tooltip.solenoid.crusher", "Crushes ores into dust");
            add("tooltip.solenoid.separator", "Separates ore dust into slag and product");
            add("tooltip.solenoid.induction_furnace", "Smelts items using EMF, no fuel needed");
            add("tooltip.solenoid.rtg.output", "Passively generates 8 EMF/t.");
            add("tooltip.solenoid.rtg.active", "Active: Decay process ongoing");
            add("tooltip.solenoid.rtg.depleted", "Depleted: Fuel exhausted");
            add("tooltip.solenoid.chemical_reactor", "Reacts materials using EMF");
            add("tooltip.solenoid.digester", "Digests materials using EMF");
            add("tooltip.solenoid.centrifuge", "Centrifuges materials using EMF");
            add("tooltip.solenoid.recharger", "Charges held EMF items at 200 EMF/t.");
            add("tooltip.solenoid.mob_magnet", "Pulls nearby entities into farms using magnetic attraction.");
            
            add("item.solenoid.magnetometer", "Solenoid Magnetometer");
            add("item.solenoid.wrench", "Solenoid Wrench");
            add("item.solenoid.multimeter", "Multimeter");
            add("item.solenoid.screen", "Screen");
            add("item.solenoid.induction_surveyor", "Induction Surveyor");

            add("message.solenoid.wrench.facing", "Facing: %s");
            add("message.solenoid.wrench.cable", "Cable %s: %s");

            add("message.solenoid.multimeter.energy", "EMF: %s / %s (%d%%)");
            add("message.solenoid.multimeter.progress", "Progress: %d%% (%s)");
            add("message.solenoid.multimeter.no_data", "No EMF data");
            
            add("tooltip.solenoid.capacitor", "Stores EMF and powers adjacent machines");
            add("tooltip.solenoid.capacitor.capacity", "Capacity: 100,000 EMF");
            add("tooltip.solenoid.magnet_charm.state", "State: %s");
            add("tooltip.solenoid.force_field.state", "Shield: %s");
            add("message.solenoid.force_field.toggle", "Force field: %s");
            add("message.solenoid.force_field.depleted", "Force field depleted!");
            add("tooltip.solenoid.energy_stored", "Energy: %d / %d EMF");
            
            // Machine GUI container titles. Shorter than the block names on purpose: the console
            // header only has room up to the status lamp, and the block already says which machine
            // this is. Longer titles still render, clipped with an ellipsis.
            add("container.solenoid.crusher", "Crusher");
            add("container.solenoid.separator", "Separator");
            add("container.solenoid.induction_furnace", "Induction Furnace");
            add("container.solenoid.capacitor", "Capacitor");
            add("container.solenoid.chemical_reactor", "Chemical Reactor");
            add("container.solenoid.digester", "Digester");
            add("container.solenoid.centrifuge", "Centrifuge");
            add("container.solenoid.recharger", "Recharger");
            // These four lived only in the hand-written en_us.json, which the generated file shadows
            // in the built jar — so they were reaching the game as raw keys.
            add("container.solenoid.thorium_rtg", "Thorium RTG");
            add("item.solenoid.thorium_rtg", "Thorium RTG");
            add("message.solenoid.on", "ON");
            add("message.solenoid.off", "OFF");

            // JEI Categories
            add("gui.solenoid.category.crushing", "Crushing");
            add("gui.solenoid.category.separating", "Electromagnetic Separation");
            add("gui.solenoid.category.reacting", "Chemical Reaction");
            add("gui.solenoid.category.digesting", "Digestion");
            add("gui.solenoid.category.centrifuging", "Centrifuging");
            add("gui.solenoid.show_recipes", "Show Recipes");

            // Machine console UI
            add("gui.solenoid.energy", "Energy Buffer");
            add("gui.solenoid.status.no_power", "No power");
            add("gui.solenoid.status.running", "Running");
            add("gui.solenoid.status.idle", "Idle");
            add("gui.solenoid.status.charged", "Charged");
            add("gui.solenoid.status.generating", "Generating");
            add("gui.solenoid.status.depleted", "Fuel depleted");
            add("gui.solenoid.separator.field", "Field");
            add("gui.solenoid.separator.field.active", "Magnetic field active");
            add("gui.solenoid.separator.field.inactive", "Magnetic field inactive");
            add("gui.solenoid.digester.feed", "Feed");
            add("gui.solenoid.digester.reagent", "Reagent");
            add("gui.solenoid.capacitor.charge", "Charge");
            add("gui.solenoid.recharger.slot", "Insert item");
            add("gui.solenoid.recharger.charging", "Charging");
            add("gui.solenoid.recharger.idle", "Nothing to charge");
            add("gui.solenoid.rtg.output", "Output");
            add("gui.solenoid.rtg.buffer", "Buffer");
            add("gui.solenoid.rtg.fuel", "Fuel");
            add("gui.solenoid.mob_magnet.radius", "Field Range: %d Blocks");
            add("gui.solenoid.mob_magnet.cost", "Drain: %d EMF/t");
            add("gui.solenoid.recipe.energy", "%s EMF");
            add("gui.solenoid.recipe.rate", "%s EMF/t");
            add("gui.solenoid.recipe.energy_rate", "%s EMF (%s EMF/t)");
            add("gui.solenoid.recipe.time", "%s ticks");
            add("gui.solenoid.recipe.chance", "%s%%");

            // Config screen
            add("solenoid.configuration.title", "Solenoid Configs");
            add("solenoid.configuration.section.solenoid.common.toml", "Solenoid Configs");
            add("solenoid.configuration.section.solenoid.common.toml.title", "Solenoid Configs");
            add("solenoid.configuration.items", "Item List");
            add("solenoid.configuration.logDirtBlock", "Log Dirt Block");
            add("solenoid.configuration.magicNumberIntroduction", "Magic Number Text");
            add("solenoid.configuration.magicNumber", "Magic Number");
        }
    }

    // ---- Loot Tables ----

    private static class ModLootTableProvider extends LootTableProvider {
        public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, Set.of(), List.of(
                    new SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)),
                    lookup);
        }
    }

    private static class ModBlockLoot extends BlockLootSubProvider {
        protected ModBlockLoot(HolderLookup.Provider provider) {
            super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), provider);
        }

        @Override
        protected void generate() {
            add(MagnetiteBlocks.MAGNETITE_ORE.get(),
                    createOreDrop(MagnetiteBlocks.MAGNETITE_ORE.get(), MagnetiteItems.RAW_MAGNETITE.get()));
            add(MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE.get(),
                    createOreDrop(MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE.get(), MagnetiteItems.RAW_MAGNETITE.get()));
            dropSelf(MagnetiteBlocks.MONAZITE_ORE.get());
            dropSelf(EmfBlocks.HAND_CRANK_GENERATOR.get());
            dropSelf(EmfBlocks.COPPER_CABLE.get());
            dropSelf(EmfBlocks.EMF_SOURCE.get());
            dropSelf(EmfBlocks.EMF_SINK.get());
            dropSelf(EmfBlocks.CRUSHER.get());
            dropSelf(EmfBlocks.SEPARATOR.get());
            dropSelf(EmfBlocks.INDUCTION_FURNACE.get());
            dropSelf(EmfBlocks.CAPACITOR.get());
            dropSelf(EmfBlocks.CHEMICAL_REACTOR.get());
            dropSelf(EmfBlocks.DIGESTER.get());
            dropSelf(EmfBlocks.CENTRIFUGE.get());
            dropSelf(EmfBlocks.THORIUM_RTG.get());
            dropSelf(EmfBlocks.RECHARGER.get());
            dropSelf(EmfBlocks.MOB_MAGNET.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return BuiltInRegistries.BLOCK.stream()
                    .filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Solenoid.MODID))
                    .toList();
        }
    }

    // ---- Block Tags ----

    private static class ModBlockTagsProvider extends BlockTagsProvider {
        public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, Solenoid.MODID);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup) {
            var magOre = MagnetiteBlocks.MAGNETITE_ORE.get();
            var deepOre = MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE.get();

            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(magOre, deepOre, MagnetiteBlocks.MONAZITE_ORE.get(), EmfBlocks.MOB_MAGNET.get());
            tag(BlockTags.NEEDS_STONE_TOOL).add(magOre, deepOre, MagnetiteBlocks.MONAZITE_ORE.get(), EmfBlocks.MOB_MAGNET.get());

            tag(Tags.Blocks.ORES).add(magOre, deepOre, MagnetiteBlocks.MONAZITE_ORE.get());
            tag(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/monazite")))
                    .add(MagnetiteBlocks.MONAZITE_ORE.get());

            tag(SolenoidTags.Blocks.CONDUCTIVE).add(
                    MagnetiteBlocks.MONAZITE_ORE.get(),
                    net.minecraft.world.level.block.Blocks.COPPER_ORE,
                    net.minecraft.world.level.block.Blocks.DEEPSLATE_COPPER_ORE,
                    net.minecraft.world.level.block.Blocks.GOLD_ORE,
                    net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE,
                    net.minecraft.world.level.block.Blocks.LAPIS_ORE,
                    net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE,
                    net.minecraft.world.level.block.Blocks.REDSTONE_ORE
            );

            tag(SolenoidTags.Blocks.WRENCHABLE).add(
                    EmfBlocks.CRUSHER.get(),
                    EmfBlocks.SEPARATOR.get(),
                    EmfBlocks.INDUCTION_FURNACE.get(),
                    EmfBlocks.CAPACITOR.get(),
                    EmfBlocks.HAND_CRANK_GENERATOR.get(),
                    EmfBlocks.CHEMICAL_REACTOR.get(),
                    EmfBlocks.DIGESTER.get(),
                    EmfBlocks.CENTRIFUGE.get(),
                    EmfBlocks.THORIUM_RTG.get(),
                    EmfBlocks.RECHARGER.get(),
                    EmfBlocks.MOB_MAGNET.get()
            );
        }
    }

    // ---- Item Tags ----

    private static class ModItemTagsProvider extends ItemTagsProvider {
        public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                    CompletableFuture<TagLookup<Block>> blockTags) {
            super(output, lookup, Solenoid.MODID);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup) {
            var magOreItem = MagnetiteBlocks.MAGNETITE_ORE_ITEM.get();
            var deepOreItem = MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE_ITEM.get();
            var monOreItem = MagnetiteBlocks.MONAZITE_ORE_ITEM.get();
            var raw = MagnetiteItems.RAW_MAGNETITE.get();
            var ingot = MagnetiteItems.MAGNETITE_INGOT.get();

            tag(Tags.Items.ORES).add(magOreItem, deepOreItem, monOreItem);
            tag(ctag("ores/magnetite")).add(magOreItem, deepOreItem);
            tag(ctag("ores/monazite")).add(monOreItem);

            tag(Tags.Items.RAW_MATERIALS).add(raw);
            tag(ctag("raw_materials/magnetite")).add(raw);

            tag(Tags.Items.INGOTS).add(ingot);
            tag(ctag("ingots/magnetite")).add(ingot);

            for (ProcessedOre ore : ProcessedOre.values()) {
                for (ProcessedForm form : ProcessedForm.values()) {
                    Item item = OreProcessingItems.getItem(ore, form).get();
                    String path = switch (form) {
                        case CRUSHED -> "crushed_ores/";
                        case CONCENTRATE -> "ore_concentrates/";
                    } + ore.getName();
                    tag(ctag(path)).add(item);
                }
            }

            tag(SolenoidTags.Items.MAGNETS).add(MagnetiteItems.MAGNET.get(), MagnetiteItems.MAGNET_CHARM.get(), MagnetiteItems.REPULSOR.get());
        }

        private static TagKey<Item> ctag(String path) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
        }
    }

    private static final TagKey<Block> STONE_ORE_REPLACEABLES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ore_bearing_ground/stone"));
    private static final TagKey<Block> DEEPSLATE_ORE_REPLACEABLES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ore_bearing_ground/deepslate"));

    // ---- Worldgen ----

    private static class ModWorldgenProvider extends DatapackBuiltinEntriesProvider {
        public ModWorldgenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, new net.minecraft.core.RegistrySetBuilder()
                    .add(Registries.CONFIGURED_FEATURE, bootstrap -> {
                        bootstrap.register(
                                ResourceKey.create(Registries.CONFIGURED_FEATURE,
                                        Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore")),
                                new ConfiguredFeature<>(
                                        Feature.ORE,
                                        new OreConfiguration(
                                                List.of(
                                                        OreConfiguration.target(
                                                                new TagMatchTest(STONE_ORE_REPLACEABLES),
                                                                MagnetiteBlocks.MAGNETITE_ORE.get().defaultBlockState()),
                                                        OreConfiguration.target(
                                                                new TagMatchTest(DEEPSLATE_ORE_REPLACEABLES),
                                                                MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE.get()
                                                                        .defaultBlockState())),
                                                7,
                                                0.0f)));
                        
                        bootstrap.register(
                                ResourceKey.create(Registries.CONFIGURED_FEATURE,
                                        Identifier.fromNamespaceAndPath(Solenoid.MODID, "monazite_ore")),
                                new ConfiguredFeature<>(
                                        Feature.ORE,
                                        new OreConfiguration(
                                                List.of(
                                                        OreConfiguration.target(
                                                                new TagMatchTest(STONE_ORE_REPLACEABLES),
                                                                MagnetiteBlocks.MONAZITE_ORE.get().defaultBlockState())),
                                                4,
                                                0.0f)));
                    })
                    .add(Registries.PLACED_FEATURE, bootstrap -> {
                        var magCfKey = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                                Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore"));
                        bootstrap.register(
                                ResourceKey.create(Registries.PLACED_FEATURE,
                                        Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore")),
                                new PlacedFeature(
                                        bootstrap.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(magCfKey),
                                        List.of(
                                                CountPlacement.of(5),
                                                InSquarePlacement.spread(),
                                                HeightRangePlacement.uniform(
                                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(-48),
                                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(48)),
                                                BiomeFilter.biome())));
                        
                        var monCfKey = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                                Identifier.fromNamespaceAndPath(Solenoid.MODID, "monazite_ore"));
                        bootstrap.register(
                                ResourceKey.create(Registries.PLACED_FEATURE,
                                        Identifier.fromNamespaceAndPath(Solenoid.MODID, "monazite_ore")),
                                new PlacedFeature(
                                        bootstrap.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(monCfKey),
                                        List.of(
                                                CountPlacement.of(net.minecraft.util.valueproviders.UniformInt.of(3, 16)),
                                                InSquarePlacement.spread(),
                                                HeightRangePlacement.triangle(
                                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(0),
                                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(128)),
                                                BiomeFilter.biome())));
                    })
                    .add(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS, bootstrap -> {
                        var magPfKey = ResourceKey.create(Registries.PLACED_FEATURE,
                                Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore"));
                        bootstrap.register(
                                ResourceKey.create(
                                        net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                                        Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore")),
                                new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                                        bootstrap.lookup(Registries.BIOME)
                                                .getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD),
                                        net.minecraft.core.HolderSet.direct(
                                                bootstrap.lookup(Registries.PLACED_FEATURE).getOrThrow(magPfKey)),
                                        net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES));
                        
                        var monPfKey = ResourceKey.create(Registries.PLACED_FEATURE,
                                Identifier.fromNamespaceAndPath(Solenoid.MODID, "monazite_ore"));
                        bootstrap.register(
                                ResourceKey.create(
                                        net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                                        Identifier.fromNamespaceAndPath(Solenoid.MODID, "monazite_ore")),
                                new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                                        bootstrap.lookup(Registries.BIOME)
                                                .getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD),
                                        net.minecraft.core.HolderSet.direct(
                                                bootstrap.lookup(Registries.PLACED_FEATURE).getOrThrow(monPfKey)),
                                        net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES));
                    }),
                    Set.of(Solenoid.MODID));
        }
    }
}
