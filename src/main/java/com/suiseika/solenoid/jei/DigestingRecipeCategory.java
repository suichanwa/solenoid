package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.DigestingRecipe;

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

public class DigestingRecipeCategory implements IRecipeCategory<DigestingRecipe> {
    private static final int WIDTH = 120;
    // Taller than the sibling categories: the Digester stacks input + reagent down the left column,
    // so the reagent slot runs to y=53 and the energy/time lines need a clear row beneath it.
    private static final int HEIGHT = 80;
    private static final int TEXT_COLOR = 0xFF404040;
    /** First text row, clear of the reagent slot's 18px background (which ends at y=53). */
    private static final int INFO_X = 4, INFO_ENERGY_Y = 58, INFO_TIME_Y = 68;

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public DigestingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(EmfBlocks.DIGESTER_ITEM.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<DigestingRecipe> getRecipeType() {
        return SolenoidJeiPlugin.DIGESTING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.solenoid.category.digesting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, DigestingRecipe recipe, IFocusGroup focuses) {
        // input + reagent stacked on the left
        builder.addInputSlot(4, 8)
                .setStandardSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, 
                        recipe.ingredient().items().map(net.minecraft.world.item.ItemStack::new).toList());
        builder.addInputSlot(4, 36)
                .setStandardSlotBackground()
                .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, 
                        recipe.reagent().items().map(net.minecraft.world.item.ItemStack::new).toList());

        if (recipe.outputs().size() >= 1) {
            builder.addOutputSlot(66, 8)
                    .setOutputSlotBackground()
                    .add(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, recipe.outputs().get(0).create());
        }
        if (recipe.outputs().size() >= 2) {
            builder.addOutputSlot(66, 36)
                    .setStandardSlotBackground()
                    .addIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, java.util.List.of(recipe.outputs().get(1).create()));
        }
        if (recipe.outputs().size() >= 3) {
            builder.addOutputSlot(94, 22)
                    .setStandardSlotBackground()
                    .add(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, recipe.outputs().get(2).create());
        }
    }

    @Override
    public void draw(DigestingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
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

        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.energy", recipe.energy()),
                INFO_X, INFO_ENERGY_Y, TEXT_COLOR, false);
        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.time", recipe.time()),
                INFO_X, INFO_TIME_Y, TEXT_COLOR, false);
    }
}
