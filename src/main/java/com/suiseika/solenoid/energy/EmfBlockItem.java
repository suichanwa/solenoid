package com.suiseika.solenoid.energy;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * BlockItem that appends fixed tooltip lines describing the block's EMF role. Lines are passed as
 * translation keys so the actual text lives in the language file.
 */
public class EmfBlockItem extends BlockItem {
    private final String[] tooltipKeys;

    public EmfBlockItem(Block block, Properties properties, String... tooltipKeys) {
        super(block, properties);
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        for (String key : tooltipKeys) {
            adder.accept(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
