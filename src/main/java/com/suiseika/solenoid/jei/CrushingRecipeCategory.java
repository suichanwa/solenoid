package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.CrushingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class CrushingRecipeCategory extends AbstractSolenoidRecipeCategory<CrushingRecipe> {

    public CrushingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, SolenoidJeiPlugin.CRUSHING,
                Component.translatable("gui.solenoid.category.crushing"),
                EmfBlocks.CRUSHER_ITEM.get(), DEFAULT_WIDTH, 54);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrushingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(4, 10)
                .setStandardSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, 
                        recipe.ingredient().items().map(net.minecraft.world.item.ItemStack::new).toList());

        builder.addOutputSlot(66, 9)
                .setOutputSlotBackground()
                .addIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, recipe.result().create());
    }

    @Override
    public void draw(CrushingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 30, 11);
        drawStats(guiGraphics, recipe.energy(), recipe.time(), 4, 34, 44);
    }
}
