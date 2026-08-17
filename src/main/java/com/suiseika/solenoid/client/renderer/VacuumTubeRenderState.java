package com.suiseika.solenoid.client.renderer;

import com.suiseika.solenoid.energy.TubeMode;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class VacuumTubeRenderState extends BlockEntityRenderState {
    public static class RenderedItem {
        public final ItemStackRenderState itemState = new ItemStackRenderState();
        public Direction from;
        public Direction to;
        public float progress;
    }

    public final List<RenderedItem> items = new ArrayList<>();
    public final TubeMode[] sideModes = new TubeMode[6];
    public long gameTime;
}
