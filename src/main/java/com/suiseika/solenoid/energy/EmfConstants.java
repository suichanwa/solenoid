package com.suiseika.solenoid.energy;

/**
 * Central place for every tunable number in the EMF energy system.
 *
 * <p>"EMF" is only the display label shown to the player. Mechanically this is standard NeoForge
 * Forge Energy (the {@code Capabilities.Energy.BLOCK} capability), so these values are RF/FE units
 * and fully interoperable with other Forge Energy mods.
 */
public final class EmfConstants {
    private EmfConstants() {}

    /** EMF pushed by the Creative EMF Source into each adjacent receiver, per server tick. */
    public static final int SOURCE_OUTPUT = 256;

    /** Max EMF an EMF Cable relays per side (pull and push), per server tick. */
    public static final int CABLE_TRANSFER = 256;

    /** Small internal buffer of an EMF Cable, so it has somewhere to hold in-flight EMF. */
    public static final int CABLE_BUFFER = 2_000;

    /** Max EMF the EMF Sink accepts per server tick. */
    public static final int SINK_INPUT = 256;

    /** Total EMF the EMF Sink can store. */
    public static final int SINK_CAPACITY = 100_000;
}
