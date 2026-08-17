package com.suiseika.solenoid;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Solenoid Mod Configuration.
 * Creates the `config/solenoid-common.toml` file automatically.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==========================================
    // Vacuum Tube Configuration
    // ==========================================
    public static final ModConfigSpec.IntValue TUBE_EXTRACT_AMOUNT;
    public static final ModConfigSpec.IntValue TUBE_EXTRACT_INTERVAL_TICKS;
    public static final ModConfigSpec.DoubleValue TUBE_UNPOWERED_SPEED;
    public static final ModConfigSpec.DoubleValue TUBE_POWERED_SPEED;
    public static final ModConfigSpec.IntValue TUBE_ENERGY_PER_EXTRACT;
    public static final ModConfigSpec.IntValue TUBE_ENERGY_PER_TRANSIT;

    // ==========================================
    // Magnetic Crane Configuration
    // ==========================================
    public static final ModConfigSpec.IntValue CRANE_RADIUS;
    public static final ModConfigSpec.IntValue CRANE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue CRANE_ENERGY_PER_PULL;

    // ==========================================
    // Mob Magnet Configuration
    // ==========================================
    public static final ModConfigSpec.IntValue MOB_MAGNET_RADIUS;
    public static final ModConfigSpec.DoubleValue MOB_MAGNET_STRENGTH;
    public static final ModConfigSpec.IntValue MOB_MAGNET_ENERGY_USAGE;

    static {
        BUILDER.push("vacuum_tubes");
        TUBE_EXTRACT_AMOUNT = BUILDER
                .comment("Amount of items extracted per operation from connected containers.")
                .defineInRange("extractAmount", 1, 1, 64);

        TUBE_EXTRACT_INTERVAL_TICKS = BUILDER
                .comment("Time in ticks between extractions (20 ticks = 1 second, 4 ticks = 5 transfers/sec).")
                .defineInRange("extractIntervalTicks", 4, 1, 200);

        TUBE_UNPOWERED_SPEED = BUILDER
                .comment("Transit speed of items through unpowered vacuum tubes (progress per tick, 0.125 = 8 ticks/block).")
                .defineInRange("unpoweredSpeed", 0.125, 0.01, 1.0);

        TUBE_POWERED_SPEED = BUILDER
                .comment("Transit speed of items through EMF-powered vacuum tubes (progress per tick, 0.25 = 4 ticks/block).")
                .defineInRange("poweredSpeed", 0.25, 0.01, 1.0);

        TUBE_ENERGY_PER_EXTRACT = BUILDER
                .comment("EMF energy consumed per extraction (if available).")
                .defineInRange("energyPerExtract", 1, 0, 1000);

        TUBE_ENERGY_PER_TRANSIT = BUILDER
                .comment("EMF energy consumed per tick per moving item to maintain high speed.")
                .defineInRange("energyPerTransit", 1, 0, 1000);
        BUILDER.pop();

        BUILDER.push("magnetic_crane");
        CRANE_RADIUS = BUILDER
                .comment("Block radius within which the Magnetic Crane pulls ground items.")
                .defineInRange("radius", 6, 1, 32);

        CRANE_INTERVAL_TICKS = BUILDER
                .comment("Interval in ticks between crane collection scans.")
                .defineInRange("intervalTicks", 4, 1, 100);

        CRANE_ENERGY_PER_PULL = BUILDER
                .comment("EMF energy consumed per vacuum operation.")
                .defineInRange("energyPerPull", 2, 0, 1000);
        BUILDER.pop();

        BUILDER.push("mob_magnet");
        MOB_MAGNET_RADIUS = BUILDER
                .comment("Block radius within which the Mob Magnet attracts entities.")
                .defineInRange("radius", 8, 1, 32);

        MOB_MAGNET_STRENGTH = BUILDER
                .comment("Pull force multiplier applied to entities.")
                .defineInRange("strength", 0.08, 0.01, 1.0);

        MOB_MAGNET_ENERGY_USAGE = BUILDER
                .comment("EMF consumed per tick while the Mob Magnet is active.")
                .defineInRange("energyUsage", 16, 0, 1000);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
