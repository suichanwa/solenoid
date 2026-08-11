package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.client.ui.Painter;
import com.suiseika.solenoid.recipe.SeparationOutput;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Base recipe category for all Solenoid JEI machine categories.
 *
 * <p>Centralizes common category infrastructure: width/height, title, icon, arrow, energy/rate/time
 * rendering, multi-output slot positioning with auto-centering for single outputs, and chance tooltips/labels.
 */
public abstract class AbstractSolenoidRecipeCategory<T> implements IRecipeCategory<T> {
    public static final int DEFAULT_WIDTH = 120;
    public static final int TEXT_COLOR = 0xFF404040;

    private final RecipeType<T> recipeType;
    private final Component title;
    private final IDrawable icon;
    protected final IDrawableStatic arrow;
    private final int width;
    private final int height;

    protected AbstractSolenoidRecipeCategory(
            IGuiHelper guiHelper,
            RecipeType<T> recipeType,
            Component title,
            ItemLike iconItem,
            int width,
            int height) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemLike(iconItem);
        this.arrow = guiHelper.getRecipeArrow();
        this.width = width;
        this.height = height;
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /**
     * Draws per-tick EMF consumption rate and execution time labels.
     */
    protected void drawStats(GuiGraphicsExtractor guiGraphics, int energy, int time, int x, int energyY, int timeY) {
        Font font = Minecraft.getInstance().font;
        int rate = Math.max(1, energy / Math.max(1, time));
        guiGraphics.text(font, Component.literal(Painter.number(rate) + " EMF/t"), x, energyY, TEXT_COLOR, false);
        guiGraphics.text(font, Component.translatable("gui.solenoid.recipe.time", time), x, timeY, TEXT_COLOR, false);
    }

    /**
     * Positions 1 to 3 output slots. When only 1 output is present, centers it vertically at y=22
     * to align cleanly with the recipe arrow.
     */
    protected void layoutOutputSlots(IRecipeLayoutBuilder builder, List<ItemStack> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return;
        }
        if (outputs.size() == 1) {
            builder.addOutputSlot(66, 22)
                    .setOutputSlotBackground()
                    .addIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, outputs.get(0));
        } else {
            builder.addOutputSlot(66, 8)
                    .setOutputSlotBackground()
                    .addIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, outputs.get(0));
            builder.addOutputSlot(66, 36)
                    .setStandardSlotBackground()
                    .addIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, outputs.get(1));
            if (outputs.size() >= 3) {
                builder.addOutputSlot(94, 22)
                        .setStandardSlotBackground()
                        .addIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, outputs.get(2));
            }
        }
    }

    /** Helper for SeparationOutput list. */
    protected void layoutSeparationOutputs(IRecipeLayoutBuilder builder, List<SeparationOutput> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return;
        }
        layoutOutputSlots(builder, outputs.stream().map(SeparationOutput::create).toList());
    }

    /**
     * Draws output chance percentages for secondary and tertiary output slots.
     */
    protected void drawOutputChances(GuiGraphicsExtractor guiGraphics, List<Float> chances) {
        if (chances == null || chances.size() < 2) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        float c1 = chances.get(1);
        if (c1 < 1.0f) {
            guiGraphics.text(font, String.format("%.0f%%", c1 * 100.0f), 66, 56, TEXT_COLOR, false);
        }
        if (chances.size() >= 3) {
            float c2 = chances.get(2);
            if (c2 < 1.0f) {
                guiGraphics.text(font, String.format("%.0f%%", c2 * 100.0f), 94, 42, TEXT_COLOR, false);
            }
        }
    }

    /** Helper to draw chances directly from SeparationOutput list. */
    protected void drawSeparationChances(GuiGraphicsExtractor guiGraphics, List<SeparationOutput> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return;
        }
        drawOutputChances(guiGraphics, outputs.stream().map(SeparationOutput::chance).toList());
    }
}
