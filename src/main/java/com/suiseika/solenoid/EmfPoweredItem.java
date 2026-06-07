package com.suiseika.solenoid;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Base class for items that use EMF energy.
 * Handles the energy bar display and provides helper methods for energy management.
 */
public abstract class EmfPoweredItem extends Item {
    public EmfPoweredItem(Properties properties) {
        super(properties);
    }

    /** @return Maximum EMF this item can hold. */
    public abstract int getCapacity();

    /** @return Current EMF stored in the item stack. */
    public int getEnergy(ItemStack stack) {
        return stack.getOrDefault(SolenoidDataComponents.EMF_ENERGY.get(), 0);
    }

    /** Sets the current EMF stored in the item stack, clamped to [0, capacity]. */
    public void setEnergy(ItemStack stack, int value) {
        stack.set(SolenoidDataComponents.EMF_ENERGY.get(), Mth.clamp(value, 0, getCapacity()));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (float)getEnergy(stack) / (float)getCapacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // EMF Cyan color
        return 0xFF00FFFF;
    }
}
