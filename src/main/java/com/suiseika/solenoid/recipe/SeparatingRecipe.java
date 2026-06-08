package com.suiseika.solenoid.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

public record SeparatingRecipe(Ingredient ingredient, List<SeparationOutput> outputs, int energy, int time) implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).create();
    }

    @Override
    public RecipeSerializer<? extends SeparatingRecipe> getSerializer() {
        return SolenoidRecipes.SEPARATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends SeparatingRecipe> getType() {
        return SolenoidRecipes.SEPARATING_TYPE.get();
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
