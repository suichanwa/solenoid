package com.suiseika.solenoid;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.suiseika.solenoid.energy.EmfBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.suiseika.solenoid.recipe.SolenoidRecipes;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Solenoid.MODID)
public class Solenoid {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "solenoid";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "solenoid" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a creative tab with the id "solenoid:solenoid_tab" for magnetite items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SOLENOID_TAB = CREATIVE_MODE_TABS.register("solenoid_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.solenoid")) // The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> MagnetiteItems.MAGNETITE_INGOT.toStack())
            .displayItems((parameters, output) -> {
                // Magnetite ores
                output.accept(MagnetiteBlocks.MAGNETITE_ORE_ITEM);
                output.accept(MagnetiteBlocks.DEEPSLATE_MAGNETITE_ORE_ITEM);
                output.accept(MagnetiteBlocks.MONAZITE_ORE_ITEM);
                // Components
                output.accept(MagnetiteItems.RAW_MAGNETITE);
                output.accept(MagnetiteItems.RAW_MONAZITE);
                output.accept(MagnetiteItems.MAGNETITE_INGOT);
                output.accept(MagnetiteItems.CERIUM_INGOT);
                output.accept(MagnetiteItems.NEODYMIUM_INGOT);
                output.accept(MagnetiteItems.THORIUM_INGOT);
                output.accept(MagnetiteItems.CERIUM_DUST);
                output.accept(MagnetiteItems.NEODYMIUM_DUST);
                output.accept(MagnetiteItems.THORIUM_DUST);
                output.accept(MagnetiteItems.COPPER_CERIUM_BATTERY);

                // Ore processing items (crushed / concentrate / slag, all per-ore)
                for (ProcessedForm form : ProcessedForm.values()) {
                    for (ProcessedOre ore : ProcessedOre.values()) {
                        output.accept(OreProcessingItems.getItem(ore, form));
                    }
                }

                // Components
                output.accept(MagnetiteItems.MAGNET);
                output.accept(MagnetiteItems.COPPER_COIL);
                output.accept(MagnetiteItems.GRINDING_GEAR);
                output.accept(MagnetometerItems.MAGNETOMETER);
                output.accept(MagnetiteItems.WRENCH);
                output.accept(MagnetiteItems.MULTIMETER);
                output.accept(MagnetiteItems.SCREEN);
                output.accept(MagnetiteItems.INDUCTION_SURVEYOR);
                // EMF energy blocks
                output.accept(EmfBlocks.HAND_CRANK_GENERATOR_ITEM);
                output.accept(EmfBlocks.CRUSHER_ITEM);
                output.accept(EmfBlocks.SEPARATOR_ITEM);
                output.accept(EmfBlocks.INDUCTION_FURNACE_ITEM);
                output.accept(EmfBlocks.CAPACITOR_ITEM);
                output.accept(EmfBlocks.EMF_SOURCE_ITEM);
                output.accept(EmfBlocks.COPPER_CABLE_ITEM);
                output.accept(EmfBlocks.EMF_SINK_ITEM);
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Solenoid(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Registers to the mod event bus
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register magnetite deferred registers
        MagnetiteBlocks.BLOCKS.register(modEventBus);
        MagnetiteBlocks.ITEMS.register(modEventBus);
        MagnetiteItems.ITEMS.register(modEventBus);
        OreProcessingItems.ITEMS.register(modEventBus);
        SolenoidMenus.MENUS.register(modEventBus);

        // Powered handheld items (Magnetometer) + their stack data components
        SolenoidDataComponents.COMPONENTS.register(modEventBus);
        MagnetometerItems.ITEMS.register(modEventBus);
        modEventBus.addListener(MagnetometerItem::registerCapabilities);
        modEventBus.addListener(InductionSurveyorItem::registerCapabilities);

        // Register recipes
        SolenoidRecipes.RECIPE_TYPES.register(modEventBus);
        SolenoidRecipes.RECIPE_SERIALIZERS.register(modEventBus);

        // Register EMF energy blocks, block entities, items, and the energy capability
        EmfBlocks.register(modEventBus);
        modEventBus.addListener(EmfBlocks::registerCapabilities);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        for (java.lang.reflect.Method m : net.minecraft.world.level.storage.ValueOutput.class.getMethods()) {
            LOGGER.info("ValueOutput method: {} args: {}", m.getName(), java.util.Arrays.toString(m.getParameterTypes()));
        }
        for (java.lang.reflect.Method m : net.minecraft.world.level.storage.ValueInput.class.getMethods()) {
            LOGGER.info("ValueInput method: {} args: {}", m.getName(), java.util.Arrays.toString(m.getParameterTypes()));
        }

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    @SubscribeEvent
    public void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        event.sendRecipes(SolenoidRecipes.CRUSHING_TYPE.get());
        event.sendRecipes(SolenoidRecipes.SEPARATING_TYPE.get());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
