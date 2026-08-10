package com.suiseika.solenoid.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * A small square button drawn in the console theme rather than with the vanilla button sprite,
 * which would look pasted-on against the dark chassis. Renders a single glyph centred in a recessed
 * key that highlights on hover.
 */
public class IconButton extends AbstractButton {
    private final String glyph;
    private final Runnable action;

    public IconButton(int x, int y, int size, String glyph, Component narration, Runnable action) {
        super(x, y, size, size, narration);
        this.glyph = glyph;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hot = isHoveredOrFocused();

        // Raised key: light on top/left, shadow on bottom/right, tinting cyan on hover.
        g.fill(x, y, x + w, y + h, hot ? Theme.BUTTON_HOVER : Theme.CHASSIS);
        g.fill(x, y, x + w, y + 1, Theme.CHASSIS_LIGHT);
        g.fill(x, y, x + 1, y + h, Theme.CHASSIS_LIGHT);
        g.fill(x, y + h - 1, x + w, y + h, hot ? Theme.EMF : Theme.CHASSIS_DARK);
        g.fill(x + w - 1, y, x + w, y + h, hot ? Theme.EMF : Theme.CHASSIS_DARK);

        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textX = x + (w - font.width(glyph)) / 2;
        int textY = y + (h - 8) / 2;
        g.text(font, glyph, textX, textY, Theme.TEXT, false);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
