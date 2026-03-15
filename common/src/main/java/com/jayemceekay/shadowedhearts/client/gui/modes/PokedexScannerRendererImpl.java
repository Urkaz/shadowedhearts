package com.jayemceekay.shadowedhearts.client.gui.modes;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation;
import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.cobblemon.mod.common.client.render.RenderHelperKt;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import com.jayemceekay.shadowedhearts.client.gui.AuraReaderManager;
import com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

public class PokedexScannerRendererImpl extends AbstractModeRenderer {

    public static ResourceLocation infoFrameResource(boolean isLeft, int tier) {
        return MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_info_frame_" + (isLeft ? "left" : "right") + "_" + tier + ".png");
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

        float progressOpacity = opacity;
        int segments = 40;
        PokedexUsageContext usageContext = AuraReaderManager.POKEDEX_USAGE_CONTEXT;
        if (usageContext.getScanningProgress() > 0) {
            if (usageContext.getScanningProgress() < 20) {
                progressOpacity -= usageContext.getScanningProgress() * 0.05F;
            } else {
                if (progressOpacity != opacity) progressOpacity = opacity;
                segments = (int) Math.floor((usageContext.getScanningProgress() - 20.0) / 2.0);
            }
        }

        for (int i = 0; i < segments; i++) {
            Quaternionf rotationQuaternion = new Quaternionf().rotateZ((float) Math.toRadians((i * 4.5) + (rotation * 0.5)));
            poseStack.pushPose();
            poseStack.mulPose(rotationQuaternion);
            GuiUtilsKt.blitk(poseStack, SCAN_RING_MIDDLE, -(SCAN_RING_MIDDLE_WIDTH / 2), -(SCAN_RING_MIDDLE_HEIGHT / 2F), SCAN_RING_MIDDLE_HEIGHT, SCAN_RING_MIDDLE_WIDTH, 0, 0, SCAN_RING_MIDDLE_WIDTH, SCAN_RING_MIDDLE_HEIGHT, 0, 1, 1, 1, progressOpacity, true, 1F);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(-AbstractModeLogic.innerRingRotation)));
        GuiUtilsKt.blitk(poseStack, SCAN_RING_INNER, -(SCAN_RING_INNER_DIAMETER / 2), -(SCAN_RING_INNER_DIAMETER / 2), SCAN_RING_INNER_DIAMETER, SCAN_RING_INNER_DIAMETER, 0, 0, SCAN_RING_INNER_DIAMETER, SCAN_RING_INNER_DIAMETER, 0, 1, 1, 1, opacity, true, 1F);
        poseStack.popPose();

        poseStack.popPose();
    }

    public MutableComponent getRegisterText(PokedexLearnedInformation info) {
        MutableComponent type = switch (info) {
            case FORM -> LocalizationUtilsKt.lang("ui.pokedex.info.form");
            case VARIATION -> LocalizationUtilsKt.lang("ui.pokedex.info.variation");
            default -> LocalizationUtilsKt.lang("ui.pokemon");
        };
        return TextKt.bold(LocalizationUtilsKt.lang("ui.pokedex.scan.registered_suffix", type));
    }

    @Override
    public void renderScanOverlay(GuiGraphics graphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = graphics.pose();
        PokedexUsageContext usageContext = AuraReaderManager.POKEDEX_USAGE_CONTEXT;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();

        // Scanning overlay
        float opacity =  1F;
        // Draw scan lines
        double interlacePos = Math.ceil((AbstractModeLogic.usageIntervals % 14) * 0.5) * 0.5;
        for (int i = 0; i < screenHeight; i++) {
            if (i % 4 == 0) GuiUtilsKt.blitk(matrices, SCAN_OVERLAY_LINES, 0, i - interlacePos, 4, screenWidth, 0, 0, screenWidth, 4, 0, 1, 1, 1, opacity, true, 1F);
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

        // Scan info frame
        renderInfoFrames(graphics, matrices, usageContext, centerX, centerY, opacity);

        // Scan rings
        renderScanRings(matrices, centerX, centerY, opacity);

        if (usageContext.getDisplayRegisterInfoIntervals() > 0) {
            GuiUtilsKt.blitk(
                    matrices,
                    CENTER_INFO_FRAME,
                    centerX - (CENTER_INFO_FRAME_WIDTH / 2),
                    centerY - (CENTER_INFO_FRAME_HEIGHT / 2),
                    CENTER_INFO_FRAME_HEIGHT,
                    CENTER_INFO_FRAME_WIDTH,
                    0,
                    Math.min(Math.ceil(usageContext.getDisplayRegisterInfoIntervals()), PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS) * CENTER_INFO_FRAME_HEIGHT,
                    CENTER_INFO_FRAME_WIDTH,
                    CENTER_INFO_FRAME_HEIGHT * 6,
                    0, 1, 1, 1, opacity, true, 1F
            );

            if (usageContext.getDisplayRegisterInfoIntervals() >= PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS) {
                RenderHelperKt.drawScaledText(
                        graphics,
                        CobblemonResources.INSTANCE.getDEFAULT_LARGE(), 
                        getRegisterText(usageContext.getNewPokemonInfo()),
                        centerX,
                        centerY - (CENTER_INFO_FRAME_HEIGHT / 2) + 4,
                        1F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null
                );
            }
        } else {
            if (usageContext.getScanningProgress() > 0) {
                // If scan progress reaches max ticks - 10, decrement opacity using last 10 ticks
                // else increment opacity using scan progress
                float centerOpacity = (
                        usageContext.getScanningProgress() > (PokedexUsageContext.MAX_SCAN_PROGRESS - 10) ?
                                (PokedexUsageContext.MAX_SCAN_PROGRESS - usageContext.getScanningProgress()) :
                                usageContext.getScanningProgress()
                ) * 0.1F;

                GuiUtilsKt.blitk(
                        matrices,
                        UNKNOWN_MARK,
                        centerX - (UNKNOWN_MARK_WIDTH / 2),
                        centerY - (UNKNOWN_MARK_HEIGHT / 2) + 2,
                        UNKNOWN_MARK_HEIGHT,
                        UNKNOWN_MARK_WIDTH,
                        0, 0, UNKNOWN_MARK_WIDTH, UNKNOWN_MARK_HEIGHT, 0, 1, 1, 1, Math.max(0F, Math.min(opacity, centerOpacity)), true, 1F
                );

                matrices.pushPose();
                matrices.translate(centerX, centerY, 0.0f);

                matrices.pushPose();
                matrices.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(AbstractModeLogic.innerRingRotation * 0.5)));
                GuiUtilsKt.blitk(
                        matrices,
                        POINTER,
                        -POINTER_WIDTH - POINTER_OFFSET,
                        -(POINTER_HEIGHT / 2),
                        POINTER_HEIGHT,
                        POINTER_WIDTH,
                        0, 0, POINTER_WIDTH * 2, POINTER_HEIGHT, 0, 1, 1, 1, Math.max(0F, Math.min(opacity, centerOpacity)), true, 1F
                );

                GuiUtilsKt.blitk(
                        matrices,
                        POINTER,
                        POINTER_OFFSET,
                        -(POINTER_HEIGHT / 2),
                        POINTER_HEIGHT,
                        POINTER_WIDTH,
                        POINTER_WIDTH, 0, POINTER_WIDTH * 2, POINTER_HEIGHT, 0, 1, 1, 1, Math.max(0F, Math.min(opacity, centerOpacity)), true, 1F
                );
                matrices.popPose();
                matrices.popPose();
            } else if (usageContext.getViewInfoTicks() > 0) {
                float pointerOpacity = usageContext.getViewInfoTicks() * 0.1F;

                GuiUtilsKt.blitk(
                        matrices,
                        POINTER,
                        centerX - POINTER_WIDTH - POINTER_OFFSET + usageContext.getViewInfoTicks(),
                        centerY - (POINTER_HEIGHT / 2),
                        POINTER_HEIGHT,
                        POINTER_WIDTH,
                        0, 0, POINTER_WIDTH * 2, POINTER_HEIGHT, 0, 1, 1, 1, Math.max(0F, Math.min(opacity, pointerOpacity)), true, 1F
                );

                GuiUtilsKt.blitk(
                        matrices,
                        POINTER,
                        centerX + POINTER_OFFSET - usageContext.getViewInfoTicks(),
                        centerY - (POINTER_HEIGHT / 2),
                        POINTER_HEIGHT,
                        POINTER_WIDTH,
                        POINTER_WIDTH, 0, POINTER_WIDTH * 2, POINTER_HEIGHT, 0, 1, 1, 1, Math.max(0F, Math.min(opacity, pointerOpacity)), true, 1F
                );
            }
        }

        RenderSystem.disableBlend();
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int width,
            int height,
            float alpha,
            float time,
            float partialTick
    ) {
        AbstractModeLogic.updateAnimations(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks());
        renderScanOverlay(guiGraphics, partialTick);
        AuraReaderManager.POKEDEX_USAGE_CONTEXT.renderUpdate(guiGraphics,
                Minecraft.getInstance().getTimer());
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
