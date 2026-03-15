package com.jayemceekay.shadowedhearts.client.aura.effects;

import com.jayemceekay.shadowedhearts.client.ModShaders;
import com.jayemceekay.shadowedhearts.client.aura.AuraPulseRenderer;
import com.jayemceekay.shadowedhearts.client.aura.IrisHandler;
import com.jayemceekay.shadowedhearts.client.render.DepthCapture;
import com.jayemceekay.shadowedhearts.mixin.RenderTargetAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class FireInterferenceEffect implements AuraInterferenceEffect{

    // Temporary color target to avoid reading from the same buffer we write to
    private static RenderTarget heatTemp;

    private static void ensureTemp(Minecraft mc) {
        RenderTarget main = mc.getMainRenderTarget();
        int mw = ((RenderTargetAccessor)(Object)main).getWidth();
        int mh = ((RenderTargetAccessor)(Object)main).getHeight();
        int tw = (heatTemp == null) ? -1 : ((RenderTargetAccessor)(Object)heatTemp).getWidth();
        int th = (heatTemp == null) ? -1 : ((RenderTargetAccessor)(Object)heatTemp).getHeight();
        if (heatTemp == null || tw != mw || th != mh) {
            if (heatTemp != null) {
                heatTemp.destroyBuffers();
            }
            heatTemp = new TextureTarget(mw, mh, true, Minecraft.ON_OSX);
            heatTemp.setClearColor(0f, 0f, 0f, 0f);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int width, int height, float partialTick, float intensity, Minecraft mc) {
        if(ModShaders.HEAT_HAZE_INTERFERENCE == null) return;
        var shader = ModShaders.HEAT_HAZE_INTERFERENCE;

        // Use the actual render target resolution for ScreenSize (matches sampled texture)
        RenderTarget target = mc.getMainRenderTarget();
        int tW = ((RenderTargetAccessor)(Object)target).getWidth();
        int tH = ((RenderTargetAccessor)(Object)target).getHeight();
        shader.safeGetUniform("ScreenSize").set((float) tW, (float) tH);
        // Supply shader time in seconds for smooth, real-time animation
        float gameTimeSec = (mc.level == null ? 0f : (mc.level.getGameTime() + partialTick) / 20.0f);
        shader.safeGetUniform("GameTime").set(gameTimeSec);
        if(shader.getUniform("uHeat") != null){
            //replace with temperature reading of aura reader operational temperature
            shader.getUniform("uHeat").set(intensity);
        }

        // Pass scan center in normalized UV space
        shader.safeGetUniform("uScanCenter").set(0.5f, 0.5f);

        // Bind samplers the same way as AuraPulseRenderer
        DepthCapture.captureIfNeeded();
        int diffuseTexture = mc.getMainRenderTarget().getColorTextureId();
        int depthTexture = DepthCapture.textureId();

        IrisHandler iris = AuraPulseRenderer.IRIS_HANDLER;
        boolean usingIrisDiffuse = false;
        if (iris != null && iris.isShaderPackInUse()) {
            IrisHandler.IrisRenderingSnapshot snapshot = iris.getIrisRenderingSnapshot();
            if (snapshot != null) {
                if (snapshot.diffuseTexture != -1) {
                    diffuseTexture = snapshot.diffuseTexture;
                    usingIrisDiffuse = true; // Safe source separate from main target
                }
                depthTexture = snapshot.depthTexture;
            }
        }

        // Avoid sampling from the same buffer we're rendering into by copying main color to a temp
        //if (!usingIrisDiffuse) {
            ensureTemp(mc);
            // Copy current main color into our temp, then sample from temp and write back to main
            // Blit color from main → temp (no depth needed here)
            RenderTarget main = mc.getMainRenderTarget();
            int mw = ((RenderTargetAccessor)(Object)main).getWidth();
            int mh = ((RenderTargetAccessor)(Object)main).getHeight();

            int prevRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int prevDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            try {
                int srcFbo = ((RenderTargetAccessor)(Object)main).getFrameBufferId();
                int dstFbo = ((RenderTargetAccessor)(Object)heatTemp).getFrameBufferId();
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dstFbo);
                GL30.glBlitFramebuffer(0, 0, mw, mh, 0, 0, mw, mh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            } finally {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
            }
            shader.setSampler("DiffuseSampler", heatTemp.getColorTextureId());
        //} else {
            // Iris provides a separate diffuse texture; sampling directly is safe
        //    shader.setSampler("DiffuseSampler", diffuseTexture);
       // }
        shader.setSampler("uDepth", depthTexture);
        // Toggle depth usage for the shader so we don't zero-out distortion when depth is missing
        if (shader.safeGetUniform("uUseDepth") != null) {
            boolean hasDepth = depthTexture != 0;
            shader.safeGetUniform("uUseDepth").set(hasDepth ? 1.0f : 0.0f);
        }
        shader.apply();
        //mc.getMainRenderTarget().bindWrite(false);
        // No additive blending: we output the refracted scene color directly
        //RenderSystem.disableDepthTest();
        //RenderSystem.disableBlend();
        RenderSystem.setShader(() -> shader);

        // Use Minecraft's basic texture shader
        //RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Bind the copied texture
        //RenderSystem.setShaderTexture(0, heatTemp.getColorTextureId());

        var pose = guiGraphics.pose().last().pose();
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float x0 = 0f;
        float y0 = 0f;
        float x1 = (float) width;
        float y1 = (float) height;

        buf.addVertex(pose, x0, y1, 0f).setUv(0f, 0f);
        buf.addVertex(pose, x1, y1, 0f).setUv(1f, 0f);
        buf.addVertex(pose, x1, y0, 0f).setUv(1f, 1f);
        buf.addVertex(pose, x0, y0, 0f).setUv(0f, 1f);

        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

}
