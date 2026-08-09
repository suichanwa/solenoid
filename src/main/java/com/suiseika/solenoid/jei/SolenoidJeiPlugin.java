package com.suiseika.solenoid.jei;

import com.suiseika.solenoid.Solenoid;
import com.suiseika.solenoid.client.JeiBridge;
import com.suiseika.solenoid.energy.CentrifugeScreen;
import com.suiseika.solenoid.energy.ChemicalReactorScreen;
import com.suiseika.solenoid.energy.CrusherScreen;
import com.suiseika.solenoid.energy.DigesterScreen;
import com.suiseika.solenoid.energy.EmfBlocks;
import com.suiseika.solenoid.energy.InductionFurnaceScreen;
import com.suiseika.solenoid.energy.SeparatorScreen;
import com.suiseika.solenoid.recipe.CentrifugingRecipe;
import com.suiseika.solenoid.recipe.CrushingRecipe;
import com.suiseika.solenoid.recipe.DigestingRecipe;
import com.suiseika.solenoid.recipe.ReactingRecipe;
import com.suiseika.solenoid.recipe.SeparatingRecipe;
import com.suiseika.solenoid.recipe.SolenoidRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.Internal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.List;
import java.util.Map;

@JeiPlugin
public class SolenoidJeiPlugin implements IModPlugin {

    public static final RecipeType<CrushingRecipe> CRUSHING =
            new RecipeType<>(Identifier.fromNamespaceAndPath(Solenoid.MODID, "crushing"), CrushingRecipe.class);
    public static final RecipeType<SeparatingRecipe> SEPARATING =
            new RecipeType<>(Identifier.fromNamespaceAndPath(Solenoid.MODID, "separating"), SeparatingRecipe.class);
    public static final RecipeType<ReactingRecipe> REACTING =
            new RecipeType<>(Identifier.fromNamespaceAndPath(Solenoid.MODID, "reacting"), ReactingRecipe.class);
    public static final RecipeType<DigestingRecipe> DIGESTING =
            new RecipeType<>(Identifier.fromNamespaceAndPath(Solenoid.MODID, "digesting"), DigestingRecipe.class);
    public static final RecipeType<CentrifugingRecipe> CENTRIFUGING =
            new RecipeType<>(Identifier.fromNamespaceAndPath(Solenoid.MODID, "centrifuging"), CentrifugingRecipe.class);

    /**
     * Which recipe type each machine GUI's "?" button opens. Built inside the client-only
     * {@link #onRuntimeAvailable} rather than in a static field, so the client-only screen classes
     * are never loaded on a dedicated server. Machines with no recipes of their own (Capacitor,
     * Recharger, Thorium RTG) are deliberately absent.
     */
    private static Map<Class<?>, IRecipeType<?>> recipeTypeByScreen() {
        return Map.of(
                CrusherScreen.class, CRUSHING,
                SeparatorScreen.class, SEPARATING,
                ChemicalReactorScreen.class, REACTING,
                DigesterScreen.class, DIGESTING,
                CentrifugeScreen.class, CENTRIFUGING,
                InductionFurnaceScreen.class, RecipeTypes.SMELTING);
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Solenoid.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CrushingRecipeCategory(guiHelper),
                new SeparatingRecipeCategory(guiHelper),
                new ReactingRecipeCategory(guiHelper),
                new DigestingRecipeCategory(guiHelper),
                new CentrifugingRecipeCategory(guiHelper)
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

        registration.addRecipes(REACTING, recipes.byType(SolenoidRecipes.REACTING_TYPE.get())
                .stream().map(RecipeHolder::value).toList());
        registration.addRecipes(DIGESTING, recipes.byType(SolenoidRecipes.DIGESTING_TYPE.get())
                .stream().map(RecipeHolder::value).toList());
        registration.addRecipes(CENTRIFUGING, recipes.byType(SolenoidRecipes.CENTRIFUGING_TYPE.get())
                .stream().map(RecipeHolder::value).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(CRUSHING, EmfBlocks.CRUSHER_ITEM.get());
        registration.addCraftingStation(SEPARATING, EmfBlocks.SEPARATOR_ITEM.get());
        registration.addCraftingStation(REACTING, EmfBlocks.CHEMICAL_REACTOR_ITEM.get());
        registration.addCraftingStation(DIGESTING, EmfBlocks.DIGESTER_ITEM.get());
        registration.addCraftingStation(CENTRIFUGING, EmfBlocks.CENTRIFUGE_ITEM.get());
        registration.addCraftingStation(RecipeTypes.SMELTING, EmfBlocks.INDUCTION_FURNACE_ITEM.get());
    }

    /**
     * Makes each machine's progress arrow a JEI click area: hovering it shows "click to view
     * recipes", clicking opens that machine's recipe category. Coordinates are read straight off the
     * screens so moving an arrow never desyncs the hitbox.
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CrusherScreen.class,
                CrusherScreen.ARROW_X, CrusherScreen.ARROW_Y,
                CrusherScreen.ARROW_W, CrusherScreen.ARROW_H, CRUSHING);
        registration.addRecipeClickArea(SeparatorScreen.class,
                SeparatorScreen.ARROW_X, SeparatorScreen.ARROW_Y,
                SeparatorScreen.ARROW_W, SeparatorScreen.ARROW_H, SEPARATING);
        registration.addRecipeClickArea(ChemicalReactorScreen.class,
                ChemicalReactorScreen.ARROW_X, ChemicalReactorScreen.ARROW_Y,
                ChemicalReactorScreen.ARROW_W, ChemicalReactorScreen.ARROW_H, REACTING);
        registration.addRecipeClickArea(DigesterScreen.class,
                DigesterScreen.ARROW_X, DigesterScreen.ARROW_Y,
                DigesterScreen.ARROW_W, DigesterScreen.ARROW_H, DIGESTING);
        registration.addRecipeClickArea(CentrifugeScreen.class,
                CentrifugeScreen.ARROW_X, CentrifugeScreen.ARROW_Y,
                CentrifugeScreen.ARROW_W, CentrifugeScreen.ARROW_H, CENTRIFUGING);
        registration.addRecipeClickArea(InductionFurnaceScreen.class,
                InductionFurnaceScreen.ARROW_X, InductionFurnaceScreen.ARROW_Y,
                InductionFurnaceScreen.ARROW_W, InductionFurnaceScreen.ARROW_H, RecipeTypes.SMELTING);
    }

    /** Hands the machine screens a way to open JEI without any of them importing a JEI class. */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        IRecipesGui recipesGui = runtime.getRecipesGui();
        Map<Class<?>, IRecipeType<?>> byScreen = recipeTypeByScreen();
        JeiBridge.setOpener(screenClass -> {
            IRecipeType<?> recipeType = byScreen.get(screenClass);
            if (recipeType != null) {
                recipesGui.showTypes(List.of(recipeType));
            }
        });
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiBridge.setOpener(null);
    }
}
