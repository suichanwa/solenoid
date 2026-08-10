package com.suiseika.solenoid.client;

import org.jetbrains.annotations.Nullable;

/**
 * Client-side bridge to the optional JEI integration.
 *
 * <p>Deliberately holds no JEI types, so the machine screens can call it whether or not JEI is
 * installed. {@code SolenoidJeiPlugin} installs the opener when the JEI runtime comes up and clears
 * it again when the runtime goes away; with JEI absent the opener stays {@code null},
 * {@link #isAvailable()} reports false and the screens add no recipe button.
 */
public final class JeiBridge {
    private JeiBridge() {}

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
     * Shows every recipe the given machine screen's machine can run. No-op when JEI is absent or
     * has shut down between the button being built and clicked.
     */
    public static void show(Class<?> machineScreenClass) {
        RecipeOpener live = opener;
        if (live != null) {
            live.showRecipes(machineScreenClass);
        }
    }
}
