package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.ReactingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ReactingRecipeCategory extends AbstractSolenoidRecipeCategory<ReactingRecipe> {

    public ReactingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, SolenoidJeiPlugin.REACTING,
                Component.translatable("gui.solenoid.category.reacting"),
                EmfBlocks.CHEMICAL_REACTOR_ITEM.get(), DEFAULT_WIDTH, 54);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReactingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(4, 10)
                .setStandardSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, 
                        recipe.ingredient().items().map(net.minecraft.world.item.ItemStack::new).toList());

        builder.addOutputSlot(66, 9)
                .setOutputSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, java.util.List.of(recipe.result().create()));
    }

    @Override
    public void draw(ReactingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 30, 11);
        if (recipe.inputCount() > 1) {
            guiGraphics.text(Minecraft.getInstance().font, "x" + recipe.inputCount(), 4, 28, TEXT_COLOR, false);
        }
        drawStats(guiGraphics, recipe.energy(), recipe.time(), 4, 34, 44);
    }
}
