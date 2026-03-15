package com.jayemceekay.shadowedhearts.client.gui.modes;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

public abstract class AbstractModeRenderer implements AuraScannerModeRenderer {

    public static final ResourceLocation SCAN_SCREEN = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/pokedex_screen_scan.png");
    public static final ResourceLocation SCAN_OVERLAY_CORNERS = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_corners.png");
    public static final ResourceLocation SCAN_OVERLAY_TOP = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_border_top.png");
    public static final ResourceLocation SCAN_OVERLAY_BOTTOM = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_border_bottom.png");
    public static final ResourceLocation SCAN_OVERLAY_LEFT = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_border_left.png");
    public static final ResourceLocation SCAN_OVERLAY_RIGHT = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_border_right.png");
    public static final ResourceLocation SCAN_OVERLAY_LINES = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_scanlines.png");
    public static final ResourceLocation SCAN_OVERLAY_NOTCH = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_notch.png");
    public static final ResourceLocation SCAN_RING_OUTER = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_ring_outer.png");
    public static final ResourceLocation SCAN_RING_MIDDLE = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_ring_middle.png");
    public static final ResourceLocation SCAN_RING_INNER = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_ring_inner.png");
    public static final ResourceLocation CENTER_INFO_FRAME = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_info_frame.png");
    public static final ResourceLocation UNKNOWN_MARK = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_unknown.png");
    public static final ResourceLocation POINTER = MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/pointer.png");
    public static final int SCAN_OVERLAY_NOTCH_WIDTH = 200;
    public static final int CENTER_INFO_FRAME_WIDTH = 128;
    public static final int CENTER_INFO_FRAME_HEIGHT = 16;
    public static final int OUTER_INFO_FRAME_WIDTH = 92;
    public static final int OUTER_INFO_FRAME_HEIGHT = 55;
    public static final int INNER_INFO_FRAME_WIDTH = 120;
    public static final int INNER_INFO_FRAME_HEIGHT = 20;
    public static final int INNER_INFO_FRAME_STEM_WIDTH = 28;
    public static final int UNKNOWN_MARK_WIDTH = 34;
    public static final int UNKNOWN_MARK_HEIGHT = 46;
    public static final int POINTER_WIDTH = 6;
    public static final int POINTER_HEIGHT = 10;
    public static final int POINTER_OFFSET = 30;
    public static final int SCAN_RING_MIDDLE_WIDTH = 100;
    public static final int SCAN_RING_MIDDLE_HEIGHT = 1;
    public static final int SCAN_RING_OUTER_DIAMETER = 116;
    public static final int SCAN_RING_INNER_DIAMETER = 84;

    public void renderScanRings(PoseStack poseStack, int centerX, int centerY, float opacity) {
        float rotation = AbstractModeLogic.usageIntervals % 360;

        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0.0f);

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians((-rotation) * 0.5)));
        GuiUtilsKt.blitk(poseStack, SCAN_RING_OUTER, -(SCAN_RING_OUTER_DIAMETER / 2), -(SCAN_RING_OUTER_DIAMETER / 2), SCAN_RING_OUTER_DIAMETER, SCAN_RING_OUTER_DIAMETER, 0, 0, SCAN_RING_OUTER_DIAMETER, SCAN_RING_OUTER_DIAMETER, 0, 1, 1, 1, opacity, true, 1F);
        poseStack.popPose();

        int segments = 40;

        for (int i = 0; i < segments; i++) {
            Quaternionf rotationQuaternion = new Quaternionf().rotateZ((float) Math.toRadians((i * 4.5) + (rotation * 0.5)));
            poseStack.pushPose();
            poseStack.mulPose(rotationQuaternion);
            GuiUtilsKt.blitk(poseStack, SCAN_RING_MIDDLE, -(SCAN_RING_MIDDLE_WIDTH / 2), -(SCAN_RING_MIDDLE_HEIGHT / 2F), SCAN_RING_MIDDLE_HEIGHT, SCAN_RING_MIDDLE_WIDTH, 0, 0, SCAN_RING_MIDDLE_WIDTH, SCAN_RING_MIDDLE_HEIGHT, 0, 1, 1, 1, opacity, true, 1F);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(-AbstractModeLogic.innerRingRotation)));
        GuiUtilsKt.blitk(poseStack, SCAN_RING_INNER, -(SCAN_RING_INNER_DIAMETER / 2), -(SCAN_RING_INNER_DIAMETER / 2), SCAN_RING_INNER_DIAMETER, SCAN_RING_INNER_DIAMETER, 0, 0, SCAN_RING_INNER_DIAMETER, SCAN_RING_INNER_DIAMETER, 0, 1, 1, 1, opacity, true, 1F);
        poseStack.popPose();

        poseStack.popPose();
    }

    public void renderScanOverlay(GuiGraphics graphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = graphics.pose();

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();

        // Scanning overlay
        float opacity = 1F;
        // Draw scan lines
        double interlacePos = Math.ceil((AbstractModeLogic.usageIntervals % 14) * 0.5) * 0.5;
        for (int i = 0; i < screenHeight; i++) {
            if (i % 4 == 0)
                GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_LINES, 0, i - interlacePos, 4, screenWidth, 0, 0, screenWidth, 4, 0, 1, 1, 1, opacity, true, 1F);
        }

        // Draw borders
        // Top left corner
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_CORNERS, 0, 0, 4, 4, 0, 0, 8, 8, 0, 1, 1, 1, opacity, true, 1F);
        // Top right corner
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_CORNERS, (screenWidth - 4), 0, 4, 4, 4, 0, 8, 8, 0, 1, 1, 1, opacity, true, 1F);
        // Bottom left corner
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_CORNERS, 0, (screenHeight - 4), 4, 4, 0, 4, 8, 8, 0, 1, 1, 1, opacity, true, 1F);
        // Bottom right corner
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_CORNERS, (screenWidth - 4), (screenHeight - 4), 4, 4, 4, 4, 8, 8, 0, 1, 1, 1, opacity, true, 1F);

        // Border sides
        int notchStartX = (screenWidth - SCAN_OVERLAY_NOTCH_WIDTH) / 2;
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_TOP, 4, 0, 3, notchStartX - 4, 0, 0, notchStartX - 4, 3, 0, 1, 1, 1, opacity, true, 1F);
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_TOP, notchStartX + SCAN_OVERLAY_NOTCH_WIDTH, 0, 3, (screenWidth - (notchStartX + SCAN_OVERLAY_NOTCH_WIDTH + 4)), 0, 0, (screenWidth - (notchStartX + SCAN_OVERLAY_NOTCH_WIDTH + 4)), 3, 0, 1, 1, 1, opacity, true, 1F);
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_BOTTOM, 4, (screenHeight - 3), 3, (screenWidth - 8), 0, 0, (screenWidth - 8), 3, 0, 1, 1, 1, opacity, true, 1F);
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_LEFT, 0, 4, (screenHeight - 8), 3, 0, 0, 3, (screenHeight - 8), 0, 1, 1, 1, opacity, true, 1F);
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_RIGHT, (screenWidth - 3), 4, (screenHeight - 8), 3, 0, 0, 3, (screenHeight - 8), 0, 1, 1, 1, opacity, true, 1F);
        GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_NOTCH, notchStartX, 0, 12, SCAN_OVERLAY_NOTCH_WIDTH, 0, 0, SCAN_OVERLAY_NOTCH_WIDTH, 12, 0, 1, 1, 1, opacity, true, 1F);

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Scan rings
        renderScanRings(matrices, centerX, centerY, opacity);

        RenderSystem.disableBlend();
    }


    @Override
    public void render(GuiGraphics guiGraphics, int width, int height, float alpha, float time, float partialTick) {
        AbstractModeLogic.updateAnimations(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks());
    }
}
