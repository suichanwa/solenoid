package com.suiseika.solenoid.client.ui;

import com.suiseika.solenoid.client.JeiBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared chassis for every Solenoid machine GUI.
 *
 * <p>Paints the console frame — header, machine well, player-inventory well, slot recesses — and
 * owns the pieces every machine has in common: the EMF gauge with its rich tooltip, the status lamp,
 * and the JEI "show recipes" button. Subclasses supply only what makes their machine different, via
 * {@link #drawMachine} and the small set of hooks below.
 *
 * <p>Coordinates passed to the hooks are screen-absolute: {@link #left()} and {@link #top()} have
 * already been applied by the caller where noted, otherwise add them yourself.
 */
public abstract class MachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected MachineScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Theme.WIDTH, Theme.HEIGHT);
    }

    // ---- Hooks ---------------------------------------------------------------------------------

    /** Current EMF in the machine's buffer. */
    protected abstract int energyStored();

    /** Size of the machine's EMF buffer. */
    protected abstract int energyMax();

    /**
     * Machine-specific painting: progress arrows, extra gauges, readouts. Called after the chassis,
     * wells and slots are down, and before tooltips are resolved.
     *
     * @param x screen-absolute left edge of the panel
     * @param y screen-absolute top edge of the panel
     */
    protected abstract void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY);

    /** Recipe-type key handed to JEI by the "?" button, or empty for machines with no recipes. */
    protected Optional<Class<?>> recipeScreenKey() {
        return Optional.of(getClass());
    }

    /** Whether this machine shows the standard vertical EMF gauge. Machines with a bespoke energy
     *  readout (Capacitor, RTG) turn this off and paint their own. */
    protected boolean showsEnergyBar() {
        return true;
    }

    /** Slot indices that should get an output accent ring, in this menu's own slot numbering. */
    protected int[] outputSlots() {
        return new int[0];
    }

    /** Extra lines appended to the EMF gauge tooltip. */
    protected void appendEnergyTooltip(List<Component> lines) {
    }

    // ---- Convenience ---------------------------------------------------------------------------

    protected final int left() {
        return this.leftPos;
    }

    protected final int top() {
        return this.topPos;
    }

    // ---- Lifecycle -----------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        recipeScreenKey().ifPresent(key -> {
            if (!JeiBridge.isAvailable()) {
                return;
            }
            addRenderableWidget(new IconButton(
                    leftPos + Theme.RECIPE_BUTTON_X,
                    topPos + Theme.RECIPE_BUTTON_Y,
                    Theme.RECIPE_BUTTON_SIZE,
                    "?",
                    Component.translatable("gui.solenoid.show_recipes"),
                    () -> JeiBridge.show(key)));
        });
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.leftPos, y = this.topPos;

        Painter.chassis(g, x, y, this.imageWidth, this.imageHeight);
        Painter.header(g, x, y, this.imageWidth);
        Painter.well(g, x + Theme.MACHINE_X, y + Theme.MACHINE_Y, Theme.MACHINE_W, Theme.MACHINE_H);
        Painter.well(g, x + Theme.PLAYER_X, y + Theme.PLAYER_Y, Theme.PLAYER_W, Theme.PLAYER_H);

        for (Slot slot : this.menu.slots) {
            Painter.slot(g, x + slot.x, y + slot.y);
        }
        for (int index : outputSlots()) {
            if (index >= 0 && index < this.menu.slots.size()) {
                Slot slot = this.menu.slots.get(index);
                Painter.outputRing(g, x + slot.x, y + slot.y);
            }
        }

        if (showsEnergyBar()) {
            Painter.energyBar(g, x + Theme.ENERGY_X, y + Theme.ENERGY_Y,
                    Theme.ENERGY_W, Theme.ENERGY_H, energyStored(), energyMax());
        }

        drawMachine(g, x, y, mouseX, mouseY);

        if (showsEnergyBar()
                && Painter.hovering(mouseX, mouseY, x + Theme.ENERGY_X, y + Theme.ENERGY_Y,
                        Theme.ENERGY_W, Theme.ENERGY_H)) {
            energyTooltip(g, mouseX, mouseY);
        }
    }

    /** Multi-line EMF tooltip: label, exact figures, percentage, plus any machine-specific lines. */
    protected void energyTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int stored = energyStored();
        int max = energyMax();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.solenoid.energy").withStyle(net.minecraft.ChatFormatting.WHITE));
        lines.add(Component.literal(Painter.number(stored) + " / " + Painter.number(max) + " EMF")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.literal(Painter.percent(stored, max) + "%")
                .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        appendEnergyTooltip(lines);
        g.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
    }

    /**
     * Title in console colours. The vanilla implementation draws near-black text, which is
     * unreadable on the dark chassis; the "Inventory" caption is dropped because the recessed well
     * already separates the player inventory visually.
     *
     * <p>The title is clipped to the space before the status lamp so a long machine name can never
     * run through the lamp and the recipe button.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, clipToWidth(this.title.getString(), Theme.STATUS_X - 8 - 4), 8, 6, Theme.TEXT, false);
    }

    /** Trims {@code text} with an ellipsis until it fits {@code maxWidth} pixels. */
    private String clipToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (!trimmed.isEmpty() && this.font.width(trimmed + "…") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "…";
    }
}
