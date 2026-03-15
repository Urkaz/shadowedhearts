package com.jayemceekay.shadowedhearts.client.aura.effects;

import com.jayemceekay.shadowedhearts.client.ModShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Ghost-type spectral static using the AURA_STATIC_INTERFERENCE screen shader. */
public class GhostInterferenceEffect implements AuraInterferenceEffect {

    @Override
    public void render(GuiGraphics guiGraphics, int width, int height, float partialTick, float intensity, Minecraft mc) {
        if (ModShaders.AURA_STATIC_INTERFERENCE == null) return;
        var shader = ModShaders.AURA_STATIC_INTERFERENCE;

        // Set uniforms
        shader.getUniform("ScreenSize")
                .set((float) mc.getWindow().getGuiScaledWidth(), (float) mc.getWindow().getGuiScaledHeight());
        float gameTimeDays = (mc.level == null ? 0f : (mc.level.getDayTime() % 24000L) / 24000.0f);
        shader.getUniform("GameTime").set(gameTimeDays + partialTick / 24000.0f);
        if (shader.safeGetUniform("uIntensity") != null) {
            shader.safeGetUniform("uIntensity").set(intensity);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);

        var pose = guiGraphics.pose().last().pose();
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float x0 = 0f;
        float y0 = 0f;
        float x1 = (float) width;
        float y1 = (float) height;

        buf.addVertex(pose, x0, y1, 0f).setUv(0f, 1f);
        buf.addVertex(pose, x1, y1, 0f).setUv(1f, 1f);
        buf.addVertex(pose, x1, y0, 0f).setUv(1f, 0f);
        buf.addVertex(pose, x0, y0, 0f).setUv(0f, 0f);

        BufferUploader.drawWithShader(buf.buildOrThrow());
    }
}
