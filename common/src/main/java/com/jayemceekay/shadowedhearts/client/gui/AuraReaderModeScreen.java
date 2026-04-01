package com.jayemceekay.shadowedhearts.client.gui;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.render.RenderHelperKt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class AuraReaderModeScreen extends Screen {
    public AuraReaderModeScreen() {
        super(Component.literal("Aura Scanner Modes"));
    }

    private AuraReaderManager.AuraScannerMode hoveredMode = null;

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Update hovered mode based on mouse position relative to center
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distSq = dx * dx + dy * dy;

        if (distSq > 400) { // Only select if mouse is far enough from center
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (angle < 0) angle += 360;

            if (angle >= 300 || angle < 60) {
                hoveredMode = AuraReaderManager.AuraScannerMode.AURA_READER;
            }
//            else if (angle >= 60 && angle < 180) {
//                hoveredMode = AuraReaderManager.AuraScannerMode.DOWSING_MACHINE;
//            } else if (angle >= 180 && angle < 300) {
//                hoveredMode = AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER;
//            }
        } else {
            hoveredMode = null;
        }

        // Publish the currently hovered mode to the HUD state so HUD labels can react visually
        AuraReaderManager.HUD_STATE.hoveredMode.set(this.hoveredMode);

        // Render the radial menu labels in this screen (moved from HUD)
        AuraReaderManager.AuraScannerMode selected = AuraReaderManager.currentMode;

        renderModeLabel(
                guiGraphics,
                centerX,
                centerY,
                0,
                -80,
                Component.translatable("aura_scanner.mode.aura_reader"),
                hoveredMode == AuraReaderManager.AuraScannerMode.AURA_READER,
                selected == AuraReaderManager.AuraScannerMode.AURA_READER
        );

//        renderModeLabel(
//                guiGraphics,
//                centerX,
//                centerY,
//                -80,
//                60,
//                Component.translatable("aura_scanner.mode.pokedex_scanner"),
//                hoveredMode == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER,
//                selected == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER
//        );
//
//        renderModeLabel(
//                guiGraphics,
//                centerX,
//                centerY,
//                80,
//                60,
//                Component.translatable("aura_scanner.mode.dowsing_machine"),
//                hoveredMode == AuraReaderManager.AuraScannerMode.DOWSING_MACHINE,
//                selected == AuraReaderManager.AuraScannerMode.DOWSING_MACHINE
//        );
    }

    public AuraReaderManager.AuraScannerMode getHoveredMode() {
        return hoveredMode;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onClose() {
        if (hoveredMode != null) {
            AuraReaderManager.currentMode = hoveredMode;
//            if (hoveredMode == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER) {
//                if (AuraReaderManager.hasUpgrade(hoveredMode)) {
//                    AuraReaderManager.currentMode = hoveredMode;
//                } else {
//                    Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.shadowedhearts.pokedex_integrator_required"), true);
//                }
//            } else {
//                AuraReaderManager.currentMode = hoveredMode;
//            }
        }
        AuraReaderManager.HUD_STATE.isModeMenuOpen = false;
        // Clear hovered state so labels revert to their base color after closing the menu
        AuraReaderManager.HUD_STATE.hoveredMode.set(null);
        super.onClose();
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(hoveredMode != null) {
            AuraReaderManager.currentMode = hoveredMode;
//            if (hoveredMode == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER) {
//                if (AuraReaderManager.hasUpgrade(hoveredMode)) {
//                    AuraReaderManager.currentMode = hoveredMode;
//                } else {
//                    Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.shadowedhearts.pokedex_integrator_required"), true);
//                }
//            } else if (hoveredMode == AuraReaderManager.AuraScannerMode.DOWSING_MACHINE) {
//                if (AuraReaderManager.hasUpgrade(hoveredMode)) {
//                    AuraReaderManager.currentMode = hoveredMode;
//                } else {
//                    Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.shadowedhearts.dowsing_machine_required"), true);
//            }
//            } else {
//                AuraReaderManager.currentMode = hoveredMode;
//            }
            this.onClose();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }

    @Override
    protected void renderBlurredBackground(float f) {
    }

    @Override
    protected void renderMenuBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderMenuBackground(GuiGraphics guiGraphics, int i, int j, int k, int l) {

    }

    @Override
    public void renderTransparentBackground(GuiGraphics guiGraphics) {
    }

    private void renderModeLabel(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int xOffset,
            int yOffset,
            MutableComponent text,
            boolean hovered,
            boolean selected
    ) {
        int baseRGB = selected ? 0xFFA500 : 0x00FFFF; // orange when selected, cyan otherwise
        int rgb = hovered ? 0xFFFFFF : baseRGB;        // white on hover
        int argb = 0xFF000000 | rgb;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        // Move to screen center, then apply X offset. Y offset is applied per-element drawing
        pose.translate(centerX + xOffset, centerY, 0);

        int textWidth = Minecraft.getInstance().font.width(TextKt.font(text, CobblemonResources.INSTANCE.getDEFAULT_LARGE()));
        int tooltipWidth = textWidth + 4;
        int tooltipHeight = Minecraft.getInstance().font.lineHeight;
        int tooltipTop = yOffset + 1;

        // Tooltip frame and background
        GuiUtilsKt.blitk(pose, AuraScannerHudFactory.TOOLTIP_EDGE, (int) (-(tooltipWidth / 2f) - 1), tooltipTop, tooltipHeight, 1, 0, 0, 1, tooltipHeight, 0, 1, 1, 1, 1.0f, true, 1F);
        GuiUtilsKt.blitk(pose, AuraScannerHudFactory.TOOLTIP_BACKGROUND, (int) -(tooltipWidth / 2f), tooltipTop, tooltipHeight, tooltipWidth, 0, 0, tooltipWidth, tooltipHeight, 0, 1, 1, 1, 1.0f, true, 1F);
        GuiUtilsKt.blitk(pose, AuraScannerHudFactory.TOOLTIP_EDGE, (int) (tooltipWidth / 2f), tooltipTop, tooltipHeight, 1, 0, 0, 1, tooltipHeight, 0, 1, 1, 1, 1.0f, true, 1F);

        // Draw label text centered at x=0
        RenderHelperKt.drawScaledText(
                graphics,
                CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                text,
                0,
                tooltipTop,
                1F,
                1F,
                Integer.MAX_VALUE,
                argb,
                true,
                true,
                null,
                null
        );

        pose.popPose();
    }
}
