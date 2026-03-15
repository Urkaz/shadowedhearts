package com.jayemceekay.shadowedhearts.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class CurvedGuiGraphics extends GuiGraphics {

    public CurvedGuiGraphics(Minecraft minecraft, MultiBufferSource.BufferSource bufferSource) {
        super(minecraft, bufferSource);
    }

    public static void fillBent(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        Matrix4f matrix = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());

        int segments = 32;

        for (int i = 0; i < segments; i++) {

            float t0 = (float)i / segments;
            float t1 = (float)(i + 1) / segments;

            float xa = Mth.lerp(t0, x1, x2);
            float xb = Mth.lerp(t1, x1, x2);

            float bendA = Mth.sin(xa * 0.02f) * 5f;
            float bendB = Mth.sin(xb * 0.02f) * 5f;

            vc.addVertex(matrix, xa, y1 + bendA, 0).setColor(color);
            vc.addVertex(matrix, xa, y2 + bendA, 0).setColor(color);
            vc.addVertex(matrix, xb, y2 + bendB, 0).setColor(color);
            vc.addVertex(matrix, xb, y1 + bendB, 0).setColor(color);
        }
    }

}
