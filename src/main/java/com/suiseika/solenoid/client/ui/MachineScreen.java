package com.suiseika.solenoid.client.ui;

import com.suiseika.solenoid.client.JeiBridge;
import com.suiseika.solenoid.energy.ISidedMachineMenu;
import com.suiseika.solenoid.energy.MachineSideMode;
import com.suiseika.solenoid.energy.RelativeSide;
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
 * the JEI "show recipes" button, and the side IO configuration panel.
 */
public abstract class MachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected boolean showSideConfig = false;

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
     */
    protected abstract void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY);

    /** Recipe-type key handed to JEI by the "?" button, or empty for machines with no recipes. */
    protected Optional<Class<?>> recipeScreenKey() {
        return Optional.of(getClass());
    }

    /** Whether this machine shows the standard vertical EMF gauge. */
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

        // 1. JEI Recipe button ("?")
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

        // 2. Side Configuration button ("⇄")
        if (this.menu instanceof ISidedMachineMenu sidedMenu && sidedMenu.supportsSideConfig()) {
            addRenderableWidget(new IconButton(
                    leftPos + Theme.IO_BUTTON_X,
                    topPos + Theme.IO_BUTTON_Y,
                    Theme.IO_BUTTON_SIZE,
                    "⇄",
                    Component.translatable("gui.solenoid.side_config"),
                    () -> this.showSideConfig = !this.showSideConfig));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.leftPos, y = this.topPos;

        Painter.chassis(g, x, y, this.imageWidth, this.imageHeight);
        Painter.header(g, x, y, this.imageWidth);
        Painter.well(g, x + Theme.MACHINE_X, y + Theme.MACHINE_Y, Theme.MACHINE_W, Theme.MACHINE_H);
        Painter.well(g, x + Theme.PLAYER_X, y + Theme.PLAYER_Y, Theme.PLAYER_W, Theme.PLAYER_H);

        if (this.showSideConfig && this.menu instanceof ISidedMachineMenu sidedMenu) {
            // Mode 1: Side I/O Configuration GUI
            // Only draw player inventory slots in background
            for (Slot slot : this.menu.slots) {
                if (slot.y >= Theme.PLAYER_Y) {
                    Painter.slot(g, x + slot.x, y + slot.y);
                }
            }

            drawSideConfigOverlay(g, mouseX, mouseY, sidedMenu);
        } else {
            // Mode 2: Standard Machine Console GUI
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
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY) {
        if (this.showSideConfig && slot.y < Theme.PLAYER_Y) {
            return;
        }
        super.extractSlot(g, slot, mouseX, mouseY);
    }

    private void drawSideConfigOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY, ISidedMachineMenu sidedMenu) {
        int ox = leftPos + Theme.MACHINE_X;
        int oy = topPos + Theme.MACHINE_Y;
        int ow = Theme.MACHINE_W;
        int oh = Theme.MACHINE_H;

        // Dark backdrop & cyan accent frame
        g.fill(ox, oy, ox + ow, oy + oh, 0xF51E2024);
        g.fill(ox, oy, ox + ow, oy + 1, Theme.EMF);
        g.fill(ox, oy + oh - 1, ox + ow, oy + oh, Theme.EMF);
        g.fill(ox, oy, ox + 1, oy + oh, Theme.EMF);
        g.fill(ox + ow - 1, oy, ox + ow, oy + oh, Theme.EMF);

        // Header Title
        g.text(this.font, Component.translatable("gui.solenoid.side_config.title").withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD),
                ox + 6, oy + 4, 0xFFFFFFFF, false);

        // Auto-Eject button
        boolean ejectHover = mouseX >= ox + 82 && mouseX <= ox + 146 && mouseY >= oy + 2 && mouseY <= oy + 13;
        boolean autoEject = sidedMenu.isAutoEject();
        int ejectBg = autoEject ? (ejectHover ? 0xFF35633D : 0xFF24482B) : (ejectHover ? 0xFF454545 : 0xFF303030);
        int ejectBorder = autoEject ? Theme.GOOD : Theme.TEXT_DIM;
        g.fill(ox + 82, oy + 3, ox + 146, oy + 13, ejectBg);
        g.fill(ox + 82, oy + 3, ox + 146, oy + 4, ejectBorder);
        g.fill(ox + 82, oy + 12, ox + 146, oy + 13, ejectBorder);
        g.fill(ox + 82, oy + 3, ox + 83, oy + 13, ejectBorder);
        g.fill(ox + 145, oy + 3, ox + 146, oy + 13, ejectBorder);

        String ejectText = autoEject ? "EJECT: ON" : "EJECT: OFF";
        int ejectColor = autoEject ? 0xFF7DFF8B : 0xFFAAAAAA;
        int etw = this.font.width(ejectText);
        g.text(this.font, ejectText, ox + 82 + (64 - etw) / 2, oy + 4, ejectColor, false);

        // Close [✕] button
        boolean closeHover = mouseX >= ox + ow - 14 && mouseX <= ox + ow - 2 && mouseY >= oy + 2 && mouseY <= oy + 13;
        g.fill(ox + ow - 14, oy + 3, ox + ow - 3, oy + 13, closeHover ? 0xFF772222 : 0xFF353535);
        g.text(this.font, "✕", ox + ow - 11, oy + 4, closeHover ? 0xFFFF7777 : 0xFFBBBBBB, false);

        // 6 Side Buttons
        int[][] buttonPositions = {
                {ox + 4, oy + 16},   // 0: TOP
                {ox + 4, oy + 37},   // 1: BOTTOM
                {ox + 58, oy + 16},  // 2: FRONT
                {ox + 58, oy + 37},  // 3: BACK
                {ox + 112, oy + 16}, // 4: LEFT
                {ox + 112, oy + 37}  // 5: RIGHT
        };

        RelativeSide[] sides = RelativeSide.values();
        RelativeSide hoveredSide = null;
        MachineSideMode hoveredMode = null;

        for (int i = 0; i < 6; i++) {
            int bx = buttonPositions[i][0];
            int by = buttonPositions[i][1];
            RelativeSide side = sides[i];
            MachineSideMode mode = sidedMenu.getSideMode(side);

            boolean btnHover = mouseX >= bx && mouseX <= bx + 48 && mouseY >= by && mouseY <= by + 18;
            if (btnHover) {
                hoveredSide = side;
                hoveredMode = mode;
            }

            int btnBg = btnHover ? 0xFF2C323B : 0xFF22262C;
            g.fill(bx, by, bx + 48, by + 18, btnBg);

            // Colored mode accent frame
            int modeColor = mode.getColor();
            g.fill(bx, by, bx + 48, by + 1, modeColor);
            g.fill(bx, by + 17, bx + 48, by + 18, modeColor);
            g.fill(bx, by, bx + 1, by + 18, modeColor);
            g.fill(bx + 47, by, bx + 48, by + 18, modeColor);

            // Side name
            g.text(this.font, side.name(), bx + 3, by + 3, 0xFFC0C0C0, false);

            // Mode badge text
            String modeStr = mode.name();
            g.text(this.font, modeStr, bx + 3, by + 10, modeColor, false);
        }

        // Tooltips
        if (ejectHover) {
            List<Component> tooltip = List.of(
                    Component.translatable("gui.solenoid.side_config.auto_eject")
                            .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD),
                    Component.translatable("gui.solenoid.side_config.auto_eject.desc")
                            .withStyle(net.minecraft.ChatFormatting.GRAY)
            );
            g.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
        } else if (hoveredSide != null && hoveredMode != null) {
            List<Component> tooltip = List.of(
                    Component.literal(hoveredSide.name() + ": ").withStyle(net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.BOLD)
                            .append(hoveredMode.getDisplayName()),
                    Component.translatable("gui.solenoid.side_config.cycle_hint")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
            );
            g.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isHovered) {
        if (this.showSideConfig && this.menu instanceof ISidedMachineMenu) {
            double mouseX = event.x();
            double mouseY = event.y();
            int ox = leftPos + Theme.MACHINE_X;
            int oy = topPos + Theme.MACHINE_Y;
            int ow = Theme.MACHINE_W;
            int oh = Theme.MACHINE_H;

            // 1. Close button
            if (mouseX >= ox + ow - 14 && mouseX <= ox + ow - 2 && mouseY >= oy + 2 && mouseY <= oy + 13) {
                this.showSideConfig = false;
                playClickSound();
                return true;
            }

            // 2. Auto-Eject button
            if (mouseX >= ox + 82 && mouseX <= ox + 146 && mouseY >= oy + 2 && mouseY <= oy + 13) {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 6);
                }
                playClickSound();
                return true;
            }

            // 3. Six side buttons
            int[][] buttonPositions = {
                    {ox + 4, oy + 16},   // 0: TOP
                    {ox + 4, oy + 37},   // 1: BOTTOM
                    {ox + 58, oy + 16},  // 2: FRONT
                    {ox + 58, oy + 37},  // 3: BACK
                    {ox + 112, oy + 16}, // 4: LEFT
                    {ox + 112, oy + 37}  // 5: RIGHT
            };

            for (int i = 0; i < 6; i++) {
                int bx = buttonPositions[i][0];
                int by = buttonPositions[i][1];
                if (mouseX >= bx && mouseX <= bx + 48 && mouseY >= by && mouseY <= by + 18) {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                    }
                    playClickSound();
                    return true;
                }
            }

            // If clicked within the overlay, consume the click so slots aren't triggered
            if (mouseX >= ox && mouseX <= ox + ow && mouseY >= oy && mouseY <= oy + oh) {
                return true;
            }
        }

        return super.mouseClicked(event, isHovered);
    }

    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
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

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (this.showSideConfig) {
            g.text(this.font, Component.translatable("gui.solenoid.side_config.title"), 8, 6, Theme.EMF, false);
        } else {
            g.text(this.font, clipToWidth(this.title.getString(), Theme.STATUS_X - 8 - 4), 8, 6, Theme.TEXT, false);
        }
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
