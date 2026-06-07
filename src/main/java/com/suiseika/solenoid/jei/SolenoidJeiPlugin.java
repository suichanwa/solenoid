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
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.Internal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.List;

/**
 * JEI integration for Solenoid's custom machine recipe types (Crusher / Electromagnetic Separator).
 *
 * <p>This class is only loaded by JEI's plugin scanner (it is annotated {@link JeiPlugin}), and the
 * rest of the mod never references it, so the mod runs fine with JEI absent — JEI is a
 * {@code compileOnly}/{@code localRuntime} dependency only.</p>
 *
 * <p>Reading recipes: as of MC 26.1.x the full {@code RecipeManager} is no longer synced to the
 * client (the client only receives {@code RecipePropertySet}s + stonecutter recipes via
 * {@code ClientboundUpdateRecipesPacket}). JEI ships its own full-recipe sync and exposes the
 * resulting {@link RecipeMap} through {@code mezz.jei.common.Internal#getClientSyncedRecipes()}.
 * That is the only client-side source for our custom recipes, so we read from it here.</p>
 */
@JeiPlugin
public class SolenoidJeiPlugin implements IModPlugin {

    public static final IRecipeType<CrushingRecipe> CRUSHING =
            IRecipeType.create(Solenoid.MODID, "crushing", CrushingRecipe.class);
    public static final IRecipeType<SeparatingRecipe> SEPARATING =
            IRecipeType.create(Solenoid.MODID, "separating", SeparatingRecipe.class);

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
        // The Induction Furnace runs vanilla smelting recipes, so link it to JEI's smelting category.
        registration.addCraftingStation(RecipeTypes.SMELTING, EmfBlocks.INDUCTION_FURNACE_ITEM.get());
    }
}
