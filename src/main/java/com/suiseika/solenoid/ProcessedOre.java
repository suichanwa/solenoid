package com.suiseika.solenoid;

public enum ProcessedOre {
    MAGNETITE("magnetite", 0x454B52),
    COPPER("copper", 0xC8743C),
    IRON("iron", 0xC6C8CA);

    private final String name;
    private final int color;

    ProcessedOre(String name, int color) {
        this.name = name;
        this.color = color | 0xFF000000; // Ensure 0xFF alpha
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}
