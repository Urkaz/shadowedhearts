package com.jayemceekay.shadowedhearts.client.gui.modes;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.cobblemon.mod.common.client.render.RenderHelperKt;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import com.jayemceekay.shadowedhearts.client.ModShaders;
import com.jayemceekay.shadowedhearts.client.render.rendertypes.HudRenderTypes;
import com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext;
import com.jayemceekay.shadowedhearts.common.shadow.ShadowAspectUtil;
import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class AuraReaderRendererImpl extends AbstractModeRenderer {

    private static final ResourceLocation POINTER = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/battle/arrow_pointer_up.png");
    private static final ResourceLocation SELECT_ARROW = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/select_arrow.png");
    private static final ResourceLocation TOOLTIP_EDGE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/tooltip_edge.png");
    private static final ResourceLocation TOOLTIP_BACKGROUND = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/tooltip_background.png");

    @Override
    public void render(GuiGraphics guiGraphics, int width, int height, float alpha, float time, float partialTick) {
        super.render(guiGraphics, width, height, alpha, time, partialTick);
        renderScanOverlay(guiGraphics, partialTick);
        renderDirectionalIndicatorsAndSweep(guiGraphics, width / 2, height / 2, alpha, time, partialTick);
        renderSignalStrength(guiGraphics, width, height, alpha, partialTick);
        guiGraphics.pose().pushPose();
        //renderCooldownBar(guiGraphics, width, height, alpha);
        guiGraphics.pose().popPose();
    }

    @Override
    public void renderScanRings(PoseStack poseStack, int centerX, int centerY, float opacity) {
        float rotation = AbstractModeLogic.usageIntervals % 360;

        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0.0f);

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians((-rotation) * 0.5)));
        GuiUtilsKt.blitk(poseStack, SCAN_RING_OUTER, -(SCAN_RING_OUTER_DIAMETER / 2), -(SCAN_RING_OUTER_DIAMETER / 2), SCAN_RING_OUTER_DIAMETER, SCAN_RING_OUTER_DIAMETER, 0, 0, SCAN_RING_OUTER_DIAMETER, SCAN_RING_OUTER_DIAMETER, 0, 1, 1, 1, opacity, true, 1F);
        poseStack.popPose();

        int segments = 40;
        int activeSegments = 0;
        if (AuraReaderLogic.lockedTarget != null) {
            activeSegments = (int) (AuraReaderLogic.lockedTarget.strength * segments);
        }

        for (int i = 0; i < activeSegments; i++) {
            Quaternionf rotationQuaternion = new Quaternionf().rotateZ((float) Math.toRadians(-(i * 4.5)));
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

    private void renderDirectionalIndicatorsAndSweep(GuiGraphics guiGraphics, int centerX, int centerY, float alpha, float time, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        float currentSweep = net.minecraft.util.Mth.lerp(partialTick, AbstractModeLogic.prevSweepAngle, AbstractModeLogic.sweepAngle);
        float currentGlitchTimer = net.minecraft.util.Mth.lerp(partialTick, AbstractModeLogic.prevGlitchTimer, AbstractModeLogic.glitchTimer);
        if (currentGlitchTimer > 0) {
            currentSweep += (mc.level.random.nextFloat() - 0.5f) * 0.5f;
        }

        int shadowRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerShadowRange();
        var nearbyEntities = mc.level.getEntities(player, player.getBoundingBox().inflate(shadowRange)).stream()
                .filter(e -> e instanceof PokemonEntity pe && ShadowAspectUtil.hasShadowAspect(pe.getPokemon()) && AuraReaderLogic.DETECTED_SHADOWS.containsKey(pe.getUUID()))
                .toList();

        for (Entity entity : nearbyEntities) {
            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            double angle = Math.atan2(dz, dx) - Math.toRadians(player.getYRot()) - Math.PI / 2;
            double dist = Math.sqrt(dx * dx + dz * dz);

            float intensity = (float) Math.max(0, 1.0 - (dist / shadowRange));
            float angleDiff = (float) (angle - currentSweep);
            while (angleDiff < -Math.PI) angleDiff += Math.PI * 2;
            while (angleDiff > Math.PI) angleDiff -= Math.PI * 2;
            float spike = Math.max(0, 1.0f - Math.abs(angleDiff) * 5.0f);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY, 0);
            guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) angle));

            boolean isLocked = AuraReaderLogic.lockedTarget != null && AuraReaderLogic.lockedTarget.type == AuraReaderLogic.TargetType.POKEMON && entity.getUUID().equals(AuraReaderLogic.lockedTarget.pokemonId);
            if (AuraReaderLogic.lockedTarget != null && !isLocked) {
                guiGraphics.pose().popPose();
                continue;
            }
            boolean isSelected = false;
            if (AuraReaderLogic.acquisitionMode && AuraReaderLogic.selectedSignalIndex >= 0 && AuraReaderLogic.selectedSignalIndex < AuraReaderLogic.CURRENT_SIGNALS.size()) {
                AuraReaderLogic.SignalTarget sel = AuraReaderLogic.CURRENT_SIGNALS.get(AuraReaderLogic.selectedSignalIndex);
                isSelected = sel.type == AuraReaderLogic.TargetType.POKEMON && entity.getUUID().equals(sel.pokemonId);
            }

            float segmentAlpha = alpha * intensity * (0.3f + 0.7f * spike);
            if (isLocked) {
                RenderSystem.setShaderColor(1.0f, 0.4f, 1.0f, Math.max(0.6f, segmentAlpha));
            } else if (isSelected) {
                RenderSystem.setShaderColor(0.9f, 0.6f, 1.0f, Math.max(0.5f, segmentAlpha));
            } else {
                RenderSystem.setShaderColor(0.6f, 0.3f, 1.0f, segmentAlpha);
            }
            guiGraphics.blit(POINTER, -8, -70, 0, 0, 16, 6, 16, 16);

            if (isSelected && AuraReaderLogic.lockedTarget == null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                guiGraphics.blit(SELECT_ARROW, -8, -82, 0, 0, 16, 16, 16, 16);
            }

            guiGraphics.pose().popPose();
        }

        int meteoroidRangeIndicator = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerMeteoroidRange();
        for (BlockPos p : AuraReaderLogic.DETECTED_METEOROIDS.keySet()) {
            double dx = p.getX() + 0.5 - player.getX();
            double dz = p.getZ() + 0.5 - player.getZ();
            double angle = Math.atan2(dz, dx) - Math.toRadians(player.getYRot()) - Math.PI / 2;
            double dist = Math.sqrt(dx * dx + dz * dz);

            float intensity = (float) Math.max(0, 1.0 - (dist / meteoroidRangeIndicator));
            float angleDiff = (float) (angle - currentSweep);
            while (angleDiff < -Math.PI) angleDiff += Math.PI * 2;
            while (angleDiff > Math.PI) angleDiff -= Math.PI * 2;
            float spike = Math.max(0, 1.0f - Math.abs(angleDiff) * 5.0f);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY, 0);
            guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) angle));

            boolean isLocked = AuraReaderLogic.lockedTarget != null && AuraReaderLogic.lockedTarget.type == AuraReaderLogic.TargetType.METEOROID && p.equals(AuraReaderLogic.lockedTarget.meteoroidPos);
            if (AuraReaderLogic.lockedTarget != null && !isLocked) {
                guiGraphics.pose().popPose();
                continue;
            }
            boolean isSelected = false;
            if (AuraReaderLogic.acquisitionMode && AuraReaderLogic.selectedSignalIndex >= 0 && AuraReaderLogic.selectedSignalIndex < AuraReaderLogic.CURRENT_SIGNALS.size()) {
                AuraReaderLogic.SignalTarget sel = AuraReaderLogic.CURRENT_SIGNALS.get(AuraReaderLogic.selectedSignalIndex);
                isSelected = sel.type == AuraReaderLogic.TargetType.METEOROID && p.equals(sel.meteoroidPos);
            }

            float segmentAlpha = alpha * intensity * (0.3f + 0.7f * spike);
            if (isLocked) {
                RenderSystem.setShaderColor(1.0f, 0.4f, 1.0f, Math.max(0.6f, segmentAlpha));
            } else if (isSelected) {
                RenderSystem.setShaderColor(0.9f, 0.6f, 1.0f, Math.max(0.5f, segmentAlpha));
            } else {
                RenderSystem.setShaderColor(0.8f, 0.2f, 0.9f, segmentAlpha);
            }
            guiGraphics.blit(POINTER, -8, -70, 0, 0, 16, 6, 16, 16);

            if (isSelected && AuraReaderLogic.lockedTarget == null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                guiGraphics.blit(SELECT_ARROW, -8, -82, 0, 0, 16, 16, 16, 16);
            }

            guiGraphics.pose().popPose();
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderSignalStrength(GuiGraphics guiGraphics, int width, int height, float alpha, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        String label = "NO SIGNAL";
        int color = 0xAAAAAA;
        if (AbstractModeLogic.maxIntensity > 0.8) {
            label = "STRONG SHADOW PRESENCE";
            color = 0xA330FF;
        } else if (AbstractModeLogic.maxIntensity > 0.5) {
            label = "MODERATE DISTURBANCE";
            color = 0x00A3FF;
        } else if (AbstractModeLogic.maxIntensity > 0) {
            label = "WEAK SIGNAL";
            color = 0x00FFFF;
        }

        int textAlpha = (int) (alpha * 255) << 24;
        int frameY = height / 2 - 110;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.drawCenteredString(mc.font, label, width / 2, frameY + 4, color | textAlpha);

        String infoLine = null;
        float strength = 0f;
        float interference = 0f;

        if (AuraReaderLogic.lockedTarget != null) {
            if (AuraReaderLogic.lockedTarget.type == AuraReaderLogic.TargetType.POKEMON) {
                Entity e = null;
                for (Entity cand : mc.level.entitiesForRendering()) {
                    if (cand.getUUID().equals(AuraReaderLogic.lockedTarget.pokemonId)) {
                        e = cand;
                        break;
                    }
                }
                if (e != null) {
                    int sRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerShadowRange();
                    double dist = e.distanceTo(mc.player);
                    float str = (float) Math.max(0, 1.0 - (dist / sRange));
                    strength = str;
                    interference = 1.0f - str;
                    infoLine = String.format("LOCKED: SHADOW POKÉMON  |  %.0fm", dist);
                } else {
                    infoLine = "LOCKED: SHADOW POKÉMON";
                }
            } else {
                int mRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerMeteoroidRange();
                double dist = Math.sqrt(AuraReaderLogic.lockedTarget.meteoroidPos.distSqr(mc.player.blockPosition()));
                float str = (float) Math.max(0, 1.0 - (dist / mRange));
                strength = str;
                interference = 1.0f - str;
                infoLine = String.format("LOCKED: SHADOW METEOROID  |  %.0fm", dist);
            }
        } else if (AuraReaderLogic.acquisitionMode && !AuraReaderLogic.CURRENT_SIGNALS.isEmpty() && AuraReaderLogic.selectedSignalIndex >= 0 && AuraReaderLogic.selectedSignalIndex < AuraReaderLogic.CURRENT_SIGNALS.size()) {
            AuraReaderLogic.SignalTarget sel = AuraReaderLogic.CURRENT_SIGNALS.get(AuraReaderLogic.selectedSignalIndex);
            strength = sel.strength;
            interference = sel.interference;
            if (sel.type == AuraReaderLogic.TargetType.POKEMON) {
                infoLine = String.format("SELECT: SHADOW POKÉMON  |  %.0fm", sel.distance);
            } else {
                infoLine = String.format("SELECT: SHADOW METEOROID  |  %.0fm", sel.distance);
            }
        }
        if (infoLine != null) {
            guiGraphics.drawCenteredString(mc.font, infoLine, width / 2, frameY + 16, 0xFFFFFF | textAlpha);
            renderSignalMeterBars(guiGraphics, width, height, alpha, strength, interference);
        }

        // Waveform
        int waveX = width / 2 - 50;
        int waveY = frameY + 45;
        float waveTime = (mc.level.getGameTime() + partialTick) * 0.5f;
        for (int i = 0; i < 20; i++) {
            float h = (float) Math.sin(waveTime + i * 0.5f) * 10 * AbstractModeLogic.maxIntensity;
            if (AbstractModeLogic.glitchTimer > 0)
                h *= mc.level.random.nextFloat() * 2;
            guiGraphics.fill(waveX + i * 5, waveY, waveX + i * 5 + 2, (int) (waveY - h), (color & 0xFFFFFF) | (textAlpha / 2));
        }
    }

    private void renderSignalMeterBars(GuiGraphics guiGraphics, int width, int height, float alpha, float strength, float interference) {
        int barWidth = 80;
        int centerX = width / 2;
        int baseY = height - 44;

        renderMeterBar(guiGraphics, centerX - barWidth - 5, baseY, barWidth, strength, 0xA330FF, alpha, "STR");
        renderMeterBar(guiGraphics, centerX + 5, baseY, barWidth, interference, 0x00FFFF, alpha, "INT");
    }

    private void renderMeterBar(GuiGraphics guiGraphics, int x, int y, int width, float progress, int barColor, float alpha, String label) {
        int barHeight = 11;
        int textAlpha = (int) (alpha * 255) << 24;
        Minecraft mc = Minecraft.getInstance();

        // Background
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        guiGraphics.blit(TOOLTIP_EDGE, x - 1, y, 0, 0, 1, barHeight, 1, barHeight);
        guiGraphics.blit(TOOLTIP_BACKGROUND, x, y, 0, 0, width, barHeight, width, barHeight);
        guiGraphics.blit(TOOLTIP_EDGE, x + width, y, 0, 0, 1, barHeight, 1, barHeight);

        // Fill
        if (progress > 0) {
            int fillWidth = (int) (width * progress);
            guiGraphics.fill(x, y + 2, x + fillWidth, y + barHeight - 2, barColor | textAlpha);
        }

        // Label
        guiGraphics.drawString(mc.font, label, x + 2, y + 2, 0xFFFFFF | textAlpha);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void renderCooldownBar(GuiGraphics guiGraphics, int width, int height, float alpha) {
        //if (AbstractModeLogic.pulseCooldown <= 0) return;
        int barWidth = 100;
        int barHeight = 5;
        int x = (width - barWidth) / 2;
        int y = height / 2;
        int color = 0x28F0FF;
        int textAlpha = (int) (alpha * 255) << 24;

       /* guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xAA000000 | (textAlpha & 0xFF000000));

        float ratio = Math.min(1.0f, Math.max(0.0f, (float) AbstractModeLogic.pulseCooldown / (float) AbstractModeLogic.PULSE_COOLDOWN_TICKS));
        int fillWidth = (int) (barWidth * ratio);
        guiGraphics.fill(x, y, x + fillWidth, y + barHeight, color | textAlpha);

        Minecraft mc = Minecraft.getInstance();
        String cdText = String.format("COOLDOWN: %.1fs", AbstractModeLogic.pulseCooldown / 20.0f);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        //guiGraphics.drawCenteredString(mc.font, cdText, width / 3, y - 10, 0xCCCCCC | textAlpha);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        poseStack.popPose();*/

        PoseStack poseStack = guiGraphics.pose();
        poseStack.translate(-125f, 60f, 300f);
        poseStack.pushPose();
        Matrix4f mv = poseStack.last().pose();
        float ratio = Math.min(1.0f, Math.max(0.0f, (float) AbstractModeLogic.pulseCooldown / (float) AbstractModeLogic.PULSE_COOLDOWN_TICKS));
        float w = barWidth * ratio;
        var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        var vc = bufferSource.getBuffer(HudRenderTypes.BARREL_DISTORTION_RENDER_TYPE);

        RenderSystem.setShader(() -> ModShaders.HUD_BARREL_DISTORTION);
        ModShaders.HUD_BARREL_DISTORTION.safeGetUniform("Center").set((width + 0f)/ 2f, (Minecraft.getInstance().getWindow().getGuiScaledHeight() + 120f) / 2f);
        ModShaders.HUD_BARREL_DISTORTION.safeGetUniform("Curvature").set(0.010f);
        ModShaders.HUD_BARREL_DISTORTION.safeGetUniform("Angle").set((float) Math.toRadians(-10f));
        ModShaders.HUD_BARREL_DISTORTION.safeGetUniform("Radius").set(1.0f);
        int argb = color | textAlpha;
        float a = ((argb >> 24) & 255) / 255f;
        float r = ((argb >> 16) & 255) / 255f;
        float g = ((argb >>  8) & 255) / 255f;
        float b = ((argb      ) & 255) / 255f;

        int segments = 64;
        float y0 = y;
        float y1 = y + barHeight;
        for (int i = 0; i < segments; i++) {
            float x0 = x + (barWidth * i) / segments;
            float x1 = x + (barWidth * (i + 1)) / segments;

            vc.addVertex(mv, x0-1, y0-1, 0).setColor(0, 0, 0, a);
            vc.addVertex(mv, x0-1, y1+1, 0).setColor(0, 0, 0, a);
            vc.addVertex(mv, x1+1, y1+1, 0).setColor(0,0, 0, a);
            vc.addVertex(mv, x1+1, y0-1, 0).setColor(0, 0, 0, a);
        }

        bufferSource.endBatch(HudRenderTypes.BARREL_DISTORTION_RENDER_TYPE);
        vc = bufferSource.getBuffer(HudRenderTypes.BARREL_DISTORTION_RENDER_TYPE);
        for (int i = 0; i < segments; i++) {
            float x0 = x  + (w * i) / segments;
            float x1 = x  + (w * (i + 1)) / segments;

            vc.addVertex(mv, x0, y0, 0).setColor(r, g, b, a);
            vc.addVertex(mv, x0, y1, 0).setColor(r, g, b, a);
            vc.addVertex(mv, x1, y1, 0).setColor(r, g, b, a);
            vc.addVertex(mv, x1, y0, 0).setColor(r, g, b, a);
        }
        bufferSource.endBatch(HudRenderTypes.BARREL_DISTORTION_RENDER_TYPE);


        poseStack.popPose();
    }

    public void renderInfoFrames(GuiGraphics graphics, PoseStack poseStack, PokedexUsageContext usageContext, int centerX, int centerY, float opacity) {
        if (usageContext.getFocusIntervals() > 0) {
            int infoDisplayedCounter = 0;
            for (int index = 0; index < usageContext.getAvailableInfoFrames().size(); index++) {
                Boolean isLeftSide = usageContext.getAvailableInfoFrames().get(index);
                if (isLeftSide != null) {
                    if (infoDisplayedCounter > 1 && !usageContext.isPokemonInFocusOwned())
                        continue;
                    infoDisplayedCounter++;
                    // Frames
                    boolean isInnerFrame = index == 1 || index == 2;
                    int frameHeight = isInnerFrame ? INNER_INFO_FRAME_HEIGHT : OUTER_INFO_FRAME_HEIGHT;
                    int xOffset = (isInnerFrame ? (-177) : (-120)) + (isLeftSide ? 0 : (isInnerFrame ? 234 : 148));
                    int yOffset = switch (index) {
                        case 0 -> -80;
                        case 1 -> -26;
                        case 2 -> 6;
                        case 3 -> 25;
                        default -> 0;
                    };
                    GuiUtilsKt.blitk(
                            poseStack,
                            PokedexScannerRendererImpl.infoFrameResource(isLeftSide, index),
                            centerX + xOffset,
                            centerY + yOffset,
                            frameHeight,
                            !isInnerFrame ? OUTER_INFO_FRAME_WIDTH : INNER_INFO_FRAME_WIDTH,
                            0,
                            Math.ceil(usageContext.getFocusIntervals()) * frameHeight,
                            !isInnerFrame ? OUTER_INFO_FRAME_WIDTH : INNER_INFO_FRAME_WIDTH,
                            frameHeight * 10,
                            0, 1, 1, 1, opacity, true, 1F
                    );

                    int xOffsetText = isInnerFrame ? (((INNER_INFO_FRAME_WIDTH - INNER_INFO_FRAME_STEM_WIDTH) / 2) + (isLeftSide ? 0 : INNER_INFO_FRAME_STEM_WIDTH)) : (OUTER_INFO_FRAME_WIDTH / 2);
                    int yOffsetText = switch (index) {
                        case 0 -> 5;
                        case 1 -> 4;
                        case 2 -> 8;
                        case 3 -> 42;
                        default -> 0;
                    };

                    // Text
                    if (usageContext.getFocusIntervals() == PokedexUsageContext.FOCUS_INTERVALS && usageContext.getScannableEntityInFocus() != null) {
                        PokedexEntityData pokedexEntityData = usageContext.getScannableEntityInFocus().resolvePokemonScan();
                        if (pokedexEntityData == null) continue;

                        if (infoDisplayedCounter == 1) {
                            RenderHelperKt.drawScaledText(
                                    graphics,
                                    CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                                    TextKt.bold(LocalizationUtilsKt.lang("ui.lv.number", pokedexEntityData.getPokemon().getLevel())),
                                    centerX + xOffset + xOffsetText,
                                    centerY + yOffset + yOffsetText,
                                    1F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null
                            );
                        }

                        if (infoDisplayedCounter == 2) {
                            boolean hasTrainer = false;
                            Object resolvedEntity = usageContext.getScannableEntityInFocus().resolveEntityScan();
                            if (resolvedEntity instanceof PokemonEntity pokemonEntity) {
                                hasTrainer = pokemonEntity.getOwnerUUID() != null;
                            }

                            MutableComponent speciesName = TextKt.bold(pokedexEntityData.getApparentSpecies().getTranslatedName());
                            int yOffsetName = hasTrainer ? 2 : 0;
                            if (hasTrainer) {
                                RenderHelperKt.drawScaledText(
                                        graphics,
                                        null,
                                        LocalizationUtilsKt.lang("ui.pokedex.scan.trainer_owned"),
                                        centerX + xOffset + xOffsetText,
                                        centerY + yOffset + yOffsetText - yOffsetName,
                                        0.5F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null
                                );
                            }
                            RenderHelperKt.drawScaledText(
                                    graphics,
                                    CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                                    speciesName,
                                    centerX + xOffset + xOffsetText,
                                    centerY + yOffset + yOffsetText + yOffsetName,
                                    1F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null
                            );

                            Gender gender = pokedexEntityData.getPokemon().getGender();
                            int speciesNameWidth = Minecraft.getInstance().font.width(TextKt.font(speciesName, CobblemonResources.INSTANCE.getDEFAULT_LARGE()));
                            if (gender != Gender.GENDERLESS) {
                                boolean isMale = gender == Gender.MALE;
                                MutableComponent textSymbol = TextKt.bold(TextKt.text(isMale ? "♂" : "♀"));
                                RenderHelperKt.drawScaledText(
                                        graphics,
                                        CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                                        textSymbol,
                                        centerX + xOffset + xOffsetText + 2 + (speciesNameWidth / 2),
                                        centerY + yOffset + yOffsetText + yOffsetName,
                                        1F, 1F, Integer.MAX_VALUE, isMale ? 0xFF32CBFF : 0xFFFC5454, false, true, null, null
                                );
                            }

                            if (usageContext.isPokemonInFocusOwned()) {
                                GuiUtilsKt.blitk(
                                        poseStack,
                                        BattleOverlay.Companion.getCaughtIndicator(),
                                        (centerX + xOffset + xOffsetText - 7 - (speciesNameWidth / 2)) / BattleOverlay.SCALE,
                                        (centerY + yOffset + yOffsetText + yOffsetName + 2) / BattleOverlay.SCALE,
                                        10, 10, 0, 0, 10, 10, 0, 1, 1, 1, 1F, true, BattleOverlay.SCALE
                                );
                            }
                        }

                        if (infoDisplayedCounter == 3) {
                            MutableComponent typeList = null;
                            for (ElementalType t : pokedexEntityData.getApparentForm().getTypes()) {
                                MutableComponent name = (MutableComponent) t.getDisplayName().copy();
                                if (typeList == null) {
                                    typeList = name;
                                } else {
                                    typeList = TextKt.plus(TextKt.plus(typeList, "/"), name);
                                }
                            }
                            MutableComponent typeText = TextKt.bold(LocalizationUtilsKt.lang("type.suffix", typeList != null ? typeList : TextKt.text("")));
                            int typeWidth = Minecraft.getInstance().font.width(TextKt.font(typeText, CobblemonResources.INSTANCE.getDEFAULT_LARGE()));
                            // Split into 2 lines if text width is too long
                            if (typeWidth > (OUTER_INFO_FRAME_WIDTH - 8) && pokedexEntityData.getApparentForm().getSecondaryType() != null) {
                                String[] splitLines = typeText.getString().split("/");
                                RenderHelperKt.drawScaledText(
                                        graphics,
                                        CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                                        TextKt.bold(TextKt.text(splitLines[0], "/")),
                                        centerX + xOffset + xOffsetText,
                                        centerY + yOffset + yOffsetText - 4,
                                        1F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null
                                );

                                RenderHelperKt.drawScaledText(
                                        graphics,
                                        CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                                        TextKt.bold(TextKt.text(splitLines[1])),
                                        centerX + xOffset + xOffsetText,
                                        centerY + yOffset + yOffsetText + 3,
                                        1F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null
                                );
                            } else {
                                RenderHelperKt.drawScaledText(
                                        graphics,
                                        CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                                        typeText,
                                        centerX + xOffset + xOffsetText,
                                        centerY + yOffset + yOffsetText,
                                        1F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null
                                );
                            }
                        }
                    }
                }
            }
        }
    }
}
