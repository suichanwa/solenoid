package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.CentrifugingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class CentrifugingRecipeCategory extends AbstractSolenoidRecipeCategory<CentrifugingRecipe> {

    public CentrifugingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, SolenoidJeiPlugin.CENTRIFUGING,
                Component.translatable("gui.solenoid.category.centrifuging"),
                EmfBlocks.CENTRIFUGE_ITEM.get(), DEFAULT_WIDTH, 68);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CentrifugingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(4, 22)
                .setStandardSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, 
                        recipe.ingredient().items().map(net.minecraft.world.item.ItemStack::new).toList());

        layoutSeparationOutputs(builder, recipe.outputs());
    }

    @Override
    public void draw(CentrifugingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 30, 23);
        drawSeparationChances(guiGraphics, recipe.outputs());
        drawStats(guiGraphics, recipe.energy(), recipe.time(), 4, 48, 58);
    }
}
