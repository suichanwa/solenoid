package com.suiseika.solenoid.client.ui;

/**
 * The Solenoid console palette and layout grid.
 *
 * <p>Every machine GUI in the mod is drawn from these constants, so a colour or a metric only ever
 * needs changing in one place. The panel itself stays the neutral vanilla grey so the GUIs sit
 * alongside the rest of the game's interface; colour is reserved for the things that carry meaning —
 * the cyan EMF gauge, the amber progress fill, and the status lamps.
 *
 * <p>All colours are ARGB. All metrics are GUI-relative (0,0 = top-left of the 176x166 panel).
 */
public final class Theme {
    private Theme() {}

    // ---- Chassis -------------------------------------------------------------------------------

    /** Main body of the panel: vanilla container grey. */
    public static final int CHASSIS = 0xFFC6C6C6;
    /** Top/left bevel highlight on the chassis. */
    public static final int CHASSIS_LIGHT = 0xFFFFFFFF;
    /** Bottom/right bevel shadow on the chassis. */
    public static final int CHASSIS_DARK = 0xFF555555;
    /** Header strip behind the machine title. Same tone as the chassis: the divider alone separates it. */
    public static final int HEADER = 0xFFC6C6C6;
    /** Hairline under the header and between sections. */
    public static final int DIVIDER = 0xFF8B8B8B;

    // ---- Recessed wells ------------------------------------------------------------------------

    /** Interior of a recessed content well. Matches the chassis; only the bevel marks it as sunken. */
    public static final int WELL = 0xFFC6C6C6;
    /** Inner shadow along a well's top/left edge. */
    public static final int WELL_SHADOW = 0xFF8B8B8B;
    /** Inner highlight along a well's bottom/right edge. */
    public static final int WELL_HIGHLIGHT = 0xFFFFFFFF;

    // ---- Slots ---------------------------------------------------------------------------------

    /** Interior of an item slot: vanilla slot grey. */
    public static final int SLOT = 0xFF8B8B8B;
    /** Slot top/left inner shadow. */
    public static final int SLOT_SHADOW = 0xFF373737;
    /** Slot bottom/right inner highlight. */
    public static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;
    /** Accent ring drawn around a machine's output slot. */
    public static final int SLOT_OUTPUT_RING = 0xFF5A8C99;

    // ---- Accents -------------------------------------------------------------------------------

    /** EMF cyan: energy bars, active flow. */
    public static final int EMF = 0xFF38C7E8;
    /** Brighter cyan for the top edge of a filled energy bar. */
    public static final int EMF_BRIGHT = 0xFF8FEBFF;
    /** Unfilled remainder of a gauge track: a dark recess, so the fill reads against it. */
    public static final int EMF_TRACK = 0xFF555555;
    /** Specular pip on a lit lamp. Always white, independent of the panel tone. */
    public static final int LAMP_GLINT = 0xFFFFFFFF;
    /** Progress fill. */
    public static final int PROGRESS = 0xFFE8A33A;
    /** Brighter amber for the leading edge of the progress fill. */
    public static final int PROGRESS_BRIGHT = 0xFFFFD07A;
    /** Healthy / running. */
    public static final int GOOD = 0xFF5FD16A;
    /** Fault / no power. */
    public static final int BAD = 0xFFE05A4A;
    /** Radioactive fuel green. */
    public static final int FUEL = 0xFF7BD44E;
    /** Brighter green for the top edge of a filled fuel bar. */
    public static final int FUEL_BRIGHT = 0xFFC2F59B;
    /** Fill behind a hovered icon button. */
    public static final int BUTTON_HOVER = 0xFFD8EEF4;
    /** 1px frame drawn around gauges and lamps, dark enough to define them against the grey panel. */
    public static final int BAR_FRAME = 0xFF373737;

    // ---- Text ----------------------------------------------------------------------------------

    /** Titles and primary readouts: vanilla container label grey. */
    public static final int TEXT = 0xFF404040;
    /** Labels and secondary readouts. */
    public static final int TEXT_DIM = 0xFF6A6A6A;
    /** Faint captions and unlit lamps. */
    public static final int TEXT_FAINT = 0xFF8B8B8B;

    // ---- Layout grid ---------------------------------------------------------------------------

    /** Standard container width. Matches the vanilla player-inventory alignment. */
    public static final int WIDTH = 176;
    /** Standard container height. */
    public static final int HEIGHT = 166;

    /** Height of the title strip at the top of the panel. */
    public static final int HEADER_H = 20;

    /** Recessed well holding the machine's own controls and slots. */
    public static final int MACHINE_X = 6, MACHINE_Y = 22, MACHINE_W = 164, MACHINE_H = 60;
    /** Recessed well holding the player inventory and hotbar. */
    public static final int PLAYER_X = 6, PLAYER_Y = 82, PLAYER_W = 164, PLAYER_H = 80;

    /** Vertical EMF bar, standard position inside the machine well. */
    public static final int ENERGY_X = 12, ENERGY_Y = 26, ENERGY_W = 12, ENERGY_H = 52;

    /** Square "show recipes" button, tucked into the header's right edge. */
    public static final int RECIPE_BUTTON_X = 156, RECIPE_BUTTON_Y = 3, RECIPE_BUTTON_SIZE = 14;

    /** Square "side configuration" button, sitting immediately left of the recipe button. */
    public static final int IO_BUTTON_X = 140, IO_BUTTON_Y = 3, IO_BUTTON_SIZE = 14;

    /**
     * Status lamp, sitting in the header just left of the IO button. The header is the one strip
     * every machine keeps free regardless of its slot layout, so the lamp never collides.
     */
    public static final int STATUS_X = 129, STATUS_Y = 7, STATUS_SIZE = 6;
}
