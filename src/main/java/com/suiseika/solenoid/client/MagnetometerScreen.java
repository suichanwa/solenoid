package com.suiseika.solenoid.client;

import com.suiseika.solenoid.MagnetometerItem;
import com.suiseika.solenoid.MagnetometerItems;
import com.suiseika.solenoid.SolenoidDataComponents;
import com.suiseika.solenoid.SolenoidTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Code-drawn radar for the Magnetometer. Scans the CLIENT level (loaded blocks around the player)
 * for blocks in a detection tag and plots them on a dark radar disc, north up. Everything is painted
 * with {@link GuiGraphicsExtractor#fill}/{@code text} primitives via the deferred render pipeline
 * ({@code extractRenderState}) -- no texture sheet.
 *
 * <p>Detection is generic over {@link DetectionMode} (tag + blip colour + label). v1 ships a single
 * ferromagnetic mode; a future EM-induction device adds a second mode with no structural change.
 */
public class MagnetometerScreen extends Screen {
    /** A detectable family: which block tag, what colour to plot it, and a human label. */
    private record DetectionMode(TagKey<Block> tag, int color, String label) {}

    private static final List<DetectionMode> MODES = List.of(
            new DetectionMode(SolenoidTags.Blocks.FERROMAGNETIC, 0xFFFF5A1E, "Ferromagnetic")
            // Future: new DetectionMode(SolenoidTags.Blocks.CONDUCTIVE, 0xFF35C7FF, "Conductive")
    );

    /** One detected block, position relative to the player eye, plus which mode found it. */
    private record Blip(double dx, double dy, double dz, int color) {
        double horizontal() { return Math.sqrt(dx * dx + dz * dz); }
        double distance() { return Math.sqrt(dx * dx + dy * dy + dz * dz); }
    }

    // Scan box around the player.
    private static final int H_RADIUS = 24;
    private static final int Y_DOWN = 48;
    private static final int Y_UP = 16;
    private static final int RESCAN_TICKS = 20;

    // Radar geometry.
    private static final int RADAR_PX = 150;
    private static final int RANGE = H_RADIUS; // world blocks mapped to the radar radius

    // Palette.
    private static final int PANEL_BG = 0xE6101418;
    private static final int PANEL_EDGE = 0xFF3A4450;
    private static final int RADAR_FACE = 0xFF06120A;
    private static final int GRID = 0x223AA6FF;
    private static final int RING = 0x553AA6FF;
    private static final int CROSS = 0xFF5CC8FF;
    private static final int TEXT = 0xFFCFE7FF;
    private static final int TEXT_DIM = 0xFF7E93A6;
    private static final int BAR_FRAME = 0xFF000000;
    private static final int BAR_EMPTY = 0xFF20303A;
    private static final int BAR_FILL = 0xFF3AA6FF;

    private final List<Blip> blips = new ArrayList<>();
    private int sinceScan = RESCAN_TICKS;

    public MagnetometerScreen() {
        super(Component.translatable("item.solenoid.magnetometer"));
    }

    /** Client-side entry point used by {@link MagnetometerItem} on use. */
    public static void open() {
        Minecraft.getInstance().setScreen(new MagnetometerScreen());
    }

    @Override
    protected void init() {
        sinceScan = RESCAN_TICKS; // force a scan on the first tick
    }

    @Override
    public boolean isPauseScreen() {
        return false; // keep the world ticking so re-scans stay current
    }

    @Override
    public void tick() {
        if (++sinceScan >= RESCAN_TICKS) {
            scan();
            sinceScan = 0;
        }
    }

    /** Sweeps the loaded client level around the player for blocks in each detection mode's tag. */
    private void scan() {
        blips.clear();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        Level level = minecraft.level;
        BlockPos center = minecraft.player.blockPosition();
        Vec3 eye = minecraft.player.getEyePosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -H_RADIUS; dx <= H_RADIUS; dx++) {
            for (int dz = -H_RADIUS; dz <= H_RADIUS; dz++) {
                if (dx * dx + dz * dz > H_RADIUS * H_RADIUS) {
                    continue; // circular horizontal range
                }
                for (int dy = -Y_DOWN; dy <= Y_UP; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var state = level.getBlockState(cursor);
                    for (DetectionMode mode : MODES) {
                        if (state.is(mode.tag())) {
                            blips.add(new Blip(
                                    cursor.getX() + 0.5 - eye.x,
                                    cursor.getY() + 0.5 - eye.y,
                                    cursor.getZ() + 0.5 - eye.z,
                                    mode.color()));
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);

        int cx = this.width / 2;
        int cy = this.height / 2 - 8;
        int half = RADAR_PX / 2;
        int x0 = cx - half, y0 = cy - half, x1 = cx + half, y1 = cy + half;

        // Panel + radar face.
        g.fill(x0 - 6, y0 - 20, x1 + 6, y1 + 58, PANEL_BG);
        g.fill(x0 - 6, y0 - 20, x1 + 6, y0 - 19, PANEL_EDGE);
        g.fill(x0 - 6, y1 + 57, x1 + 6, y1 + 58, PANEL_EDGE);
        g.fill(x0 - 6, y0 - 20, x0 - 5, y1 + 58, PANEL_EDGE);
        g.fill(x1 + 5, y0 - 20, x1 + 6, y1 + 58, PANEL_EDGE);
        g.fill(x0, y0, x1, y1, RADAR_FACE);

        // Grid lines.
        for (int i = 1; i < 6; i++) {
            int gx = x0 + RADAR_PX * i / 6;
            int gy = y0 + RADAR_PX * i / 6;
            g.fill(gx, y0, gx + 1, y1, GRID);
            g.fill(x0, gy, x1, gy + 1, GRID);
        }

        // Concentric range rings (1/3, 2/3, full).
        drawRing(g, cx, cy, half / 3, RING);
        drawRing(g, cx, cy, half * 2 / 3, RING);
        drawRing(g, cx, cy, half, RING);

        // Center crosshair (player).
        g.fill(cx - 4, cy, cx + 5, cy + 1, CROSS);
        g.fill(cx, cy - 4, cx + 1, cy + 5, CROSS);

        // North marker.
        g.centeredText(this.font, "N", cx, y0 - 11, CROSS);

        // Blips.
        Blip nearest = null;
        for (Blip blip : blips) {
            if (blip.horizontal() > RANGE) {
                continue;
            }
            int bx = cx + (int) Math.round(blip.dx / RANGE * half);
            int bz = cy + (int) Math.round(blip.dz / RANGE * half);
            double t = Math.min(1.0, blip.distance() / (RANGE * 1.2)); // 0 close .. 1 far
            int color = scaleBrightness(blip.color(), 1.0 - 0.7 * t);
            int s = t < 0.33 ? 2 : (t < 0.66 ? 1 : 0); // half-size: closer = bigger
            g.fill(bx - s, bz - s, bx + s + 1, bz + s + 1, color);
            if (nearest == null || blip.distance() < nearest.distance()) {
                nearest = blip;
            }
        }

        // ---- Text readout ----
        int tx = x0 - 4;
        int ty = y1 + 4;
        g.text(this.font, "Magnetometer", tx, ty, TEXT, false);
        ty += 11;

        if (nearest != null) {
            int depth = (int) Math.round(-nearest.dy); // positive = below player
            String depthStr = depth == 0 ? "level" : (depth > 0 ? depth + "m below" : (-depth) + "m above");
            g.text(this.font, String.format("Nearest: %.1fm (%s)", nearest.distance(), depthStr), tx, ty, TEXT_DIM, false);
        } else {
            g.text(this.font, "Nearest: none in range", tx, ty, TEXT_DIM, false);
        }
        ty += 11;
        g.text(this.font, "Anomalies: " + blips.size(), tx, ty, TEXT_DIM, false);
        ty += 13;

        // EMF bar.
        ItemStack device = findDevice();
        int energy = device == null ? 0 : device.getOrDefault(SolenoidDataComponents.EMF_ENERGY.get(), 0);
        int capacity = MagnetometerItem.CAPACITY;
        int barW = RADAR_PX + 8;
        int bx0 = tx;
        g.fill(bx0 - 1, ty - 1, bx0 + barW + 1, ty + 9, BAR_FRAME);
        g.fill(bx0, ty, bx0 + barW, ty + 8, BAR_EMPTY);
        if (capacity > 0 && energy > 0) {
            int fillW = (int) ((long) barW * energy / capacity);
            g.fill(bx0, ty, bx0 + fillW, ty + 8, BAR_FILL);
        }
        g.text(this.font, energy + " / " + capacity + " EMF", bx0 + 2, ty, TEXT, false);
    }

    /** Plots a 1px-thick circle outline by sampling points around the circumference. */
    private static void drawRing(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        if (r <= 0) {
            return;
        }
        int steps = Math.max(24, r * 4);
        for (int i = 0; i < steps; i++) {
            double ang = (Math.PI * 2 * i) / steps;
            int px = cx + (int) Math.round(Math.cos(ang) * r);
            int py = cy + (int) Math.round(Math.sin(ang) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** Multiplies an ARGB colour's RGB channels by {@code factor} (alpha preserved). */
    private static int scaleBrightness(int argb, double factor) {
        factor = Math.max(0.0, Math.min(1.0, factor));
        int aa = (argb >>> 24) & 0xFF;
        int rr = (int) (((argb >> 16) & 0xFF) * factor);
        int gg = (int) (((argb >> 8) & 0xFF) * factor);
        int bb = (int) ((argb & 0xFF) * factor);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    /** Finds the magnetometer in either hand so the bar shows the live, synced energy. */
    private ItemStack findDevice() {
        Player player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return null;
        }
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (stack.is(MagnetometerItems.MAGNETOMETER.get())) {
                return stack;
            }
        }
        return null;
    }
}
