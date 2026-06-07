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
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
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
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Solenoid.MODID)
public class SolenoidDataGenerator {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        var gen = event.getGenerator();
        var output = gen.getPackOutput();
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

            // Single Ore Slag model
            com.google.gson.JsonObject slagJson = new com.google.gson.JsonObject();
            com.google.gson.JsonObject slagModel = new com.google.gson.JsonObject();
            slagModel.addProperty("type", "minecraft:model");
            slagModel.addProperty("model", "solenoid:item/ore_concentrate_template"); // Placeholder template
            slagJson.add("model", slagModel);
            futures.add(net.minecraft.data.DataProvider.saveStable(cache, slagJson, pathProvider.json(Identifier.fromNamespaceAndPath(Solenoid.MODID, "ore_slag"))));

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
            addItem(MagnetiteItems.RAW_MAGNETITE, "Raw Magnetite");
            addItem(MagnetiteItems.MAGNETITE_INGOT, "Magnetite Ingot");
            addItem(MagnetiteItems.MAGNET, "Magnet");
            addItem(MagnetiteItems.COPPER_COIL, "Copper Coil");

            // Ore processing items
            for (ProcessedOre ore : ProcessedOre.values()) {
                String oreName = ore.getName().substring(0, 1).toUpperCase() + ore.getName().substring(1);
                for (ProcessedForm form : ProcessedForm.values()) {
                    String id = form.getPrefix() + ore.getName() + form.getSuffix();
                    String name;
                    if (form == ProcessedForm.CRUSHED) {
                        name = "Crushed " + oreName;
                    } else {
                        name = oreName + " Concentrate";
                    }
                    add("item." + Solenoid.MODID + "." + id, name);
                }
            }
            add("item." + Solenoid.MODID + ".ore_slag", "Ore Slag");

            add("itemGroup.solenoid", "Solenoid");

            // EMF energy blocks
            addBlock(com.suiseika.solenoid.energy.EmfBlocks.HAND_CRANK_GENERATOR, "Hand-Crank Generator");
            addBlock(com.suiseika.solenoid.energy.EmfBlocks.EMF_SOURCE, "Creative EMF Source");
            addBlock(com.suiseika.solenoid.energy.EmfBlocks.COPPER_CABLE, "Copper Cable");
            addBlock(com.suiseika.solenoid.energy.EmfBlocks.EMF_SINK, "EMF Sink");
            addBlock(com.suiseika.solenoid.energy.EmfBlocks.CRUSHER, "Crusher");
            addBlock(com.suiseika.solenoid.energy.EmfBlocks.SEPARATOR, "Electromagnetic Separator");
            addBlock(com.suiseika.solenoid.energy.EmfBlocks.INDUCTION_FURNACE, "Induction Furnace");
            addBlock(com.suiseika.solenoid.energy.EmfBlocks.CAPACITOR, "Capacitor");
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
            add("tooltip.solenoid.capacitor", "Stores EMF and powers adjacent machines");
            add("tooltip.solenoid.capacitor.capacity", "Capacity: 100,000 EMF");
            // Machine GUI container titles
            add("container.solenoid.crusher", "Crusher");
            add("container.solenoid.separator", "Electromagnetic Separator");
            add("container.solenoid.induction_furnace", "Induction Furnace");
            // JEI recipe categories
            add("gui.solenoid.category.crushing", "Crushing");
            add("gui.solenoid.category.separating", "Electromagnetic Separation");
            add("gui.solenoid.recipe.energy", "%s EMF");
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
            dropSelf(com.suiseika.solenoid.energy.EmfBlocks.HAND_CRANK_GENERATOR.get());
            dropSelf(com.suiseika.solenoid.energy.EmfBlocks.COPPER_CABLE.get());
            dropSelf(com.suiseika.solenoid.energy.EmfBlocks.EMF_SOURCE.get());
            dropSelf(com.suiseika.solenoid.energy.EmfBlocks.EMF_SINK.get());
            dropSelf(com.suiseika.solenoid.energy.EmfBlocks.CRUSHER.get());
            dropSelf(com.suiseika.solenoid.energy.EmfBlocks.SEPARATOR.get());
            dropSelf(com.suiseika.solenoid.energy.EmfBlocks.INDUCTION_FURNACE.get());
            dropSelf(com.suiseika.solenoid.energy.EmfBlocks.CAPACITOR.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(
                    MagnetiteBlocks.MAGNETITE_ORE.get(),
                    MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE.get(),
                    com.suiseika.solenoid.energy.EmfBlocks.HAND_CRANK_GENERATOR.get(),
                    com.suiseika.solenoid.energy.EmfBlocks.COPPER_CABLE.get(),
                    com.suiseika.solenoid.energy.EmfBlocks.EMF_SOURCE.get(),
                    com.suiseika.solenoid.energy.EmfBlocks.EMF_SINK.get(),
                    com.suiseika.solenoid.energy.EmfBlocks.CRUSHER.get(),
                    com.suiseika.solenoid.energy.EmfBlocks.SEPARATOR.get(),
                    com.suiseika.solenoid.energy.EmfBlocks.INDUCTION_FURNACE.get(),
                    com.suiseika.solenoid.energy.EmfBlocks.CAPACITOR.get());
        }
    }

    // ---- Block Tags ----

    private static class ModBlockTagsProvider extends IntrinsicHolderTagsProvider<Block> {
        public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, Registries.BLOCK, lookup, block -> BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow());
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup) {
            var magOre = MagnetiteBlocks.MAGNETITE_ORE.get();
            var deepOre = MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE.get();

            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(magOre, deepOre);
            tag(BlockTags.NEEDS_STONE_TOOL).add(magOre, deepOre);

            tag(Tags.Blocks.ORES).add(magOre, deepOre);
            tag(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/magnetite")))
                    .add(magOre, deepOre);
        }
    }

    // ---- Item Tags ----

    private static class ModItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
        public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                    CompletableFuture<TagLookup<Block>> blockTags) {
            super(output, Registries.ITEM, lookup, item -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup) {
            var magOreItem = MagnetiteBlocks.MAGNETITE_ORE_ITEM.get();
            var deepOreItem = MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE_ITEM.get();
            var raw = MagnetiteItems.RAW_MAGNETITE.get();
            var ingot = MagnetiteItems.MAGNETITE_INGOT.get();

            tag(Tags.Items.ORES).add(magOreItem, deepOreItem);
            tag(ctag("ores/magnetite")).add(magOreItem, deepOreItem);

            tag(Tags.Items.RAW_MATERIALS).add(raw);
            tag(ctag("raw_materials/magnetite")).add(raw);

            tag(Tags.Items.INGOTS).add(ingot);
            tag(ctag("ingots/magnetite")).add(ingot);
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
                    })
                    .add(Registries.PLACED_FEATURE, bootstrap -> {
                        var cfKey = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                                Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore"));
                        bootstrap.register(
                                ResourceKey.create(Registries.PLACED_FEATURE,
                                        Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore")),
                                new PlacedFeature(
                                        bootstrap.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(cfKey),
                                        List.of(
                                                CountPlacement.of(5),
                                                InSquarePlacement.spread(),
                                                HeightRangePlacement.uniform(
                                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(-48),
                                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(48)),
                                                BiomeFilter.biome())));
                    })
                    .add(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS, bootstrap -> {
                        var pfKey = ResourceKey.create(Registries.PLACED_FEATURE,
                                Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore"));
                        bootstrap.register(
                                ResourceKey.create(
                                        net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                                        Identifier.fromNamespaceAndPath(Solenoid.MODID, "magnetite_ore")),
                                new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                                        bootstrap.lookup(Registries.BIOME)
                                                .getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD),
                                        net.minecraft.core.HolderSet.direct(
                                                bootstrap.lookup(Registries.PLACED_FEATURE).getOrThrow(pfKey)),
                                        net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES));
                    }),
                    Set.of(Solenoid.MODID));
        }
    }
}
