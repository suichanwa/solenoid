package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.CentrifugingRecipe;

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

public class CentrifugingRecipeCategory implements IRecipeCategory<CentrifugingRecipe> {
    private static final int WIDTH = 120;
    private static final int HEIGHT = 68;
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public CentrifugingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(EmfBlocks.CENTRIFUGE_ITEM.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<CentrifugingRecipe> getRecipeType() {
        return SolenoidJeiPlugin.CENTRIFUGING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.solenoid.category.centrifuging");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CentrifugingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(4, 22)
                .setStandardSlotBackground()
                .addIngredients(recipe.ingredient());

        if (recipe.outputs().size() >= 1) {
            builder.addOutputSlot(66, 8)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.outputs().get(0).create());
        }
        if (recipe.outputs().size() >= 2) {
            builder.addOutputSlot(66, 36)
                    .setStandardSlotBackground()
                    .addItemStack(recipe.outputs().get(1).create());
        }
        if (recipe.outputs().size() >= 3) {
            builder.addOutputSlot(94, 22)
                    .setStandardSlotBackground()
                    .addItemStack(recipe.outputs().get(2).create());
        }
    }

    @Override
    public void draw(CentrifugingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 30, 23);

        Font font = Minecraft.getInstance().font;
        if (recipe.outputs().size() >= 2) {
            float c = recipe.outputs().get(1).chance();
            if (c < 1.0f) {
                guiGraphics.text(font, String.format("%.0f%%", c * 100.0f), 66, 56, TEXT_COLOR, false);
            }
        }
        if (recipe.outputs().size() >= 3) {
            float c = recipe.outputs().get(2).chance();
            if (c < 1.0f) {
                guiGraphics.text(font, String.format("%.0f%%", c * 100.0f), 94, 42, TEXT_COLOR, false);
            }
        }

        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.energy", recipe.energy()), 4, 48, TEXT_COLOR, false);
        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.time", recipe.time()), 4, 58, TEXT_COLOR, false);
    }
}
