package com.suiseika.solenoid.energy;

import net.minecraft.util.StringRepresentable;

public enum TubeMode implements StringRepresentable {
    INSERT("insert"),
    EXTRACT("extract"),
    DISCONNECT("disconnect");

    private final String name;

    TubeMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public TubeMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
