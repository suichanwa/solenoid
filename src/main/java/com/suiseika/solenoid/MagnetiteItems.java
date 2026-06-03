package com.suiseika.solenoid;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MagnetiteItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Solenoid.MODID);

    public static final DeferredItem<Item> RAW_MAGNETITE = ITEMS.registerSimpleItem("raw_magnetite");

    public static final DeferredItem<Item> MAGNETITE_INGOT = ITEMS.registerSimpleItem("magnetite_ingot");
}
