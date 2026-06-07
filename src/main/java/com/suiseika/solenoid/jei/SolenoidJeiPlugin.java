package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.Solenoid;
import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.recipe.CrushingRecipe;
import com.suiseika.solenoid.recipe.SeparatingRecipe;
import com.suiseika.solenoid.recipe.SolenoidRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.Internal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.List;

@JeiPlugin
public class SolenoidJeiPlugin implements IModPlugin {

    public static final RecipeType<CrushingRecipe> CRUSHING =
            new RecipeType<>(Identifier.fromNamespaceAndPath(Solenoid.MODID, "crushing"), CrushingRecipe.class);
    public static final RecipeType<SeparatingRecipe> SEPARATING =
            new RecipeType<>(Identifier.fromNamespaceAndPath(Solenoid.MODID, "separating"), SeparatingRecipe.class);

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Solenoid.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CrushingRecipeCategory(guiHelper),
                new SeparatingRecipeCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeMap recipes = Internal.getClientSyncedRecipes();
        if (recipes == null) {
            return;
        }

        List<CrushingRecipe> crushing = recipes.byType(SolenoidRecipes.CRUSHING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(CRUSHING, crushing);

        List<SeparatingRecipe> separating = recipes.byType(SolenoidRecipes.SEPARATING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(SEPARATING, separating);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(CRUSHING, EmfBlocks.CRUSHER_ITEM.get());
        registration.addCraftingStation(SEPARATING, EmfBlocks.SEPARATOR_ITEM.get());
        registration.addCraftingStation(RecipeTypes.SMELTING, EmfBlocks.INDUCTION_FURNACE_ITEM.get());
    }
}
