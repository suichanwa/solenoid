package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.DigestingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class DigestingRecipeCategory extends AbstractSolenoidRecipeCategory<DigestingRecipe> {
    private static final int INFO_X = 4, INFO_ENERGY_Y = 58, INFO_TIME_Y = 68;

    public DigestingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, SolenoidJeiPlugin.DIGESTING,
                Component.translatable("gui.solenoid.category.digesting"),
                EmfBlocks.DIGESTER_ITEM.get(), DEFAULT_WIDTH, 80);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DigestingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(4, 8)
                .setStandardSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, 
                        recipe.ingredient().items().map(net.minecraft.world.item.ItemStack::new).toList());
        builder.addInputSlot(4, 36)
                .setStandardSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, 
                        recipe.reagent().items().map(net.minecraft.world.item.ItemStack::new).toList());

        layoutSeparationOutputs(builder, recipe.outputs());
    }

    @Override
    public void draw(DigestingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 30, 23);
        drawSeparationChances(guiGraphics, recipe.outputs());
        drawStats(guiGraphics, recipe.energy(), recipe.time(), INFO_X, INFO_ENERGY_Y, INFO_TIME_Y);
    }
}
