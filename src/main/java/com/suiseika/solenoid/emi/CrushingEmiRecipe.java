package com.suiseika.solenoid.emi;

import com.suiseika.solenoid.recipe.CrushingRecipe;
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

public class CrushingEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final EmiIngredient input;
    private final EmiStack output;
    private final int energy;
    private final int time;

    public CrushingEmiRecipe(RecipeHolder<CrushingRecipe> recipe) {
        this.id = recipe.id().location();
        this.input = EmiIngredient.of(recipe.value().ingredient());
        this.output = EmiStack.of(recipe.value().result().create());
        this.energy = recipe.value().energy();
        this.time = recipe.value().time();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return SolenoidEmiPlugin.CRUSHING;
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
        return List.of(output);
    }

    @Override
    public int getDisplayWidth() {
        return 120;
    }

    @Override
    public int getDisplayHeight() {
        return 54;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(input, 4, 10);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 30, 11);
        widgets.addSlot(output, 66, 9).recipeContext(this);

        widgets.addText(Component.translatable("gui.solenoid.recipe.energy", energy), 4, 34, 0xFF404040, false);
        widgets.addText(Component.translatable("gui.solenoid.recipe.time", time), 4, 44, 0xFF404040, false);
    }
}
