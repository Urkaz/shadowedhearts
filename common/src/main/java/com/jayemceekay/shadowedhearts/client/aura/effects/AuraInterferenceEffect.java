package com.jayemceekay.shadowedhearts.client.aura.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Strategy interface for element-specific interference overlays.
 */
public interface AuraInterferenceEffect {
    /**
     * Render a full-screen or partial overlay for the given intensity.
     * Intensity is 0..1 after distance mapping and any easing.
     */
    void render(GuiGraphics guiGraphics, int width, int height, float partialTick, float intensity, Minecraft mc);
}
