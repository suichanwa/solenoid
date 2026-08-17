package com.suiseika.solenoid.energy;

/**
 * Interface implemented by machine menus that support side configuration and auto-ejection.
 */
public interface ISidedMachineMenu {
    MachineSideMode getSideMode(RelativeSide side);
    boolean isAutoEject();

    default boolean supportsSideConfig() {
        return true;
    }
}
