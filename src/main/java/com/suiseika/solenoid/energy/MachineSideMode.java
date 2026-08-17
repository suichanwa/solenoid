package com.suiseika.solenoid.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Sided I/O mode for Solenoid machines.
 */
public enum MachineSideMode implements StringRepresentable {
    BOTH("both", 0xFF5FD16A, ChatFormatting.GREEN),         // Input & Output
    INPUT("input", 0xFF38C7E8, ChatFormatting.AQUA),        // Input only
    OUTPUT("output", 0xFFE8A33A, ChatFormatting.GOLD),      // Output only
    DISABLED("disabled", 0xFF555555, ChatFormatting.GRAY);  // Disconnected / closed

    private final String name;
    private final int color;
    private final ChatFormatting formatting;

    MachineSideMode(String name, int color, ChatFormatting formatting) {
        this.name = name;
        this.color = color;
        this.formatting = formatting;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getColor() {
        return this.color;
    }

    public ChatFormatting getFormatting() {
        return this.formatting;
    }

    public Component getDisplayName() {
        return Component.translatable("gui.solenoid.side_mode." + name).withStyle(formatting);
    }

    public MachineSideMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
