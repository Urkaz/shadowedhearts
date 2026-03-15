package com.jayemceekay.shadowedhearts.client.gui;

import com.jayemceekay.shadowedhearts.client.gui.modes.AbstractModeLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
            } else if (angle >= 60 && angle < 180) {
                hoveredMode = AuraReaderManager.AuraScannerMode.DOWSING_MACHINE;
            } else if (angle >= 180 && angle < 300) {
                hoveredMode = AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER;
            }
        } else {
            hoveredMode = null;
        }
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
            if (hoveredMode == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER) {
                if (AuraReaderManager.hasUpgrade(hoveredMode)) {
                    AuraReaderManager.currentMode = hoveredMode;
                } else {
                    Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.shadowedhearts.pokedex_integrator_required"), true);
                }
            } else {
                AuraReaderManager.currentMode = hoveredMode;
            }
        }
        AbstractModeLogic.modeMenuOpen = false;
        super.onClose();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(hoveredMode != null) {
            if (hoveredMode == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER) {
                if (AuraReaderManager.hasUpgrade(hoveredMode)) {
                    AuraReaderManager.currentMode = hoveredMode;
                } else {
                    Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.shadowedhearts.pokedex_integrator_required"), true);
                }
            } else if (hoveredMode == AuraReaderManager.AuraScannerMode.DOWSING_MACHINE) {
                if (AuraReaderManager.hasUpgrade(hoveredMode)) {
                    AuraReaderManager.currentMode = hoveredMode;
                } else {
                    Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.shadowedhearts.dowsing_machine_required"), true);
            }
            } else {
                AuraReaderManager.currentMode = hoveredMode;
            }
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
}
