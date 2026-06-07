package com.suiseika.solenoid;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration for the Magnetometer and any future powered handheld detectors. Uses the same
 * id-on-Properties registration path as the working crushed_X / magnet items so the stack's data
 * components (notably {@link SolenoidDataComponents#EMF_ENERGY}) initialise correctly.
 */
public final class MagnetometerItems {
    private MagnetometerItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Solenoid.MODID);

    public static final DeferredItem<MagnetometerItem> MAGNETOMETER =
            ITEMS.registerItem("magnetometer", MagnetometerItem::new, p -> p.stacksTo(1));
}
