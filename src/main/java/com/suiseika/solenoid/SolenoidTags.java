package com.suiseika.solenoid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tag keys used by detection logic. Detection is tag-driven so new ore families can be added by
 * editing the tag JSON -- no code change. The Magnetometer scans {@link Blocks#FERROMAGNETIC};
 * a future EM-induction device can add a {@code CONDUCTIVE} tag + a second blip colour with minimal
 * change (see {@code MagnetometerScreen.DetectionMode}).
 */
public final class SolenoidTags {
    private SolenoidTags() {}

    public static final class Blocks {
        private Blocks() {}

        /** Ferromagnetic ores detectable by the Magnetometer (magnetite family). */
        public static final TagKey<Block> FERROMAGNETIC = create("ferromagnetic");

        /** Blocks that can be rotated with a wrench. */
        public static final TagKey<Block> WRENCHABLE = create("wrenchable");

        /** Conductive ores detectable by the Induction Surveyor (copper, gold, etc). */
        public static final TagKey<Block> CONDUCTIVE = create("conductive");

        private static TagKey<Block> create(String path) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Solenoid.MODID, path));
        }
    }

    public static final class Items {
        private Items() {}

        /** Magnet items (handheld or trinket). */
        public static final TagKey<Item> MAGNETS = create("magnets");

        private static TagKey<Item> create(String path) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Solenoid.MODID, path));
        }
    }
}
