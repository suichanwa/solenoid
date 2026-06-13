package com.suiseika.solenoid;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MagnetiteItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Solenoid.MODID);

    public static final DeferredItem<Item> RAW_MAGNETITE = ITEMS.registerItem("raw_magnetite", Item::new);

    public static final DeferredItem<Item> MAGNETITE_INGOT = ITEMS.registerItem("magnetite_ingot", Item::new);

    public static final DeferredItem<MagnetItem> MAGNET = ITEMS.registerItem("magnet", MagnetItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerItem("copper_coil", Item::new);

    public static final DeferredItem<Item> GRINDING_GEAR = ITEMS.registerItem("grinding_gear", Item::new);

    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.registerItem("wrench", WrenchItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<MultimeterItem> MULTIMETER = ITEMS.registerItem("multimeter", MultimeterItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> SCREEN = ITEMS.registerItem("screen", Item::new);

    public static final DeferredItem<InductionSurveyorItem> INDUCTION_SURVEYOR =
            ITEMS.registerItem("induction_surveyor", InductionSurveyorItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> RAW_MONAZITE = ITEMS.registerItem("raw_monazite", Item::new);

    // Monazite processing line intermediates.
    public static final DeferredItem<Item> SAWDUST = ITEMS.registerItem("sawdust", Item::new);
    public static final DeferredItem<Item> LYE = ITEMS.registerItem("lye", Item::new);
    public static final DeferredItem<Item> CRUSHED_MONAZITE = ITEMS.registerItem("crushed_monazite", Item::new);
    public static final DeferredItem<Item> MONAZITE_CONCENTRATE = ITEMS.registerItem("monazite_concentrate", Item::new);
    public static final DeferredItem<Item> RARE_EARTH_CAKE = ITEMS.registerItem("rare_earth_cake", Item::new);
    public static final DeferredItem<Item> THORIUM_SLUDGE = ITEMS.registerItem("thorium_sludge", Item::new);
    public static final DeferredItem<Item> PHOSPHATE = ITEMS.registerItem("phosphate", Item::new);

    public static final DeferredItem<Item> CERIUM_DUST = ITEMS.registerItem("cerium_dust", Item::new);
    public static final DeferredItem<Item> NEODYMIUM_DUST = ITEMS.registerItem("neodymium_dust", Item::new);
    public static final DeferredItem<Item> THORIUM_DUST = ITEMS.registerItem("thorium_dust", Item::new);

    public static final DeferredItem<Item> COPPER_CERIUM_BATTERY = ITEMS.registerItem("copper_cerium_battery", Item::new);

    public static final DeferredItem<Item> CERIUM_INGOT = ITEMS.registerItem("cerium_ingot", Item::new);
    public static final DeferredItem<Item> NEODYMIUM_INGOT = ITEMS.registerItem("neodymium_ingot", Item::new);
    public static final DeferredItem<Item> THORIUM_INGOT = ITEMS.registerItem("thorium_ingot", Item::new);

    public static final DeferredItem<Item> THORIUM_PELLET = ITEMS.registerItem("thorium_pellet", Item::new);

    public static final DeferredItem<Item> MACHINE_FRAME = ITEMS.registerItem("machine_frame", Item::new);
    public static final DeferredItem<Item> SCREW = ITEMS.registerItem("screw", Item::new);

    public static final DeferredItem<MagnetCharmItem> MAGNET_CHARM =
            ITEMS.registerItem("magnet_charm", MagnetCharmItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<RepulsorItem> REPULSOR =
            ITEMS.registerItem("repulsor", RepulsorItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<ForceFieldGeneratorItem> FORCE_FIELD_GENERATOR =
            ITEMS.registerItem("force_field_generator", ForceFieldGeneratorItem::new, p -> p.stacksTo(1));
}
