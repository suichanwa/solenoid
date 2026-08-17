package com.suiseika.solenoid.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.suiseika.solenoid.energy.VacuumTubeBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Renders items traveling inside the transparent magnetic vacuum tubes using NeoForge 26.1.2 render pipeline.
 */
public class VacuumTubeRenderer implements BlockEntityRenderer<VacuumTubeBlockEntity, VacuumTubeRenderState> {
    private final ItemModelResolver itemModelResolver;

    public VacuumTubeRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public VacuumTubeRenderState createRenderState() {
        return new VacuumTubeRenderState();
    }

    @Override
    public void extractRenderState(VacuumTubeBlockEntity be, VacuumTubeRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumblingOverlay);
        state.gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        state.items.clear();

        for (int i = 0; i < VacuumTubeBlockEntity.MAX_TRAVELING; i++) {
            ItemStack stack = be.getTravelingStack(i);
            if (!stack.isEmpty()) {
                VacuumTubeRenderState.RenderedItem item = new VacuumTubeRenderState.RenderedItem();
                this.itemModelResolver.updateForTopItem(item.itemState, stack, ItemDisplayContext.FIXED, be.getLevel(), null, i);
                item.from = be.getFromDir(i);
                item.to = be.getToDir(i);
                item.progress = be.getProgress(i, partialTick);
                state.items.add(item);
            }
        }

        for (int d = 0; d < 6; d++) {
            state.sideModes[d] = be.getSideMode(Direction.values()[d]);
        }
    }

    @Override
    public void submit(VacuumTubeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        int light = state.lightCoords != 0 ? state.lightCoords : 0x00F000F0;

        for (VacuumTubeRenderState.RenderedItem item : state.items) {
            if (item.itemState.isEmpty()) continue;

            Direction from = item.from;
            Direction to = item.to;
            float progress = Math.max(0.0f, Math.min(1.0f, item.progress));

            double x = 0.5;
            double y = 0.5;
            double z = 0.5;

            if (from != null && to != null) {
                if (progress < 0.5f) {
                    float t = progress * 2.0f;
                    x = (0.5 + from.getStepX() * 0.5) * (1.0f - t) + 0.5 * t;
                    y = (0.5 + from.getStepY() * 0.5) * (1.0f - t) + 0.5 * t;
                    z = (0.5 + from.getStepZ() * 0.5) * (1.0f - t) + 0.5 * t;
                } else {
                    float t = (progress - 0.5f) * 2.0f;
                    x = 0.5 * (1.0f - t) + (0.5 + to.getStepX() * 0.5) * t;
                    y = 0.5 * (1.0f - t) + (0.5 + to.getStepY() * 0.5) * t;
                    z = 0.5 * (1.0f - t) + (0.5 + to.getStepZ() * 0.5) * t;
                }
            } else if (to != null) {
                float t = progress;
                x = 0.5 * (1.0f - t) + (0.5 + to.getStepX() * 0.5) * t;
                y = 0.5 * (1.0f - t) + (0.5 + to.getStepY() * 0.5) * t;
                z = 0.5 * (1.0f - t) + (0.5 + to.getStepZ() * 0.5) * t;
            } else if (from != null) {
                float t = progress;
                x = (0.5 + from.getStepX() * 0.5) * (1.0f - t) + 0.5 * t;
                y = (0.5 + from.getStepY() * 0.5) * (1.0f - t) + 0.5 * t;
                z = (0.5 + from.getStepZ() * 0.5) * (1.0f - t) + 0.5 * t;
            }

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.scale(0.35f, 0.35f, 0.35f);

            float rotation = (state.gameTime + progress * 20.0f) * 15.0f;
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

            item.itemState.submit(poseStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
