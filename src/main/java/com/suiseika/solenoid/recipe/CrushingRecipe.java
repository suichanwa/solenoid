package com.suiseika.solenoid.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

public record CrushingRecipe(Ingredient ingredient, ItemStackTemplate result, int energy, int time) implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
    }

    @Override
    public RecipeSerializer<? extends CrushingRecipe> getSerializer() {
        return SolenoidRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends CrushingRecipe> getType() {
        return SolenoidRecipes.CRUSHING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
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
