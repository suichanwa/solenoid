package com.suiseika.solenoid.emi;

import com.suiseika.solenoid.recipe.SeparatingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class SeparatingEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final EmiIngredient input;
    private final EmiStack output;
    private final EmiStack secondary;
    private final float secondaryChance;
    private final int energy;
    private final int time;

    public SeparatingEmiRecipe(RecipeHolder<SeparatingRecipe> recipe) {
        this.id = recipe.id().location();
        this.input = EmiIngredient.of(recipe.value().ingredient());
        this.output = EmiStack.of(recipe.value().result().create());
        this.secondary = EmiStack.of(recipe.value().secondary().create());
        this.secondaryChance = recipe.value().secondaryChance();
        this.energy = recipe.value().energy();
        this.time = recipe.value().time();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return SolenoidEmiPlugin.SEPARATING;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(input);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(output, secondary);
    }

    @Override
    public int getDisplayWidth() {
        return 140;
    }

    @Override
    public int getDisplayHeight() {
        return 64;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(input, 4, 22);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 30, 23);
        widgets.addSlot(output, 66, 8).recipeContext(this);
        widgets.addSlot(secondary, 66, 36).recipeContext(this);

        String chance = String.format("%.0f", secondaryChance * 100.0f);
        widgets.addText(Component.translatable("gui.solenoid.recipe.chance", chance), 90, 40, 0xFF404040, false);

        widgets.addText(Component.translatable("gui.solenoid.recipe.energy", energy), 4, 48, 0xFF404040, false);
        widgets.addText(Component.translatable("gui.solenoid.recipe.time", time), 4, 58, 0xFF404040, false);
    }
}
