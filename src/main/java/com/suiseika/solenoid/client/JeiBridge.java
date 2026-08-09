package com.suiseika.solenoid.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Client-side bridge to the optional JEI integration.
 *
 * <p>Deliberately holds no JEI types, so the machine screens can call it whether or not JEI is
 * installed. {@code SolenoidJeiPlugin} installs the opener when the JEI runtime comes up and clears
 * it again when the runtime goes away; with JEI absent the opener stays {@code null} and
 * {@link #recipeButton} returns empty, so the screens simply add no button.
 */
public final class JeiBridge {
    private JeiBridge() {}

    /** Edge length of the square "show recipes" button drawn in each machine GUI. */
    public static final int BUTTON_SIZE = 16;

    /** Opens the JEI recipe GUI for whichever recipe type the given machine screen runs. */
    @FunctionalInterface
    public interface RecipeOpener {
        void showRecipes(Class<?> machineScreenClass);
    }

    private static volatile @Nullable RecipeOpener opener;

    /** Called by the JEI plugin. {@code null} clears the binding when the runtime goes away. */
    public static void setOpener(@Nullable RecipeOpener value) {
        opener = value;
    }

    public static boolean isAvailable() {
        return opener != null;
    }

    /**
     * Builds the "?" button that lists every recipe the machine can run.
     *
     * @param machineScreenClass the screen's own class; the JEI plugin maps it to a recipe type
     * @return empty when JEI is not present
     */
    public static Optional<Button> recipeButton(int x, int y, Class<?> machineScreenClass) {
        if (opener == null) {
            return Optional.empty();
        }
        return Optional.of(Button.builder(Component.literal("?"), button -> {
                    // Re-read the field: JEI may have shut down between init and the click.
                    RecipeOpener live = opener;
                    if (live != null) {
                        live.showRecipes(machineScreenClass);
                    }
                })
                .bounds(x, y, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("gui.solenoid.show_recipes")))
                .build());
    }
}
