package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.SeparatingRecipe;

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

public class SeparatingRecipeCategory implements IRecipeCategory<SeparatingRecipe> {
    private static final int WIDTH = 140;
    private static final int HEIGHT = 64;
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public SeparatingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(EmfBlocks.SEPARATOR_ITEM.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<SeparatingRecipe> getRecipeType() {
        return SolenoidJeiPlugin.SEPARATING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.solenoid.category.separating");
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
    public void setRecipe(IRecipeLayoutBuilder builder, SeparatingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(4, 22)
                .setStandardSlotBackground()
                .add(recipe.ingredient());

        builder.addOutputSlot(66, 8)
                .setOutputSlotBackground()
                .add(recipe.result().create());

        builder.addOutputSlot(66, 36)
                .setStandardSlotBackground()
                .add(recipe.secondary().create());
    }

    @Override
    public void draw(SeparatingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 30, 23);

        Font font = Minecraft.getInstance().font;
        // Slag chance, drawn next to the secondary (slag) slot.
        String chance = String.format("%.0f", recipe.secondaryChance() * 100.0f);
        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.chance", chance), 90, 40, TEXT_COLOR, false);

        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.energy", recipe.energy()), 4, 48, TEXT_COLOR, false);
        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.time", recipe.time()), 4, 58, TEXT_COLOR, false);
    }
}
