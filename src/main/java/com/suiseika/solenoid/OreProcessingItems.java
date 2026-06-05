package com.suiseika.solenoid;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OreProcessingItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Solenoid.MODID);

    // Crushed Ores
    public static final DeferredItem<Item> CRUSHED_MAGNETITE = ITEMS.registerSimpleItem("crushed_magnetite");
    public static final DeferredItem<Item> CRUSHED_COPPER = ITEMS.registerSimpleItem("crushed_copper");
    public static final DeferredItem<Item> CRUSHED_IRON = ITEMS.registerSimpleItem("crushed_iron");

    // Concentrates
    public static final DeferredItem<Item> MAGNETITE_CONCENTRATE = ITEMS.registerSimpleItem("magnetite_concentrate");
    public static final DeferredItem<Item> COPPER_CONCENTRATE = ITEMS.registerSimpleItem("copper_concentrate");
    public static final DeferredItem<Item> IRON_CONCENTRATE = ITEMS.registerSimpleItem("iron_concentrate");

    // Slags
    public static final DeferredItem<Item> MAGNETITE_SLAG = ITEMS.registerSimpleItem("magnetite_slag");
    public static final DeferredItem<Item> COPPER_SLAG = ITEMS.registerSimpleItem("copper_slag");
    public static final DeferredItem<Item> IRON_SLAG = ITEMS.registerSimpleItem("iron_slag");
}
