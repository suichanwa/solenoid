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

    /** Max EMF a Copper Cable relays per side, per server tick. */
    public static final int COPPER_CABLE_TRANSFER = 200;

    /** Small internal buffer of an EMF Cable, so it has somewhere to hold in-flight EMF. */
    public static final int CABLE_BUFFER = 2_000;

    /** Small internal buffer of a Copper Cable. */
    public static final int COPPER_CABLE_BUFFER = 400;

    /** Max EMF the EMF Sink accepts per server tick. */
    public static final int SINK_INPUT = 256;

    /** Total EMF the EMF Sink can store. */
    public static final int SINK_CAPACITY = 100_000;

    // ---- Induction Furnace ----

    /** EMF buffer of the Induction Furnace. */
    public static final int INDUCTION_FURNACE_CAPACITY = 10_000;

    /** Max EMF the Induction Furnace accepts per server tick. */
    public static final int INDUCTION_FURNACE_INPUT = 1_000;

    /** EMF consumed by the Induction Furnace per tick of progress. */
    public static final int INDUCTION_FURNACE_ENERGY_PER_TICK = 40;

    /** Ticks the Induction Furnace takes to smelt one item (vanilla furnace is 200). */
    public static final int INDUCTION_FURNACE_SMELT_TICKS = 100;

    /** Chance of a +1 bonus output when the Induction Furnace smelts a metal ingot. */
    public static final float INDUCTION_FURNACE_BONUS_CHANCE = 0.25f;

    // ---- Capacitor ----

    /** Total EMF the Capacitor can store. */
    public static final int CAPACITOR_CAPACITY = 100_000;

    /** Max EMF the Capacitor moves (in or out) per server tick, shared across all faces. */
    public static final int CAPACITOR_TRANSFER = 500;

    // ---- Recharger ----

    /** Total EMF the Recharger buffers. */
    public static final int RECHARGER_CAPACITY = 100_000;

    /** Max EMF the Recharger accepts from cables per server tick. */
    public static final int RECHARGER_INPUT = 1_000;

    /** Max EMF the Recharger pumps into the slotted item per server tick. */
    public static final int RECHARGER_TRANSFER = 200;

    // ---- Thorium RTG ----

    /** EMF trickled out by the Thorium RTG per server tick. */
    public static final int RTG_OUTPUT = 16;

    /** Total fuel units in a new Thorium RTG. Each unit is roughly 1 EMF. */
    public static final int RTG_MAX_FUEL = 1_000_000;
}
