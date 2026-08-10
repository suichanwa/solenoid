package com.suiseika.solenoid.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The header status lamp shared by every machine console.
 *
 * <p>Colour carries the state at a glance; the exact wording comes from the hover tooltip, which
 * keeps the lamp to six pixels and free of any layout collision with a machine's slots.
 */
public enum MachineStatus {
    /** Buffer is empty — the machine cannot do anything. */
    NO_POWER(Theme.BAD, true, "gui.solenoid.status.no_power"),
    /** Actively progressing a recipe. */
    RUNNING(Theme.GOOD, true, "gui.solenoid.status.running"),
    /** Powered, but nothing to do. */
    IDLE(Theme.TEXT_FAINT, false, "gui.solenoid.status.idle"),
    /** Has charge and is pushing it somewhere. */
    CHARGED(Theme.EMF, true, "gui.solenoid.status.charged"),
    /** Generator producing power. */
    GENERATING(Theme.GOOD, true, "gui.solenoid.status.generating"),
    /** Generator out of fuel. */
    DEPLETED(Theme.BAD, true, "gui.solenoid.status.depleted");

    private final int color;
    private final boolean lit;
    private final String key;

    MachineStatus(int color, boolean lit, String key) {
        this.color = color;
        this.lit = lit;
        this.key = key;
    }

    public Component label() {
        return Component.translatable(key);
    }

    /** Classifies a processing machine from the values its menu already syncs. */
    public static MachineStatus of(int energy, int progress, int maxProgress) {
        if (energy <= 0) {
            return NO_POWER;
        }
        return progress > 0 && maxProgress > 0 ? RUNNING : IDLE;
    }

    /**
     * Draws the lamp in the header and, when hovered, its caption as a tooltip.
     *
     * @param x screen-absolute left edge of the panel
     * @param y screen-absolute top edge of the panel
     */
    public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int mouseX, int mouseY) {
        int lampX = x + Theme.STATUS_X;
        int lampY = y + Theme.STATUS_Y;
        Painter.lamp(g, lampX, lampY, Theme.STATUS_SIZE, color, lit);
        if (Painter.hovering(mouseX, mouseY, lampX, lampY, Theme.STATUS_SIZE, Theme.STATUS_SIZE)) {
            g.setTooltipForNextFrame(label(), mouseX, mouseY);
        }
    }

    /** Convenience for the common processing-machine case. */
    public static void draw(GuiGraphicsExtractor g, Font font, int x, int y, int mouseX, int mouseY,
                            int energy, int progress, int maxProgress) {
        of(energy, progress, maxProgress).draw(g, font, x, y, mouseX, mouseY);
    }
}
