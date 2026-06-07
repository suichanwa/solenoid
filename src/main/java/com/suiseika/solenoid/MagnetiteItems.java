package com.suiseika.solenoid;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MagnetiteItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Solenoid.MODID);

    public static final DeferredItem<Item> RAW_MAGNETITE = ITEMS.registerItem("raw_magnetite", Item::new);

    public static final DeferredItem<Item> MAGNETITE_INGOT = ITEMS.registerItem("magnetite_ingot", Item::new);

    public static final DeferredItem<Item> MAGNET = ITEMS.registerItem("magnet", Item::new);

    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerItem("copper_coil", Item::new);

    public static final DeferredItem<Item> GRINDING_GEAR = ITEMS.registerItem("grinding_gear", Item::new);

    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.registerItem("wrench", WrenchItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<MultimeterItem> MULTIMETER = ITEMS.registerItem("multimeter", MultimeterItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> SCREEN = ITEMS.registerItem("screen", Item::new);

    public static final DeferredItem<InductionSurveyorItem> INDUCTION_SURVEYOR =
            ITEMS.registerItem("induction_surveyor", InductionSurveyorItem::new, p -> p.stacksTo(1));
}
