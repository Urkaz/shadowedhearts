package com.jayemceekay.shadowedhearts.client.gui;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.client.gui.pokedex.PokedexTooltipKt;
import com.jayemceekay.shadowedhearts.client.aura.effects.AuraInterferenceEffect;
import com.jayemceekay.shadowedhearts.client.aura.effects.AuraInterferenceRegistry;
import com.jayemceekay.shadowedhearts.client.gui.modes.*;
import com.jayemceekay.shadowedhearts.common.aura.AuraReaderCharge;
import com.jayemceekay.shadowedhearts.common.shadow.ShadowPokemonData;
import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs;
import com.jayemceekay.shadowedhearts.content.items.AuraReaderItem;
import com.jayemceekay.shadowedhearts.integration.accessories.SnagAccessoryBridgeHolder;
import com.jayemceekay.shadowedhearts.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Rendering-only class for the Aura Scanner HUD. All mutable state and logic
 * remain in AuraScannerHUD. This class reads from that state and renders.
 */
public final class AuraReaderRenderer {

    private static final Map<AuraReaderManager.AuraScannerMode, AuraScannerModeRenderer> MODE_RENDERERS = new EnumMap<>(AuraReaderManager.AuraScannerMode.class);

    static {
        MODE_RENDERERS.put(AuraReaderManager.AuraScannerMode.AURA_READER, new AuraReaderRendererImpl());
        MODE_RENDERERS.put(AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER, new PokedexScannerRendererImpl());
        MODE_RENDERERS.put(AuraReaderManager.AuraScannerMode.DOWSING_MACHINE, new DowsingMachineRendererImpl());
    }

    // Textures and rendering constants
    private static final ResourceLocation SCAN_RING_INNER = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/scan_ring_inner.png");
    private static final ResourceLocation SCAN_RING_OUTER = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/scan_ring_outer.png");
    private static final ResourceLocation SCAN_RING_MIDDLE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/scan_ring_middle.png");
    private static final ResourceLocation SCANLINES = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/overlay_scanlines.png");
    private static final ResourceLocation POINTER = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/battle/arrow_pointer_up.png");
    private static final ResourceLocation SELECT_ARROW = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/select_arrow.png");
    private static final ResourceLocation SCAN_OVERLAY_CORNERS = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/overlay_corners.png");
    private static final ResourceLocation SCAN_OVERLAY_TOP = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/overlay_border_top.png");
    private static final ResourceLocation SCAN_OVERLAY_BOTTOM = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/overlay_border_bottom.png");
    private static final ResourceLocation SCAN_OVERLAY_LEFT = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/overlay_border_left.png");
    private static final ResourceLocation SCAN_OVERLAY_RIGHT = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/overlay_border_right.png");
    private static final ResourceLocation SCAN_OVERLAY_NOTCH = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/overlay_notch.png");
    private static final ResourceLocation UNKNOWN_MARK = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/scan_unknown.png");

    private static ResourceLocation getInfoFrameResource(boolean isLeft, int tier) {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/scan_info_frame_" + (isLeft ? "left" : "right") + "_" + tier + ".png");
    }

    private static final int SCAN_OVERLAY_NOTCH_WIDTH = 200;
    private static final ResourceLocation CENTER_INFO_FRAME = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/scan/scan_info_frame.png");
    private static final int CENTER_INFO_FRAME_WIDTH = 128;
    private static final int CENTER_INFO_FRAME_HEIGHT = 16;
    private static final int OUTER_INFO_FRAME_WIDTH = 92;
    private static final int OUTER_INFO_FRAME_HEIGHT = 55;
    private static final int INNER_INFO_FRAME_WIDTH = 120;
    private static final int INNER_INFO_FRAME_HEIGHT = 20;
    private static final int INNER_INFO_FRAME_STEM_WIDTH = 28;

    private static ItemStack directionArrowStack;

    private static ItemStack getDirectionArrowStack() {
        if (directionArrowStack == null) {
            directionArrowStack = new ItemStack(ModItems.DIRECTION_ARROW.get());
        }
        return directionArrowStack;
    }

    private AuraReaderRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        float baseAlpha = Mth.lerp(partialTick, AbstractModeLogic.prevFadeAmount, AbstractModeLogic.fadeAmount);
        if (baseAlpha <= 0.0f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        float time = deltaTracker.getRealtimeDeltaTicks();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float renderAlpha = baseAlpha;
        float currentBootTimer = Mth.lerp(partialTick, AbstractModeLogic.prevBootTimer, AbstractModeLogic.bootTimer);
        if (currentBootTimer > 0) {
            if ((int) (currentBootTimer * 20) % 2 == 0) renderAlpha *= 0.5f;
        }

        float currentGlitchTimer = Mth.lerp(partialTick, AbstractModeLogic.prevGlitchTimer, AbstractModeLogic.glitchTimer);
        if (currentGlitchTimer > 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate((mc.level.random.nextFloat() - 0.5f) * 10, (mc.level.random.nextFloat() - 0.5f) * 10, 0);
            if (mc.level.random.nextFloat() < 0.3f) renderAlpha *= 0.5f;
        }

        guiGraphics.pose().pushPose();
        float yOffset = (1.0f - baseAlpha) * -height;
        guiGraphics.pose().translate(0, yOffset, 0);

        AuraScannerModeRenderer modeRenderer = MODE_RENDERERS.get(AuraReaderManager.currentMode);
        if (modeRenderer != null) {
            modeRenderer.render(guiGraphics, width, height, renderAlpha, time, partialTick);
        }

        float modeMenuAlpha = Mth.lerp(partialTick, AbstractModeLogic.prevModeMenuAlpha, AbstractModeLogic.modeMenuAlpha);
        if (modeMenuAlpha > 0) {
            renderRadialMenu(guiGraphics, width / 2, height / 2, modeMenuAlpha, partialTick);
        }

        guiGraphics.pose().popPose();

        if (currentGlitchTimer > 0) {
            guiGraphics.pose().popPose();
        }

        RenderSystem.disableBlend();
    }

    private static void renderRadialMenu(GuiGraphics guiGraphics, int centerX, int centerY, float alpha, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        AuraReaderManager.AuraScannerMode hovered = null;
        if (mc.screen instanceof AuraReaderModeScreen screen) {
            hovered = screen.getHoveredMode();
        }


        // 3 modes around the center
        // Aura Reader (Top), Pokedex Scanner (Bottom Left), Dowsing Machine (Bottom Right)
        drawMode(guiGraphics, centerX, centerY - 80, Component.translatable("aura_scanner.mode.aura_reader"), AuraReaderManager.currentMode == AuraReaderManager.AuraScannerMode.AURA_READER, hovered == AuraReaderManager.AuraScannerMode.AURA_READER, alpha);
        drawMode(guiGraphics, centerX - 80, centerY + 60, Component.translatable("aura_scanner.mode.pokedex_scanner"), AuraReaderManager.currentMode == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER, hovered == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER, alpha);
        drawMode(guiGraphics, centerX + 80, centerY + 60, Component.translatable("aura_scanner.mode.dowsing_machine"), AuraReaderManager.currentMode == AuraReaderManager.AuraScannerMode.DOWSING_MACHINE, hovered == AuraReaderManager.AuraScannerMode.DOWSING_MACHINE, alpha);
    }

    private static void drawMode(GuiGraphics guiGraphics, int x, int y, MutableComponent text, boolean current, boolean hovered, float alpha) {
        int color = 0x00FFFF; // Cyan
        if (hovered) color = 0xFFFFFF; // White
        if (current) color = 0xFFA500; // Orange

        PokedexTooltipKt.renderTooltip(guiGraphics, text.withStyle(Style.EMPTY.withColor(color).withBold(true)), x, y, 1.0f,0);
    }

    private static void renderChargeBar(GuiGraphics guiGraphics, int width, int height, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack auraReader = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(mc.player);
        if (auraReader.isEmpty() || !(auraReader.getItem() instanceof AuraReaderItem))
            return;

        int charge = AuraReaderCharge.get(auraReader);
        float chargeRatio = (float) charge / (float) AuraReaderItem.MAX_CHARGE;

        int barWidth = 100;
        int barHeight = 4;
        int x = (width - barWidth) / 2;
        int y = height / 2 + 91;

        int textAlpha = (int) (alpha * 255) << 24;

        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xAA000000 | (textAlpha & 0xFF000000));
        int fillWidth = (int) (barWidth * chargeRatio);
        int color = 0x00FFFF;
        if (chargeRatio < 0.2f) color = 0xFF0000;
        guiGraphics.fill(x, y, x + fillWidth, y + barHeight, color | textAlpha);

        String chargeText = String.format("CHARGE: %d%%", (int) (chargeRatio * 100));
        guiGraphics.drawCenteredString(mc.font, chargeText, width / 2, y - 10, color | textAlpha);
    }

    private static void renderOperationalTempBar(GuiGraphics guiGraphics, int width, int height, float alpha, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack auraReader = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(mc.player);
        if (auraReader.isEmpty() || !(auraReader.getItem() instanceof AuraReaderItem))
            return;

        float tempC = AuraReaderItem.getOperationalTempC(auraReader);
        float minC = AuraReaderItem.getMinOperatingTempC(auraReader);
        float maxC = AuraReaderItem.getMaxOperatingTempC(auraReader);
        if (maxC <= minC + 0.001f) return;

        int barWidth = 100;
        int barHeight = 4;
        int centerX = width / 2;
        int x = centerX - (barWidth / 2);
        int y = height / 2 + 72;

        int textAlpha = (int) (alpha * 255) << 24;

        // Background frame
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xAA000000 | (textAlpha & 0xFF000000));

        // Markers at 0°C and 75°C
        int zeroOff = (int) ((0 - minC) / (maxC - minC) * barWidth);
        int stableOff = (int) ((75.0f - minC) / (maxC - minC) * barWidth);
        int zeroX = x + Mth.clamp(zeroOff, 0, barWidth);
        int stableX = x + Mth.clamp(stableOff, 0, barWidth);


        // Fill up to current temp
        float currentTempC = Mth.lerp(partialTick, AbstractModeLogic.prevOperationalTempC, AbstractModeLogic.operationalTempC);
        float ratio = Mth.clamp((currentTempC - minC) / (maxC - minC), 0.0f, 1.0f);
        int fillWidth = (int) (barWidth * ratio);
        int color = colorForTempC(currentTempC, minC, maxC) | textAlpha;
        guiGraphics.fill(x, y, x + fillWidth, y + barHeight, color);
        guiGraphics.fill(zeroX - 1, y - 2, zeroX + 1, y + barHeight + 2, (0x666666 | textAlpha));
        guiGraphics.fill(stableX - 1, y - 2, stableX + 1, y + barHeight + 2, (0x888888 | textAlpha));
        // Label
        String state;
        if (currentTempC < 0.0f) state = "COLD INSTABILITY";
        else if (currentTempC < 75.0f) state = "STABLE";
        else state = "HEAT INSTABILITY";
        boolean useF = ShadowedHeartsConfigs.getInstance().getClientConfig().useFahrenheitDisplay();
        float displayTemp = useF ? (currentTempC * 9.0f / 5.0f + 32.0f) : currentTempC;
        String unit = useF ? "°F" : "°C";
        Component label = Component.literal(String.format("TEMP: %s  (%.1f%s)", state, displayTemp, unit));
        StringWidget labelWidget = new StringWidget(label, mc.font);
        labelWidget.setX(centerX - labelWidget.getWidth() / 2);
        labelWidget.setY(y - 10);
        labelWidget.setColor(0xFFFFFF | textAlpha);
        labelWidget.renderWidget(guiGraphics, width, height, partialTick);
        //guiGraphics.drawCenteredString(mc.font, label, centerX, y - 10, (0xFFFFFF | textAlpha));
    }

    private static int colorForTempC(float tempC, float minC, float maxC) {
        // Blue for <0°C → green near mid of stable → red near max
        if (tempC < 0.0f) {
            float t = Mth.clamp((0.0f - tempC) / Math.max(1.0f, 0.0f - minC), 0.0f, 1.0f);
            int r = (int) Mth.lerp(t, 0x20, 0x00);
            int g = (int) Mth.lerp(t, 0xC0, 0xA0);
            int b = 0xFF;
            return (r << 16) | (g << 8) | b;
        } else if (tempC <= 75.0f) {
            float t = tempC / 75.0f; // 0..1 across stable band
            int r = (int) Mth.lerp(t, 0x20, 0x40);
            int g = (int) Mth.lerp(t, 0xE0, 0xFF);
            int b = (int) Mth.lerp(t, 0x80, 0x40);
            return (r << 16) | (g << 8) | b;
        } else {
            float t = Mth.clamp((tempC - 75.0f) / Math.max(1.0f, maxC - 75.0f), 0.0f, 1.0f);
            int r = (int) Mth.lerp(t, 0xFF, 0xFF);
            int g = (int) Mth.lerp(t, 0xA0, 0x40);
            int b = (int) Mth.lerp(t, 0x40, 0x20);
            return (r << 16) | (g << 8) | b;
        }
    }



    // Optional info pane (currently not invoked; retained for parity)
    @SuppressWarnings("unused")
    private static void renderInfoPane(GuiGraphics guiGraphics, int width, int height, float alpha, float partialTick) {
        float currentScanningProgress = Mth.lerp(partialTick, AuraReaderLogic.prevScanningProgress, AuraReaderLogic.scanningProgress);
        if (currentScanningProgress <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (AuraReaderLogic.scannedPokemon == null) return;

        int centerX = width / 2;
        int centerY = height / 2;

        renderFrame(guiGraphics, centerX, centerY, true, 0, alpha);
        if (currentScanningProgress > 0.25f) {
            int level = AuraReaderLogic.scannedPokemon.getPokemon().getLevel();
            drawFrameText(guiGraphics, mc, "LV. " + level, centerX, centerY, true, 0, alpha);
        }

        renderFrame(guiGraphics, centerX, centerY, false, 1, alpha);
        if (currentScanningProgress > 0.5f) {
            String name = AuraReaderLogic.scannedPokemon.getPokemon().getSpecies().getName().toUpperCase();
            drawFrameText(guiGraphics, mc, name, centerX, centerY, false, 1, alpha);
        }

        renderFrame(guiGraphics, centerX, centerY, true, 3, alpha);
        if (currentScanningProgress > 0.75f) {
            float corruption = ShadowPokemonData.getHeartGauge(AuraReaderLogic.scannedPokemon);
            String status = corruption > 0.8f ? "ELITE" : "COMMON";
            if (AuraReaderLogic.scannedPokemon.getPokemon().isLegendary())
                status = "LEGENDARY";
            drawFrameText(guiGraphics, mc, status, centerX, centerY, true, 3, alpha);
        }
    }

    private static void renderFrame(GuiGraphics guiGraphics, int centerX, int centerY, boolean isLeft, int tier, float alpha) {
        ResourceLocation tex = getInfoFrameResource(isLeft, tier);
        boolean isInner = tier == 1 || tier == 2;
        int frameWidth = isInner ? INNER_INFO_FRAME_WIDTH : OUTER_INFO_FRAME_WIDTH;
        int frameHeight = isInner ? INNER_INFO_FRAME_HEIGHT : OUTER_INFO_FRAME_HEIGHT;

        int xOffset = (isInner ? -177 : -120) + (isLeft ? 0 : (isInner ? 234 : 148));
        int yOffset = switch (tier) {
            case 0 -> -80;
            case 1 -> -26;
            case 2 -> 6;
            case 3 -> 25;
            default -> 0;
        };

        RenderSystem.setShaderColor(0.0f, 1.0f, 1.0f, alpha);
        guiGraphics.blit(tex, centerX + xOffset, centerY + yOffset, 0, 0, frameWidth, frameHeight, frameWidth, frameHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawFrameText(GuiGraphics guiGraphics, Minecraft mc, String text, int centerX, int centerY, boolean isLeft, int tier, float alpha) {
        boolean isInner = tier == 1 || tier == 2;
        int xOffset = (isInner ? -177 : -120) + (isLeft ? 0 : (isInner ? 234 : 148));
        int yOffset = switch (tier) {
            case 0 -> -80;
            case 1 -> -26;
            case 2 -> 6;
            case 3 -> 25;
            default -> 0;
        };

        int xOffsetText = isInner ? (((INNER_INFO_FRAME_WIDTH - INNER_INFO_FRAME_STEM_WIDTH) / 2) + (isLeft ? 0 : INNER_INFO_FRAME_STEM_WIDTH)) : (OUTER_INFO_FRAME_WIDTH / 2);
        int yOffsetText = switch (tier) {
            case 0 -> 15;
            case 1 -> 6;
            case 2 -> 6;
            case 3 -> 35;
            default -> 0;
        };

        if (AbstractModeLogic.glitchTimer > 0 && mc.level.random.nextFloat() < 0.2f) {
            text = "########";
        }

        int textAlpha = (int) (alpha * 255) << 24;
        guiGraphics.drawCenteredString(mc.font, text, centerX + xOffset + xOffsetText, centerY + yOffset + yOffsetText, 0x00FFFF | textAlpha);
    }

    private static void renderInterferenceOverlay(GuiGraphics guiGraphics, int width, int height, float partialTick) {
        if (!AbstractModeLogic.active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        AuraReaderLogic.SignalTarget target = AuraReaderLogic.lockedTarget;
        if (target == null && AuraReaderLogic.acquisitionMode && !AuraReaderLogic.CURRENT_SIGNALS.isEmpty()) {
            int idx = Math.max(0, Math.min(AuraReaderLogic.selectedSignalIndex, AuraReaderLogic.CURRENT_SIGNALS.size() - 1));
            target = AuraReaderLogic.CURRENT_SIGNALS.get(idx);
        } else if (target == null && !AuraReaderLogic.CURRENT_SIGNALS.isEmpty()) {
            target = AuraReaderLogic.CURRENT_SIGNALS.stream().max(Comparator.comparingDouble(s -> s.strength)).orElse(null);
        }

        if (target == null) return;

        float maxRange = (target.type == AuraReaderLogic.TargetType.METEOROID)
                ? ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerMeteoroidRange()
                : ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerShadowRange();

        double dynamicDistance = -1.0;
        if (target.type == AuraReaderLogic.TargetType.POKEMON) {
            for (Entity cand : mc.level.entitiesForRendering()) {
                if (cand.getUUID().equals(target.pokemonId)) {
                    dynamicDistance = cand.distanceTo(mc.player);
                    break;
                }
            }
        } else if (target.meteoroidPos != null) {
            dynamicDistance = Math.sqrt(target.meteoroidPos.distSqr(mc.player.blockPosition()));
        }

        float base = target.strength;
        if (dynamicDistance >= 0.0) {
            base = (float) Math.max(0.0, 1.0 - (dynamicDistance / maxRange));
        } else if (target.distance > 0.0) {
            base = (float) Math.max(0.0, 1.0 - (target.distance / maxRange));
        }
        base = Mth.clamp(base, 0.0f, 1.0f);
        float intensity = base * base * base;
        if (intensity < 0.02f) return;

        ElementalType element = resolveElementForTarget(target);
        AuraInterferenceEffect effect = AuraInterferenceRegistry.get(element);
        if (effect == null && element != ElementalTypes.NORMAL) {
            effect = AuraInterferenceRegistry.get(ElementalTypes.NORMAL);
        }
        if (effect != null) {
            effect.render(guiGraphics, width, height, partialTick, intensity, mc);
        }
    }

    private static ElementalType resolveElementForTarget(AuraReaderLogic.SignalTarget target) {
        return (target.type == AuraReaderLogic.TargetType.POKEMON) ? ElementalTypes.GHOST : ElementalTypes.NORMAL;
    }
}
