package com.suiseika.solenoid.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

/**
 * Chemical Reactor recipe: a single ingredient (consumed {@code inputCount} at a time) becomes one
 * result over {@code time} ticks, draining {@code energy} EMF total. Used for the reagent sub-chain
 * (e.g. sawdust x4 -> lye).
 */
public record ReactingRecipe(Ingredient ingredient, int inputCount, ItemStackTemplate result, int energy, int time)
        implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item()) && input.item().getCount() >= this.inputCount;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
    }

    @Override
    public RecipeSerializer<? extends ReactingRecipe> getSerializer() {
        return SolenoidRecipes.REACTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends ReactingRecipe> getType() {
        return SolenoidRecipes.REACTING_TYPE.get();
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
