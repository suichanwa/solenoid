package com.suiseika.solenoid;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Data components attached to item stacks. The EMF energy buffer of powered handheld items (the
 * Magnetometer, and future devices) lives here as a plain {@link Integer} component so it is both
 * NBT-persisted and automatically network-synchronised to the client -- the radar GUI reads the
 * level straight off the held stack with no custom packet.
 */
public final class SolenoidDataComponents {
    private SolenoidDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Solenoid.MODID);

    /** Stored EMF on a powered item stack (Forge Energy units). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EMF_ENERGY =
            COMPONENTS.registerComponentType("emf_energy", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT));
}
