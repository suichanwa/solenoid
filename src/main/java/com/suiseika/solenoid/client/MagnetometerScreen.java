package com.suiseika.solenoid.client;

import com.suiseika.solenoid.MagnetometerItem;
import com.suiseika.solenoid.MagnetometerItems;
import com.suiseika.solenoid.SolenoidDataComponents;
import com.suiseika.solenoid.SolenoidTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Code-drawn radar for the Magnetometer. Scans the CLIENT level (loaded blocks around the player)
 * for blocks in a detection tag, classifies each hit into an {@link OreType} (name + colour), and
 * plots them on a circular radar disc, north up. Painted entirely with {@link GuiGraphicsExtractor}
 * primitives via the deferred render pipeline ({@code extractRenderState}) -- no texture sheet.
 *
 * <p>Detection is tag-driven ({@link SolenoidTags.Blocks#FERROMAGNETIC}); per-hit identity is derived
 * from the block id so a future {@code conductive} tag can plug in another {@link OreType} with no
 * structural change.
 */
public class MagnetometerScreen extends Screen {

    /** A classified anomaly family: display name + blip colour. */
    private record OreType(String name, int color) {}

    private static final OreType MAGNETITE = new OreType("Magnetite", 0xFFFF6A2A);
    private static final OreType IRON = new OreType("Iron", 0xFFB8C6D4);
    private static final OreType UNKNOWN = new OreType("Anomaly", 0xFFE34BFF);

    /** Detection tag(s) scanned. v1: ferromagnetic only. */
    private static final List<TagKey<Block>> TAGS = List.of(SolenoidTags.Blocks.FERROMAGNETIC);

    private static OreType classify(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.contains("magnetite")) return MAGNETITE;
        if (path.contains("iron")) return IRON;
        return UNKNOWN;
    }

    /** One detected block, position relative to the player eye, plus its classified type. */
    private record Blip(double dx, double dy, double dz, OreType type) {
        double horizontal() { return Math.sqrt(dx * dx + dz * dz); }
        double distance() { return Math.sqrt(dx * dx + dy * dy + dz * dz); }
    }

    // Scan box around the player.
    private static final int H_RADIUS = 24;
    private static final int Y_DOWN = 48;
    private static final int Y_UP = 16;
    private static final int RESCAN_TICKS = 20;

    // Radar geometry.
    private static final int RADAR_R = 62;
    private static final int RANGE = H_RADIUS; // world blocks mapped to the radar radius
    private static final int PANEL_W = RADAR_R * 2 + 28;

    // Palette.
    private static final int PANEL_BG = 0xE60B1016;
    private static final int PANEL_EDGE = 0xFF2C3A47;
    private static final int DISC_FACE = 0xFF071019;
    private static final int DISC_EDGE = 0xFF1E3344;
    private static final int RING = 0x443AA6FF;
    private static final int SPOKE = 0x222F6E8F;
    private static final int CROSS = 0xFF5CC8FF;
    private static final int TEXT = 0xFFDCEBFF;
    private static final int TEXT_DIM = 0xFF8197A8;
    private static final int BAR_FRAME = 0xFF05080B;
    private static final int BAR_EMPTY = 0xFF1A2730;
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

    /** Sweeps the loaded client level around the player for blocks in each detection tag. */
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
                    BlockState state = level.getBlockState(cursor);
                    if (!isTarget(state)) {
                        continue;
                    }
                    blips.add(new Blip(
                            cursor.getX() + 0.5 - eye.x,
                            cursor.getY() + 0.5 - eye.y,
                            cursor.getZ() + 0.5 - eye.z,
                            classify(state)));
                }
            }
        }
    }

    private static boolean isTarget(BlockState state) {
        for (TagKey<Block> tag : TAGS) {
            if (state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);

        // ---- Layout (measured top-down so the panel hugs the content) ----
        // Per-type tallies, ordered by first appearance.
        Map<OreType, Integer> counts = new LinkedHashMap<>();
        Blip nearest = null;
        for (Blip blip : blips) {
            if (blip.horizontal() > RANGE) {
                continue;
            }
            counts.merge(blip.type(), 1, Integer::sum);
            if (nearest == null || blip.distance() < nearest.distance()) {
                nearest = blip;
            }
        }

        int cx = this.width / 2;
        int contentLeft = cx - PANEL_W / 2 + 12;
        int top = this.height / 2 - RADAR_R - 34;

        int titleY = top;
        int discCY = top + 14 + RADAR_R;
        int readoutY = discCY + RADAR_R + 8;
        int legendY = readoutY + 22;
        int barY = legendY + counts.size() * 11 + 6;
        int panelBottom = barY + 11 + 6;

        // ---- Panel ----
        int px0 = cx - PANEL_W / 2, px1 = cx + PANEL_W / 2;
        int py0 = titleY - 8, py1 = panelBottom;
        g.fill(px0, py0, px1, py1, PANEL_BG);
        g.fill(px0, py0, px1, py0 + 1, PANEL_EDGE);
        g.fill(px0, py1 - 1, px1, py1, PANEL_EDGE);
        g.fill(px0, py0, px0 + 1, py1, PANEL_EDGE);
        g.fill(px1 - 1, py0, px1, py1, PANEL_EDGE);

        // ---- Title ----
        g.centeredText(this.font, "Magnetometer", cx, titleY, TEXT);

        // ---- Radar disc ----
        fillDisc(g, cx, discCY, RADAR_R + 1, DISC_EDGE);
        fillDisc(g, cx, discCY, RADAR_R, DISC_FACE);
        // Spokes (faint cross through the disc).
        g.fill(cx - RADAR_R, discCY, cx + RADAR_R, discCY + 1, SPOKE);
        g.fill(cx, discCY - RADAR_R, cx + 1, discCY + RADAR_R, SPOKE);
        // Range rings.
        drawRing(g, cx, discCY, RADAR_R / 3, RING);
        drawRing(g, cx, discCY, RADAR_R * 2 / 3, RING);
        drawRing(g, cx, discCY, RADAR_R, RING);
        // Cardinal markers.
        g.centeredText(this.font, "N", cx, discCY - RADAR_R - 1, CROSS);
        g.centeredText(this.font, "S", cx, discCY + RADAR_R - 7, TEXT_DIM);
        g.centeredText(this.font, "E", cx + RADAR_R - 5, discCY - 4, TEXT_DIM);
        g.centeredText(this.font, "W", cx - RADAR_R + 5, discCY - 4, TEXT_DIM);

        // ---- Blips ----
        for (Blip blip : blips) {
            if (blip.horizontal() > RANGE) {
                continue;
            }
            int bx = cx + (int) Math.round(blip.dx / RANGE * RADAR_R);
            int bz = discCY + (int) Math.round(blip.dz / RANGE * RADAR_R);
            double t = Math.min(1.0, blip.distance() / (RANGE * 1.2)); // 0 close .. 1 far
            int color = scaleBrightness(blip.type().color(), 1.0 - 0.65 * t);
            int s = t < 0.33 ? 2 : (t < 0.66 ? 1 : 0); // half-size: closer = bigger
            g.fill(bx - s, bz - s, bx + s + 1, bz + s + 1, color);
        }
        // ---- Direction arrow to the nearest deposit (horizontal bearing, north up) ----
        if (nearest != null && nearest.horizontal() > 0.01) {
            double ux = nearest.dx / nearest.horizontal();
            double uz = nearest.dz / nearest.horizontal();
            drawArrow(g, cx, discCY, ux, uz, RADAR_R * 0.55, nearest.type().color());
        }

        // Player at center.
        g.fill(cx - 1, discCY - 1, cx + 2, discCY + 2, CROSS);

        // ---- Readout ----
        if (nearest != null) {
            int depth = (int) Math.round(-nearest.dy);
            String where = depth == 0 ? "same level" : (depth > 0 ? depth + "m below" : (-depth) + "m above");
            g.text(this.font, "Nearest: " + nearest.type().name(), contentLeft, readoutY, nearest.type().color(), false);
            g.text(this.font, String.format("%.1fm  %s", nearest.distance(), where), contentLeft, readoutY + 10, TEXT_DIM, false);
        } else {
            g.text(this.font, "Nearest: none in range", contentLeft, readoutY, TEXT_DIM, false);
        }

        // ---- Legend (per ore type, with count) ----
        int ly = legendY;
        for (Map.Entry<OreType, Integer> e : counts.entrySet()) {
            OreType type = e.getKey();
            g.fill(contentLeft, ly + 1, contentLeft + 7, ly + 8, type.color());
            g.fill(contentLeft, ly + 1, contentLeft + 7, ly + 2, 0x40FFFFFF);
            g.text(this.font, type.name() + "  x" + e.getValue(), contentLeft + 12, ly, TEXT, false);
            ly += 11;
        }
        if (counts.isEmpty()) {
            g.text(this.font, "No deposits in range", contentLeft, legendY, TEXT_DIM, false);
        }

        // ---- EMF bar ----
        ItemStack device = findDevice();
        int energy = device == null ? 0 : device.getOrDefault(SolenoidDataComponents.EMF_ENERGY.get(), 0);
        int capacity = MagnetometerItem.CAPACITY;
        int barW = PANEL_W - 24;
        int bx0 = contentLeft;
        g.fill(bx0 - 1, barY - 1, bx0 + barW + 1, barY + 9, BAR_FRAME);
        g.fill(bx0, barY, bx0 + barW, barY + 8, BAR_EMPTY);
        if (capacity > 0 && energy > 0) {
            int fillW = (int) ((long) barW * energy / capacity);
            g.fill(bx0, barY, bx0 + fillW, barY + 8, BAR_FILL);
        }
        g.centeredText(this.font, energy + " / " + capacity + " EMF", cx, barY, TEXT);
    }

    /**
     * Draws a thick arrow from {@code (cx,cy)} along unit vector {@code (ux,uz)} of the given length,
     * with a two-barb arrowhead. A bright core is laid over a darker outline so it reads on the disc.
     */
    private static void drawArrow(GuiGraphicsExtractor g, int cx, int cy, double ux, double uz, double length, int color) {
        int outline = 0xFF05080B;
        double tipX = cx + ux * length;
        double tipY = cy + uz * length;
        double perpX = -uz, perpZ = ux;

        // Shaft: outline (width 3) then bright core (width 1).
        plotThickSegment(g, cx, cy, tipX, tipY, perpX, perpZ, 1.5, outline);
        plotThickSegment(g, cx, cy, tipX, tipY, perpX, perpZ, 0.5, color);

        // Arrowhead: two barbs swept back from the tip.
        double ang = Math.atan2(uz, ux);
        double barb = 8.0;
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            double ba = ang + Math.PI + sgn * 0.6;
            double ex = tipX + Math.cos(ba) * barb;
            double ey = tipY + Math.sin(ba) * barb;
            plotThickSegment(g, tipX, tipY, ex, ey, perpX, perpZ, 1.5, outline);
            plotThickSegment(g, tipX, tipY, ex, ey, perpX, perpZ, 0.5, color);
        }
    }

    /** Samples a segment, plotting a perpendicular band of pixels for the given half-thickness. */
    private static void plotThickSegment(GuiGraphicsExtractor g, double x0, double y0, double x1, double y1,
                                         double perpX, double perpZ, double halfWidth, int color) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int steps = Math.max(1, (int) Math.round(len * 2));
        int w = (int) Math.ceil(halfWidth);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = x0 + (x1 - x0) * t;
            double y = y0 + (y1 - y0) * t;
            for (int o = -w; o <= w; o++) {
                if (Math.abs(o) > halfWidth) {
                    continue;
                }
                int px = (int) Math.round(x + perpX * o);
                int py = (int) Math.round(y + perpZ * o);
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    /** Filled disc via per-row horizontal spans. */
    private static void fillDisc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** 1px-thick circle outline by sampling points around the circumference. */
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
