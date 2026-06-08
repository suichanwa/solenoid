package com.suiseika.solenoid.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

/**
 * Centrifuge recipe: a single ingredient is density-separated into a list of chanced outputs over
 * {@code time} ticks, draining {@code energy} EMF total (e.g. rare-earth cake -> cerium x2 + neodymium).
 */
public record CentrifugingRecipe(Ingredient ingredient, List<SeparationOutput> outputs, int energy, int time)
        implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).create();
    }

    @Override
    public RecipeSerializer<? extends CentrifugingRecipe> getSerializer() {
        return SolenoidRecipes.CENTRIFUGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends CentrifugingRecipe> getType() {
        return SolenoidRecipes.CENTRIFUGING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(this.ingredient);
    }

    @Override
    public List<RecipeDisplay> display() {
        return Collections.emptyList();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.FURNACE_MISC;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }
}
