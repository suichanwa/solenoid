package com.suiseika.solenoid;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MagnetiteBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Solenoid.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Solenoid.MODID);

    // ---- Ore blocks ----

    public static final DeferredBlock<Block> MAGNETITE_ORE = BLOCKS.registerSimpleBlock(
            "magnetite_ore",
            p -> p.strength(3.0f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
    );

    public static final DeferredItem<BlockItem> MAGNETITE_ORE_ITEM = ITEMS.registerSimpleBlockItem(
            MAGNETITE_ORE
    );

    public static final DeferredBlock<Block> DEEPSLATE_MAGNETITE_ORE = BLOCKS.registerSimpleBlock(
            "deepslate_magnetite_ore",
            p -> p.strength(4.5f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
    );

    public static final DeferredItem<BlockItem> DEEPSLATE_MAGNETITE_ORE_ITEM = ITEMS.registerSimpleBlockItem(
            DEEPSLATE_MAGNETITE_ORE
    );
}
