package com.suiseika.solenoid.client;

import com.suiseika.solenoid.MagnetometerItem;
import com.suiseika.solenoid.SolenoidDataComponents;
import com.suiseika.solenoid.SolenoidTags;
import com.suiseika.solenoid.InductionSurveyorItem;
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

public class MagnetometerScreen extends Screen {

    private record OreType(String name, int color) {}

    private static final OreType MAGNETITE = new OreType("Magnetite", 0xFFFF6A2A);
    private static final OreType IRON = new OreType("Iron", 0xFFB8C6D4);
    
    private static final OreType COPPER = new OreType("Copper", 0xFFE0734D);
    private static final OreType GOLD = new OreType("Gold", 0xFFFCEE4B);
    private static final OreType LAPIS = new OreType("Lapis", 0xFF1044A5);
    private static final OreType REDSTONE = new OreType("Redstone", 0xFFFF0000);
    
    private static final OreType UNKNOWN = new OreType("Anomaly", 0xFFE34BFF);

    private enum DetectionMode {
        MAGNETIC("Magnetometer", List.of(SolenoidTags.Blocks.FERROMAGNETIC)),
        INDUCTION("Induction Surveyor", List.of(SolenoidTags.Blocks.CONDUCTIVE));

        final String title;
        final List<TagKey<Block>> tags;

        DetectionMode(String title, List<TagKey<Block>> tags) {
            this.title = title;
            this.tags = tags;
        }
    }

    private static OreType classify(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.contains("magnetite")) return MAGNETITE;
        if (path.contains("iron")) return IRON;
        if (path.contains("copper")) return COPPER;
        if (path.contains("gold")) return GOLD;
        if (path.contains("lapis")) return LAPIS;
        if (path.contains("redstone")) return REDSTONE;
        return UNKNOWN;
    }

    private record Blip(double dx, double dy, double dz, OreType type) {
        double horizontal() { return Math.sqrt(dx * dx + dz * dz); }
        double distance() { return Math.sqrt(dx * dx + dy * dy + dz * dz); }
    }

    private static final int H_RADIUS = 24;
    private static final int Y_DOWN = 48;
    private static final int Y_UP = 16;
    private static final int RESCAN_TICKS = 20;

    private static final int RADAR_R = 62;
    private static final int RANGE = H_RADIUS;
    private static final int PANEL_W = RADAR_R * 2 + 28;

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
    private final DetectionMode mode;

    public MagnetometerScreen() {
        super(Component.literal("Scanner"));
        
        Player player = Minecraft.getInstance().player;
        ItemStack main = player != null ? player.getMainHandItem() : ItemStack.EMPTY;
        ItemStack off = player != null ? player.getOffhandItem() : ItemStack.EMPTY;
        
        if (main.getItem() instanceof InductionSurveyorItem || off.getItem() instanceof InductionSurveyorItem) {
            this.mode = DetectionMode.INDUCTION;
        } else {
            this.mode = DetectionMode.MAGNETIC;
        }
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new MagnetometerScreen());
    }

    @Override
    protected void init() {
        sinceScan = RESCAN_TICKS;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (++sinceScan >= RESCAN_TICKS) {
            scan();
            sinceScan = 0;
        }
    }

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
                    continue;
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

    private boolean isTarget(BlockState state) {
        for (TagKey<Block> tag : mode.tags) {
            if (state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);

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

        int px0 = cx - PANEL_W / 2, px1 = cx + PANEL_W / 2;
        int py0 = titleY - 8, py1 = panelBottom;
        g.fill(px0, py0, px1, py1, PANEL_BG);
        g.fill(px0, py0, px1, py0 + 1, PANEL_EDGE);
        g.fill(px0, py1 - 1, px1, py1, PANEL_EDGE);
        g.fill(px0, py0, px0 + 1, py1, PANEL_EDGE);
        g.fill(px1 - 1, py0, px1, py1, PANEL_EDGE);

        g.centeredText(this.font, mode.title, cx, titleY, TEXT);

        fillDisc(g, cx, discCY, RADAR_R + 1, DISC_EDGE);
        fillDisc(g, cx, discCY, RADAR_R, DISC_FACE);
        g.fill(cx - RADAR_R, discCY, cx + RADAR_R, discCY + 1, SPOKE);
        g.fill(cx, discCY - RADAR_R, cx + 1, discCY + RADAR_R, SPOKE);
        drawRing(g, cx, discCY, RADAR_R / 3, RING);
        drawRing(g, cx, discCY, RADAR_R * 2 / 3, RING);
        drawRing(g, cx, discCY, RADAR_R, RING);
        g.centeredText(this.font, "N", cx, discCY - RADAR_R - 1, CROSS);
        g.centeredText(this.font, "S", cx, discCY + RADAR_R - 7, TEXT_DIM);
        g.centeredText(this.font, "E", cx + RADAR_R - 5, discCY - 4, TEXT_DIM);
        g.centeredText(this.font, "W", cx - RADAR_R + 5, discCY - 4, TEXT_DIM);

        for (Blip blip : blips) {
            if (blip.horizontal() > RANGE) {
                continue;
            }
            int bx = cx + (int) Math.round(blip.dx / RANGE * RADAR_R);
            int bz = discCY + (int) Math.round(blip.dz / RANGE * RADAR_R);
            double t = Math.min(1.0, blip.distance() / (RANGE * 1.2));
            int color = scaleBrightness(blip.type().color(), 1.0 - 0.65 * t);
            int s = t < 0.33 ? 2 : (t < 0.66 ? 1 : 0);
            g.fill(bx - s, bz - s, bx + s + 1, bz + s + 1, color);
        }
        if (nearest != null && nearest.horizontal() > 0.01) {
            double ux = nearest.dx / nearest.horizontal();
            double uz = nearest.dz / nearest.horizontal();
            drawArrow(g, cx, discCY, ux, uz, RADAR_R * 0.55, nearest.type().color());
        }

        g.fill(cx - 1, discCY - 1, cx + 2, discCY + 2, CROSS);

        if (nearest != null) {
            int depth = (int) Math.round(-nearest.dy);
            String where = depth == 0 ? "same level" : (depth > 0 ? depth + "m below" : (-depth) + "m above");
            g.text(this.font, "Nearest: " + nearest.type().name(), contentLeft, readoutY, nearest.type().color(), false);
            g.text(this.font, String.format("%.1fm  %s", nearest.distance(), where), contentLeft, readoutY + 10, TEXT_DIM, false);
        } else {
            g.text(this.font, "Nearest: none in range", contentLeft, readoutY, TEXT_DIM, false);
        }

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

        ItemStack device = findDevice();
        int energy = device == null ? 0 : device.getOrDefault(SolenoidDataComponents.EMF_ENERGY.get(), 0);
        int capacity = device != null && device.getItem() instanceof com.suiseika.solenoid.EmfPoweredItem epi 
                ? epi.getCapacity() : MagnetometerItem.CAPACITY;
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

    private static void drawArrow(GuiGraphicsExtractor g, int cx, int cy, double ux, double uz, double length, int color) {
        int outline = 0xFF05080B;
        double tipX = cx + ux * length;
        double tipY = cy + uz * length;
        double perpX = -uz, perpZ = ux;
        plotThickSegment(g, cx, cy, tipX, tipY, perpX, perpZ, 1.5, outline);
        plotThickSegment(g, cx, cy, tipX, tipY, perpX, perpZ, 0.5, color);
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
                if (Math.abs(o) > halfWidth) continue;
                int px = (int) Math.round(x + perpX * o);
                int py = (int) Math.round(y + perpZ * o);
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private static void fillDisc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static void drawRing(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        if (r <= 0) return;
        int steps = Math.max(24, r * 4);
        for (int i = 0; i < steps; i++) {
            double ang = (Math.PI * 2 * i) / steps;
            int px = cx + (int) Math.round(Math.cos(ang) * r);
            int py = cy + (int) Math.round(Math.sin(ang) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static int scaleBrightness(int argb, double factor) {
        factor = Math.max(0.0, Math.min(1.0, factor));
        int aa = (argb >>> 24) & 0xFF;
        int rr = (int) (((argb >> 16) & 0xFF) * factor);
        int gg = (int) (((argb >> 8) & 0xFF) * factor);
        int bb = (int) ((argb & 0xFF) * factor);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    private ItemStack findDevice() {
        Player player = minecraft == null ? null : minecraft.player;
        if (player == null) return null;
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (stack.getItem() instanceof com.suiseika.solenoid.EmfPoweredItem) {
                return stack;
            }
        }
        return null;
    }
}
