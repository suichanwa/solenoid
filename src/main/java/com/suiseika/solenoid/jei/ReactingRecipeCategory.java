package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.ReactingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ReactingRecipeCategory implements IRecipeCategory<ReactingRecipe> {
    private static final int WIDTH = 120;
    private static final int HEIGHT = 54;
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public ReactingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(EmfBlocks.CHEMICAL_REACTOR_ITEM.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<ReactingRecipe> getRecipeType() {
        return SolenoidJeiPlugin.REACTING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.solenoid.category.reacting");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReactingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(4, 10)
                .setStandardSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, 
                        recipe.ingredient().items().map(net.minecraft.world.item.ItemStack::new).toList());

        builder.addOutputSlot(66, 9)
                .setOutputSlotBackground()
                .addIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, recipe.result().create());
    }

    @Override
    public void draw(ReactingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 30, 11);

        Font font = Minecraft.getInstance().font;
        if (recipe.inputCount() > 1) {
            guiGraphics.text(font, "x" + recipe.inputCount(), 4, 28, TEXT_COLOR, false);
        }
        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.energy", recipe.energy()), 4, 34, TEXT_COLOR, false);
        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.time", recipe.time()), 4, 44, TEXT_COLOR, false);
    }
}
