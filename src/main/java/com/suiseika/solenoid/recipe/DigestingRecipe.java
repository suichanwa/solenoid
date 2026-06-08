package com.suiseika.solenoid.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

/**
 * Digester recipe: an input ingredient plus a reagent ingredient (1 consumed per op) crack into a
 * list of chanced outputs over {@code time} ticks, draining {@code energy} EMF total
 * (e.g. concentrate + lye -> cake + phosphate + thorium sludge).
 *
 * <p>{@link #matches} only validates the primary input -- the reagent slot is validated by the
 * Digester block entity, which also consumes one reagent per completed operation.
 */
public record DigestingRecipe(Ingredient ingredient, Ingredient reagent, List<SeparationOutput> outputs, int energy, int time)
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
    public RecipeSerializer<? extends DigestingRecipe> getSerializer() {
        return SolenoidRecipes.DIGESTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends DigestingRecipe> getType() {
        return SolenoidRecipes.DIGESTING_TYPE.get();
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
