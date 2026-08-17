package com.suiseika.solenoid.energy;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Relative sides of a machine oriented towards its horizontal facing direction.
 */
public enum RelativeSide implements StringRepresentable {
    TOP("top"),
    BOTTOM("bottom"),
    FRONT("front"),
    BACK("back"),
    LEFT("left"),
    RIGHT("right");

    private final String name;

    RelativeSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Component getDisplayName() {
        return Component.translatable("gui.solenoid.side." + name);
    }

    /**
     * Converts a world Direction to a RelativeSide given the machine's horizontal facing.
     */
    public static RelativeSide fromDirection(Direction worldDir, Direction facing) {
        if (worldDir == Direction.UP) return TOP;
        if (worldDir == Direction.DOWN) return BOTTOM;
        if (worldDir == facing) return FRONT;
        if (worldDir == facing.getOpposite()) return BACK;
        if (worldDir == facing.getCounterClockWise()) return LEFT;
        if (worldDir == facing.getClockWise()) return RIGHT;
        return FRONT;
    }

    /**
     * Converts this RelativeSide to a world Direction given the machine's horizontal facing.
     */
    public Direction toDirection(Direction facing) {
        return switch (this) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getCounterClockWise();
            case RIGHT -> facing.getClockWise();
        };
    }
}
