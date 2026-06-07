package com.suiseika.solenoid.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

// result/secondary are ItemStackTemplate (not ItemStack) so they parse via the plain Item.CODEC,
// matching the working CrushingRecipe. ItemStack.CODEC requires components to already be bound,
// which is not the case during recipe loading (binding runs after the reload listeners), producing
// "Item ... does not have components yet" errors for modded items referenced as recipe results.
public record SeparatingRecipe(Ingredient ingredient, ItemStackTemplate result, ItemStackTemplate secondary, float secondaryChance, int energy, int time) implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
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
