package com.suiseika.solenoid;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class OreProcessingItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Solenoid.MODID);

    public static final Map<ProcessedOre, Map<ProcessedForm, DeferredItem<Item>>> PROCESSED_ITEMS = new EnumMap<>(ProcessedOre.class);
    public static final DeferredItem<Item> ORE_SLAG = ITEMS.registerSimpleItem("ore_slag");

    static {
        for (ProcessedOre ore : ProcessedOre.values()) {
            Map<ProcessedForm, DeferredItem<Item>> formMap = new EnumMap<>(ProcessedForm.class);
            for (ProcessedForm form : ProcessedForm.values()) {
                String id = form.getPrefix() + ore.getName() + form.getSuffix();
                formMap.put(form, ITEMS.registerSimpleItem(id));
            }
            PROCESSED_ITEMS.put(ore, formMap);
        }
    }

    public static DeferredItem<Item> getItem(ProcessedOre ore, ProcessedForm form) {
        return PROCESSED_ITEMS.get(ore).get(form);
    }
}
