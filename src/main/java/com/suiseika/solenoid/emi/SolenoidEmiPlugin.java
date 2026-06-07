package com.suiseika.solenoid.emi;

import com.suiseika.solenoid.Solenoid;
import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.CrushingRecipe;
import com.suiseika.solenoid.recipe.SeparatingRecipe;
import com.suiseika.solenoid.recipe.SolenoidRecipes;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

public class SolenoidEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CRUSHING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Solenoid.MODID, "crushing"),
            EmiStack.of(EmfBlocks.CRUSHER_ITEM.get())
    );
    public static final EmiRecipeCategory SEPARATING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Solenoid.MODID, "separating"),
            EmiStack.of(EmfBlocks.SEPARATOR_ITEM.get())
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CRUSHING);
        registry.addCategory(SEPARATING);

        registry.addWorkstation(CRUSHING, EmiStack.of(EmfBlocks.CRUSHER_ITEM.get()));
        registry.addWorkstation(SEPARATING, EmiStack.of(EmfBlocks.SEPARATOR_ITEM.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(EmfBlocks.INDUCTION_FURNACE_ITEM.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.BLASTING, EmiStack.of(EmfBlocks.INDUCTION_FURNACE_ITEM.get()));

        for (RecipeHolder<CrushingRecipe> recipe : registry.getRecipeManager().getAllRecipesFor(SolenoidRecipes.CRUSHING_TYPE.get())) {
            registry.addRecipe(new CrushingEmiRecipe(recipe));
        }

        for (RecipeHolder<SeparatingRecipe> recipe : registry.getRecipeManager().getAllRecipesFor(SolenoidRecipes.SEPARATING_TYPE.get())) {
            registry.addRecipe(new SeparatingEmiRecipe(recipe));
        }
    }
}
