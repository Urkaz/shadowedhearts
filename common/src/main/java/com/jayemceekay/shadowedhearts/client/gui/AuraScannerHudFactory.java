package com.jayemceekay.shadowedhearts.client.gui;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.cobblemon.mod.common.client.render.RenderHelperKt;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import com.jayemceekay.fluxui.hud.animation.Animator;
import com.jayemceekay.fluxui.hud.core.HudContext;
import com.jayemceekay.fluxui.hud.core.HudNode;
import com.jayemceekay.fluxui.hud.layout.Anchor;
import com.jayemceekay.fluxui.hud.layout.LayoutNode;
import com.jayemceekay.fluxui.hud.state.StateBindings;
import com.jayemceekay.fluxui.hud.widgets.*;
import com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext;
import com.jayemceekay.shadowedhearts.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class AuraScannerHudFactory {
    public static final ResourceLocation SCAN_RING_OUTER = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_ring_outer.png");
    public static final ResourceLocation SCAN_RING_MIDDLE = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_ring_middle.png");
    public static final ResourceLocation SCAN_RING_INNER = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_ring_inner.png");
    public static final ResourceLocation SCAN_SCREEN = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/pokedex_screen_scan.png");
    public static final ResourceLocation SCAN_OVERLAY_CORNERS = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_corners.png");
    public static final ResourceLocation SCAN_OVERLAY_TOP = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_border_top.png");
    public static final ResourceLocation SCAN_OVERLAY_BOTTOM = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_border_bottom.png");
    public static final ResourceLocation SCAN_OVERLAY_LEFT = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_border_left.png");
    public static final ResourceLocation SCAN_OVERLAY_RIGHT = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_border_right.png");
    public static final ResourceLocation SCAN_OVERLAY_LINES = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_scanlines.png");
    public static final ResourceLocation SCAN_OVERLAY_NOTCH = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/overlay_notch.png");
    public static final ResourceLocation CENTER_INFO_FRAME = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_info_frame.png");
    public static final ResourceLocation UNKNOWN_MARK = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/scan_unknown.png");
    public static final ResourceLocation POINTER = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/pointer.png");
    public static final ResourceLocation TOOLTIP_EDGE = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/tooltip_edge.png");
    public static final ResourceLocation TOOLTIP_BACKGROUND = com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/tooltip_background.png");

    public static final int CENTER_INFO_FRAME_WIDTH = 128;
    public static final int CENTER_INFO_FRAME_HEIGHT = 16;
    public static final int OUTER_INFO_FRAME_WIDTH = 92;
    public static final int OUTER_INFO_FRAME_HEIGHT = 55;
    public static final int INNER_INFO_FRAME_WIDTH = 120;
    public static final int INNER_INFO_FRAME_HEIGHT = 20;
    public static final int INNER_INFO_FRAME_STEM_WIDTH = 28;
    public static final int SCAN_OVERLAY_NOTCH_WIDTH = 200;

    public static ResourceLocation infoFrameResource(boolean isLeft, int tier) {
        return com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource("textures/gui/pokedex/scan/info_frame_" + (isLeft ? "left" : "right") + "_" + tier + ".png");
    }

    public static net.minecraft.network.chat.MutableComponent getRegisterText(com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation info) {
        if (info == com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation.SPECIES) {
            return com.cobblemon.mod.common.util.LocalizationUtilsKt.lang("ui.pokedex.scan.registered_pokemon");
        } else {
            return com.cobblemon.mod.common.util.LocalizationUtilsKt.lang("ui.pokedex.scan.registered_aspect");
        }
    }

    public static HudNode create(AuraScannerHudState state, Animator animator) {
        GroupNode root = new GroupNode();
        StateBindings.bindVisible(root, state.active);
        StateBindings.bindOpacity(root, state.fadeAmount);

        // Jitter for glitch effects
        JitterNode glitchWrapper = new JitterNode();
        StateBindings.bindJitter(glitchWrapper, state.glitchTimer);
        root.addChild(glitchWrapper);

        // Mode Switcher
        SwitchNode<AuraReaderManager.AuraScannerMode> modeSwitch = new SwitchNode<>();
        StateBindings.bindSwitch(modeSwitch, state.mode);
        glitchWrapper.addChild(modeSwitch);

        // --- AURA READER MODE ---
        GroupNode auraReaderGroup = createAuraReaderMode(state, animator);
        modeSwitch.addCase(AuraReaderManager.AuraScannerMode.AURA_READER, auraReaderGroup);

        // --- POKEDEX SCANNER MODE ---
        GroupNode pokedexScannerGroup = createPokedexScannerMode(state, animator);
        modeSwitch.addCase(AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER, pokedexScannerGroup);

        // --- DOWSING MACHINE MODE ---
        GroupNode dowsingMachineGroup = createDowsingMachineMode(state, animator);
        modeSwitch.addCase(AuraReaderManager.AuraScannerMode.DOWSING_MACHINE, dowsingMachineGroup);

        // --- COMMON OVERLAYS ---
        // Energy Bar
        LayoutNode energyBarLayout = new LayoutNode();
        energyBarLayout.setAnchor(Anchor.BOTTOM_CENTER);
        energyBarLayout.setOffsetY(-40f);
        BarMeterNode energyBar = new BarMeterNode();
        energyBar.setWidth(100f);
        energyBar.setHeight(4f);
        energyBar.setX(-50f);
        StateBindings.bindFloat(energyBar::setFillAmount, state.charge.map(c -> c / (float) com.jayemceekay.shadowedhearts.content.items.AuraReaderItem.MAX_CHARGE));
        energyBarLayout.addChild(energyBar);
        glitchWrapper.addChild(energyBarLayout);

        // Radial Menu moved to AuraReaderModeScreen
        // setupRadialMenu(glitchWrapper, state);

        return root;
    }

    private static void setupRadialMenu(HudNode parent, AuraScannerHudState state) {
        LayoutNode menuLayout = new LayoutNode();
        menuLayout.setAnchor(Anchor.CENTER);
        StateBindings.bindOpacity(menuLayout, state.modeMenuAlpha);
        parent.addChild(menuLayout);

        // Aura Reader (Top)
        addModeNode(menuLayout, 0, -80, "aura_scanner.mode.aura_reader", AuraReaderManager.AuraScannerMode.AURA_READER, state);
        // Pokedex Scanner (Bottom Left)
        addModeNode(menuLayout, -80, 60, "aura_scanner.mode.pokedex_scanner", AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER, state);
        // Dowsing Machine (Bottom Right)
        addModeNode(menuLayout, 80, 60, "aura_scanner.mode.dowsing_machine", AuraReaderManager.AuraScannerMode.DOWSING_MACHINE, state);
    }

    private static void addModeNode(LayoutNode parent, float x, float y, String translationKey, AuraReaderManager.AuraScannerMode mode, AuraScannerHudState state) {
        PokedexTooltipNode text = new PokedexTooltipNode(net.minecraft.network.chat.Component.translatable(translationKey), 0);
        text.setX(x);
        text.setY(y);

        Runnable apply = () -> {
            AuraReaderManager.AuraScannerMode selected = state.mode.get();
            AuraReaderManager.AuraScannerMode hovered = state.hoveredMode.get();
            int baseRGB = (selected == mode) ? 0xFFA500 /* orange */ : 0x00FFFF /* cyan */;
            int rgb = (hovered == mode) ? 0xFFFFFF /* white on hover */ : baseRGB;
            text.setBaseColor(baseRGB);
            // Apply the current visual color immediately
            text.getText().setStyle(text.getText().getStyle().withColor(rgb));
        };

        state.mode.subscribe((o, n) -> apply.run());
        state.hoveredMode.subscribe((o, n) -> apply.run());
        apply.run();

        parent.addChild(text);
    }

    private static class WaveformNode extends HudNode {
        private final AuraScannerHudState state;
        private final int color;

        public WaveformNode(AuraScannerHudState state, int color) {
            this.state = state;
            this.color = color;
        }

        @Override
        protected void renderSelf(HudContext ctx, GuiGraphics graphics) {
            float intensity = state.maxIntensity.get();
            if (intensity <= 0) return;

            float alpha = state.fadeAmount.get();
            int textAlpha = (int) (alpha * 255) << 24;
            float partialTick = ctx.partialTick();
            float waveTime = (Minecraft.getInstance().level.getGameTime() + partialTick) * 0.5f;

            for (int i = 0; i < 20; i++) {
                float h = (float) Math.sin(waveTime + i * 0.5f) * 10 * intensity;
                if (state.glitchTimer.get() > 0)
                    h *= Minecraft.getInstance().level.random.nextFloat() * 2;
                graphics.fill(i * 5, 0, i * 5 + 2, -(int) h, (color & 0xFFFFFF) | (textAlpha / 2));
            }
        }
    }

    private static class DirectionalPointersNode extends HudNode {
        private final AuraScannerHudState state;
        private static final ResourceLocation POINTER = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/battle/arrow_pointer_up.png");
        private static final ResourceLocation SELECT_ARROW = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/select_arrow.png");

        public DirectionalPointersNode(AuraScannerHudState state) {
            this.state = state;
        }

        @Override
        protected void renderSelf(HudContext ctx, GuiGraphics graphics) {
            java.util.List<AuraScannerHudState.DirectionalPointer> pointers = state.directionalPointers.get();
            float alpha = state.fadeAmount.get();

            for (AuraScannerHudState.DirectionalPointer p : pointers) {
                graphics.pose().pushPose();
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation(p.angle()));

                if (p.isLocked()) {
                    RenderSystem.setShaderColor(1.0f, 0.4f, 1.0f, Math.max(0.6f, alpha * p.intensity()));
                } else if (p.isSelected()) {
                    RenderSystem.setShaderColor(0.9f, 0.6f, 1.0f, Math.max(0.5f, alpha * p.intensity()));
                } else if (p.isMeteoroid()) {
                    RenderSystem.setShaderColor(0.8f, 0.2f, 0.9f, alpha * p.intensity());
                } else {
                    RenderSystem.setShaderColor(0.6f, 0.3f, 1.0f, alpha * p.intensity());
                }

                graphics.blit(POINTER, -8, -70, 0, 0, 16, 6, 16, 16);

                if (p.isSelected() && state.lockedTarget.get() == null) {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                    graphics.blit(SELECT_ARROW, -8, -82, 0, 0, 16, 16, 16, 16);
                }

                graphics.pose().popPose();
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static class InfoFramesNode extends HudNode {
        private final AuraScannerHudState state;

        public InfoFramesNode(AuraScannerHudState state) {
            this.state = state;
        }

        @Override
        protected void renderSelf(HudContext ctx, GuiGraphics graphics) {
            PokedexUsageContext usageContext = state.pokedexUsageContext.get();
            if (usageContext == null) return;
            float alpha = state.fadeAmount.get();
            renderInfoFrames(graphics, graphics.pose(), usageContext, 0, 0, alpha);
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
                                infoFrameResource(isLeftSide, index),
                                centerX + xOffset,
                                centerY + yOffset,
                                frameHeight,
                                !isInnerFrame ? OUTER_INFO_FRAME_WIDTH : INNER_INFO_FRAME_WIDTH,
                                0,
                                (int) (Math.ceil(usageContext.getFocusIntervals()) * frameHeight),
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
                                    MutableComponent textSymbol = TextKt.bold(net.minecraft.network.chat.Component.literal(isMale ? "♂" : "♀"));
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
                        }
                    }
                }
            }
        }
    }

    private static class ScanlinesNode extends HudNode {
        private final AuraScannerHudState state;

        public ScanlinesNode(AuraScannerHudState state) {
            this.state = state;
        }

        @Override
        public void resolveLayout(HudContext ctx) {
            this.width = ctx.screenWidth();
            this.height = ctx.screenHeight();
        }

        @Override
        protected void renderSelf(HudContext ctx, GuiGraphics graphics) {
            float alpha = state.fadeAmount.get();
            double interlacePos = Math.ceil((state.usageIntervals % 14) * 0.5) * 0.5;
            for (int i = 0; i < height; i++) {
                if (i % 4 == 0) {
                    GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_LINES, 0, i - interlacePos, 4, width, 0, 0, 1, 4, 0, 1, 1, 1, opacity, true, 1F);
                }
            }
        }
    }

    private static class ThreeDItemNode extends HudNode {
        private final ItemStack stack;
        private final AuraScannerHudState state;

        public ThreeDItemNode(ItemStack stack, AuraScannerHudState state) {
            this.stack = stack;
            this.state = state;
            this.width = 16;
            this.height = 16;
        }

        @Override
        protected void renderSelf(HudContext ctx, GuiGraphics graphics) {
            if (stack.isEmpty()) return;
            Minecraft mc = Minecraft.getInstance();
            Quaternionf rotation = state.dowsingArrowRotation.get();
            float alpha = state.fadeAmount.get();

            graphics.pose().pushPose();
            graphics.pose().scale(25, -25, 25);
            graphics.pose().mulPose(rotation);

            net.minecraft.client.resources.model.BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);

            com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
            mc.getItemRenderer().render(
                    stack,
                    net.minecraft.world.item.ItemDisplayContext.GUI,
                    false,
                    graphics.pose(),
                    graphics.bufferSource(),
                    0xF000F0,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    model
            );
            graphics.flush();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            graphics.pose().popPose();
        }
    }

    private static class CenterRegistrationNode extends HudNode {
        private final AuraScannerHudState state;

        public CenterRegistrationNode(AuraScannerHudState state) {
            this.state = state;
            this.width = CENTER_INFO_FRAME_WIDTH;
            this.height = CENTER_INFO_FRAME_HEIGHT;
        }

        @Override
        protected void renderSelf(HudContext ctx, GuiGraphics graphics) {
            PokedexUsageContext usageContext = state.pokedexUsageContext.get();
            if (usageContext == null || usageContext.getDisplayRegisterInfoIntervals() <= 0)
                return;

            float alpha = state.fadeAmount.get();
            int vOffset = (int) (Math.min(Math.ceil(usageContext.getDisplayRegisterInfoIntervals()), com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS) * height);

            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(
                    graphics.pose(),
                    CENTER_INFO_FRAME,
                    (int) -width / 2,
                    (int) -height / 2,
                    (int) height,
                    (int) width,
                    0,
                    vOffset,
                    (int) width,
                    (int) height * 6,
                    0, 1, 1, 1, alpha, true, 1F
            );

            if (usageContext.getDisplayRegisterInfoIntervals() >= com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS) {
                com.cobblemon.mod.common.client.render.RenderHelperKt.drawScaledText(
                        graphics,
                        com.cobblemon.mod.common.client.CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                        getRegisterText(usageContext.getNewPokemonInfo()),
                        0,
                        (int) (-height / 2 + 4),
                        1F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null
                );
            }
        }
    }

    private static class PokedexTooltipNode extends HudNode {
        private final MutableComponent text;
        private final int offsetY;
        private int baseColor = 0x00FFFF;

        public PokedexTooltipNode(MutableComponent text, int offsetY) {
            this.text = text;
            this.offsetY = offsetY;
        }

        public void setBaseColor(int color) {
            this.baseColor = color;
            text.setStyle(text.getStyle().withColor(baseColor));
        }

        public MutableComponent getText() {
            return text;
        }

        @Override
        public void resolveLayout(HudContext ctx) {
            int textWidth = Minecraft.getInstance().font.width(
                    com.cobblemon.mod.common.api.text.TextKt.font(text, com.cobblemon.mod.common.client.CobblemonResources.INSTANCE.getDEFAULT_LARGE())
            );
            this.width = textWidth + 4;
            this.height = Minecraft.getInstance().font.lineHeight; // typically 9 or 10
        }

        @Override
        public boolean contains(double mouseX, double mouseY) {
            float halfWidth = width / 2f;
            int tooltipTop = offsetY + 1;
            return mouseX >= -halfWidth && mouseX <= halfWidth &&
                    mouseY >= tooltipTop && mouseY <= tooltipTop + height;
        }

        @Override
        public boolean onMouseHovered(double mouseX, double mouseY) {
            if(!contains(mouseX, mouseY)) return false;
            text.setStyle(text.getStyle().withColor(0xFFFFFF));
            return true;
        }

        @Override
        protected void renderSelf(HudContext ctx, GuiGraphics graphics) {
            PoseStack poseStack = graphics.pose();
            int tooltipWidth = (int) width;
            int tooltipHeight = (int) height;
            int tooltipTop = offsetY + 1;

            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, 1000.0);

            Minecraft mc = Minecraft.getInstance();
            mc.getMainRenderTarget().bindWrite(false);

            graphics.enableScissor(
                    (int) (this.getX() - (tooltipWidth / 2f)),
                    (int) (this.getY() + tooltipTop + 1),
                    (int) (this.getX() - (tooltipWidth / 2f) + tooltipWidth),
                    (int) (this.getY() + tooltipTop + tooltipHeight - 1)
            );

            graphics.disableScissor();
            mc.getMainRenderTarget().bindWrite(true);

            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(poseStack, TOOLTIP_EDGE, (int) -(tooltipWidth / 2f) - 1, tooltipTop, tooltipHeight, 1, 0, 0, 1, tooltipHeight, 0, 1, 1, 1, 1.0f, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(poseStack, TOOLTIP_BACKGROUND, (int) -(tooltipWidth / 2f), tooltipTop, tooltipHeight, tooltipWidth, 0, 0, tooltipWidth, tooltipHeight, 0, 1, 1, 1, 1.0f, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(poseStack, TOOLTIP_EDGE, (int) (tooltipWidth / 2f), tooltipTop, tooltipHeight, 1, 0, 0, 1, tooltipHeight, 0, 1, 1, 1, 1.0f, true, 1F);

            int rgb = 0xFFFFFF; // fallback
            net.minecraft.network.chat.TextColor styleColor = text.getStyle().getColor();
            if (styleColor != null) {
                rgb = styleColor.getValue(); // RRGGBB
            }
            int argb = 0xFF000000 | rgb; // make it opaque ARGB

            com.cobblemon.mod.common.client.render.RenderHelperKt.drawScaledText(
                    graphics,
                    com.cobblemon.mod.common.client.CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
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
            poseStack.popPose();
        }
    }

    private static GroupNode createAuraReaderMode(AuraScannerHudState state, Animator animator) {
        GroupNode group = new GroupNode();

        // Scanlines
        group.addChild(new ScanlinesNode(state));

        // Borders & Notch
        LayoutNode bordersLayout = new LayoutNode();
        bordersLayout.setAnchor(Anchor.CENTER);
        bordersLayout.addChild(new PokedexBordersNode(state));
        group.addChild(bordersLayout);

        // Scan Rings
        group.addChild(createScanRings(state));

        // Reticle
        /*LayoutNode reticleLayout = new LayoutNode();
        reticleLayout.setAnchor(Anchor.CENTER);
        ReticleNode reticle = new ReticleNode();
        reticle.setRadius(25f);
        state.lockedTarget.subscribe((oldV, newV) -> {
            reticle.setLocked(newV != null);
            if (newV != null) {
                animator.animate(reticle.scaleXProperty(), 1.2f, new com.jayemceekay.fluxui.hud.animation.AnimationSpec(0.3f, com.jayemceekay.fluxui.hud.animation.Easing.SPRING));
                animator.animate(reticle.scaleYProperty(), 1.2f, new com.jayemceekay.fluxui.hud.animation.AnimationSpec(0.3f, com.jayemceekay.fluxui.hud.animation.Easing.SPRING));
            } else {
                animator.animate(reticle.scaleXProperty(), 1.0f, com.jayemceekay.fluxui.hud.animation.AnimationSpec.NORMAL);
                animator.animate(reticle.scaleYProperty(), 1.0f, com.jayemceekay.fluxui.hud.animation.AnimationSpec.NORMAL);
            }
        });
        reticle.setLocked(state.lockedTarget.get() != null);
        reticleLayout.addChild(reticle);
        group.addChild(reticleLayout);*/

        // Signal Strength Arc
        /*LayoutNode signalLayout = new LayoutNode();
        signalLayout.setAnchor(Anchor.CENTER);
        ProceduralArcNode signalMeter = new ProceduralArcNode();
        signalMeter.setRadius(65f);
        signalMeter.setThickness(4f);
        signalMeter.setStartAngle(135f);
        signalMeter.setEndAngle(225f);
        signalMeter.setSegments(10);
        signalMeter.setSegmentGap(2f);
        StateBindings.bindArcFill(signalMeter, state.signalStrength);
        signalLayout.addChild(signalMeter);
        group.addChild(signalLayout);*/

        // Directional Pointers
        /*LayoutNode pointersLayout = new LayoutNode();
        pointersLayout.setAnchor(Anchor.CENTER);
        pointersLayout.addChild(new DirectionalPointersNode(state));
        group.addChild(pointersLayout);*/

        // Signal Labels
        LayoutNode labelLayout = new LayoutNode();
        labelLayout.setAnchor(Anchor.CENTER);
        labelLayout.setOffsetY(-110f);
        TextNode signalLabel = new TextNode();
        signalLabel.setCentered(true);
        StateBindings.bindText(signalLabel, state.signalLabel);
        state.signalColor.subscribe((oldV, newV) -> signalLabel.setColor(newV));
        signalLabel.setColor(state.signalColor.get());
        labelLayout.addChild(signalLabel);

        TextNode infoLine = new TextNode();
        infoLine.setCentered(true);
        infoLine.setY(12f);
        StateBindings.bindText(infoLine, state.infoLine.map(s -> s == null ? "" : s));
        labelLayout.addChild(infoLine);
        group.addChild(labelLayout);

        // Signal Meter Bars (Bottom)
        /*LayoutNode barsLayout = new LayoutNode();
        barsLayout.setAnchor(Anchor.BOTTOM_CENTER);
        barsLayout.setOffsetY(-44f);

        BarMeterNode strBar = new BarMeterNode();
        strBar.setX(-85f);
        strBar.setWidth(80f);
        strBar.setHeight(11f);
        strBar.setColor(0xA330FF);
        StateBindings.bindFloat(strBar::setFillAmount, state.signalStrength);
        barsLayout.addChild(strBar);

        BarMeterNode intBar = new BarMeterNode();
        intBar.setX(5f);
        intBar.setWidth(80f);
        intBar.setHeight(11f);
        intBar.setColor(0x00FFFF);
        StateBindings.bindFloat(intBar::setFillAmount, state.interference);
        barsLayout.addChild(intBar);

        TextNode strLabel = new TextNode("STR");
        strLabel.setX(-83f);
        strLabel.setY(2f);
        barsLayout.addChild(strLabel);

        TextNode intLabel = new TextNode("INT");
        intLabel.setX(7f);
        intLabel.setY(2f);
        barsLayout.addChild(intLabel);

        group.addChild(barsLayout);*/

        // Waveform
        /*LayoutNode waveLayout = new LayoutNode();
        waveLayout.setAnchor(Anchor.CENTER);
        waveLayout.setX(-50f);
        waveLayout.setY(-65f);
        waveLayout.addChild(new WaveformNode(state, 0x00FFFF));
        group.addChild(waveLayout);*/

        // Info Frames
        LayoutNode infoFramesLayout = new LayoutNode();
        infoFramesLayout.setAnchor(Anchor.CENTER);
        infoFramesLayout.addChild(new InfoFramesNode(state));
        group.addChild(infoFramesLayout);

        return group;
    }

    private static GroupNode createPokedexScannerMode(AuraScannerHudState state, Animator animator) {
        GroupNode group = new GroupNode();

        // Scanlines
        group.addChild(new ScanlinesNode(state));

        // Borders & Notch
        LayoutNode bordersLayout = new LayoutNode();
        bordersLayout.setAnchor(Anchor.CENTER);
        bordersLayout.addChild(new PokedexBordersNode(state));
        group.addChild(bordersLayout);

        // Scan Rings
        group.addChild(createScanRings(state));

        // Info Frames
        LayoutNode infoFramesLayout = new LayoutNode();
        infoFramesLayout.setAnchor(Anchor.CENTER);
        infoFramesLayout.addChild(new InfoFramesNode(state));
        group.addChild(infoFramesLayout);

        // Center Info & Scanning Pointers
        LayoutNode centerLayout = new LayoutNode();
        centerLayout.setAnchor(Anchor.CENTER);

        var scanningOpacity = state.scanningProgress.map(p -> {
            float progress = p * 100f;
            float centerOpacity = (progress > (com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext.MAX_SCAN_PROGRESS - 10) ?
                    (com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext.MAX_SCAN_PROGRESS - progress) : progress) * 0.1F;
            return Math.max(0F, Math.min(1.0f, centerOpacity));
        });

        // Unknown Mark
        ImageNode unknownMark = new ImageNode(UNKNOWN_MARK, 34, 46);
        unknownMark.setX(-17);
        unknownMark.setY(-21);
        StateBindings.bindVisible(unknownMark, state.scanningProgress.map(p -> p > 0 && p < 1.0));
        StateBindings.bindOpacity(unknownMark, scanningOpacity);
        centerLayout.addChild(unknownMark);

        // Scanning Pointers
        GroupNode pointersGroup = new GroupNode();
        ImageNode leftPointer = new ImageNode(POINTER, 6, 10);
        leftPointer.setRegionWidth(6);
        leftPointer.setRegionHeight(10);
        leftPointer.setTextureWidth(12);
        leftPointer.setTextureHeight(10);
        leftPointer.setX(-6 - 30);
        leftPointer.setY(-5);

        ImageNode rightPointer = new ImageNode(POINTER, 6, 10);
        rightPointer.setRegionWidth(6);
        rightPointer.setRegionHeight(10);
        rightPointer.setTextureWidth(12);
        rightPointer.setTextureHeight(10);
        rightPointer.setU(6);
        rightPointer.setX(30);
        rightPointer.setY(-5);

        StateBindings.bindVisible(pointersGroup, state.scanningProgress.map(p -> p > 0 && p < 1.0));
        StateBindings.bindOpacity(pointersGroup, scanningOpacity);
        StateBindings.bindRotation(pointersGroup, state.sweepAngle.map(a -> (float) Math.toDegrees(a * 0.5)));

        pointersGroup.addChild(leftPointer);
        pointersGroup.addChild(rightPointer);
        centerLayout.addChild(pointersGroup);

        // Center Registration Info
        centerLayout.addChild(new CenterRegistrationNode(state));

        group.addChild(centerLayout);

        return group;
    }

    private static class PokedexBordersNode extends HudNode {
        private final AuraScannerHudState state;

        public PokedexBordersNode(AuraScannerHudState state) {
            this.state = state;
        }

        @Override
        public void resolveLayout(HudContext ctx) {
            this.width = ctx.screenWidth();
            this.height = ctx.screenHeight();
        }

        @Override
        protected void renderSelf(HudContext ctx, GuiGraphics graphics) {
            float alpha = state.fadeAmount.get();
            int screenWidth = (int) width;
            int screenHeight = (int) height;

            // Draw borders exactly as in Impl
            // Corners
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_CORNERS, -screenWidth / 2, -screenHeight / 2, 4, 4, 0, 0, 8, 8, 0, 1, 1, 1, alpha, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_CORNERS, screenWidth / 2 - 4, -screenHeight / 2, 4, 4, 4, 0, 8, 8, 0, 1, 1, 1, alpha, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_CORNERS, -screenWidth / 2, screenHeight / 2 - 4, 4, 4, 0, 4, 8, 8, 0, 1, 1, 1, alpha, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_CORNERS, screenWidth / 2 - 4, screenHeight / 2 - 4, 4, 4, 4, 4, 8, 8, 0, 1, 1, 1, alpha, true, 1F);

            // Sides and Notch
            int notchStartX = (screenWidth - SCAN_OVERLAY_NOTCH_WIDTH) / 2;
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_TOP, 4 - screenWidth / 2, -screenHeight / 2, 3, notchStartX - 4, 0, 0, notchStartX - 4, 3, 0, 1, 1, 1, alpha, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_TOP, notchStartX + SCAN_OVERLAY_NOTCH_WIDTH - screenWidth / 2, -screenHeight / 2, 3, (screenWidth - (notchStartX + SCAN_OVERLAY_NOTCH_WIDTH + 4)), 0, 0, (screenWidth - (notchStartX + SCAN_OVERLAY_NOTCH_WIDTH + 4)), 3, 0, 1, 1, 1, alpha, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_BOTTOM, 4 - screenWidth / 2, (screenHeight / 2 - 3), 3, (screenWidth - 8), 0, 0, (screenWidth - 8), 3, 0, 1, 1, 1, alpha, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_LEFT, -screenWidth / 2, 4 - screenHeight / 2, (screenHeight - 8), 3, 0, 0, 3, (screenHeight - 8), 0, 1, 1, 1, alpha, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_RIGHT, (screenWidth / 2 - 3), 4 - screenHeight / 2, (screenHeight - 8), 3, 0, 0, 3, (screenHeight - 8), 0, 1, 1, 1, alpha, true, 1F);
            com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk(graphics.pose(), SCAN_OVERLAY_NOTCH, notchStartX - screenWidth / 2, -screenHeight / 2, 12, SCAN_OVERLAY_NOTCH_WIDTH, 0, 0, SCAN_OVERLAY_NOTCH_WIDTH, 12, 0, 1, 1, 1, alpha, true, 1F);
        }
    }

    private static GroupNode createDowsingMachineMode(AuraScannerHudState state, Animator animator) {
        GroupNode group = new GroupNode();

        group.addChild(new ScanlinesNode(state));

        // 3D Item Arrow
        LayoutNode arrowLayout = new LayoutNode();
        arrowLayout.setAnchor(Anchor.CENTER);
        arrowLayout.setZ(100f);
        arrowLayout.addChild(new ThreeDItemNode(new ItemStack(ModItems.DIRECTION_ARROW.get()), state));
        group.addChild(arrowLayout);

        // Distance Text
        LayoutNode distTextLayout = new LayoutNode();
        distTextLayout.setAnchor(Anchor.CENTER);
        distTextLayout.setOffsetY(20f);
        TextNode distText = new TextNode();
        distText.setCentered(true);
        distText.setColor(0x00FFFF);
        StateBindings.bindText(distText, state.dowsingDistance.map(d -> (int) d.floatValue() + "m"));
        distTextLayout.addChild(distText);
        group.addChild(distTextLayout);

        // Material Name
        LayoutNode materialTextLayout = new LayoutNode();
        materialTextLayout.setAnchor(Anchor.CENTER);
        materialTextLayout.setOffsetY(-124f);
        TextNode materialText = new TextNode();
        materialText.setCentered(true);
        materialText.setGlow(true);
        materialText.setColor(0x00FFFF);
        StateBindings.bindText(materialText, state.dowsingMaterialName);
        materialTextLayout.addChild(materialText);
        group.addChild(materialTextLayout);

        return group;
    }

    private static GroupNode createScanRings(AuraScannerHudState state) {
        GroupNode rings = new GroupNode();

        LayoutNode centerLayout = new LayoutNode();
        centerLayout.setAnchor(Anchor.CENTER);
        rings.addChild(centerLayout);

        // Outer Ring
        ImageNode outer = new ImageNode(SCAN_RING_OUTER);
        outer.setTextureHeight(116);
        outer.setTextureWidth(116);
        outer.setRegionWidth(116);
        outer.setRegionHeight(116);
        outer.setWidth(116);
        outer.setHeight(116);
        outer.setX(-58);
        outer.setY(-58);
        StateBindings.bindRotation(outer, state.sweepAngle.map(a -> (float) Math.toDegrees(-a * 0.5)));
        centerLayout.addChild(outer);

        // Middle Rings (40 segments)
        GroupNode middleRingsGroup = new GroupNode();
        for (int i = 0; i < 40; i++) {
            final int idx = i;
            ImageNode middle = new ImageNode(SCAN_RING_MIDDLE, 100, 1);
            middle.setX(-50);
            middle.setY(-0.5f);
            middle.setPivot(0.5f, 0.5f);
            middle.setRotationDeg(idx * 4.5f);

            // Logic for visibility/opacity based on mode
            state.mode.subscribe((oldM, newM) -> {
                if (newM == AuraReaderManager.AuraScannerMode.AURA_READER) {
                    state.scanningProgress.subscribe((oldP, newP) -> {
                        float progress = newP * 100f; // Assuming 0-1
                        int segments = 40;
                        if (progress > 0) {
                            if (progress >= 20) {
                                segments = (int) Math.floor((progress - 20.0) / 2.0);
                            }
                        }
                        middle.setVisible(idx < segments);
                        // Also handle opacity from PokedexScannerRendererImpl
                        float opacity = 1.0f;
                        if (progress > 0 && progress < 20) {
                            opacity = 1.0f - (progress * 0.05f);
                        }
                        middle.setOpacity(opacity);
                    });
                } else if (newM == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER) {
                    state.scanningProgress.subscribe((oldP, newP) -> {
                        float progress = newP * 100f; // Assuming 0-1
                        int segments = 40;
                        if (progress > 0) {
                            if (progress >= 20) {
                                segments = (int) Math.floor((progress - 20.0) / 2.0);
                            }
                        }
                        middle.setVisible(idx < segments);
                        // Also handle opacity from PokedexScannerRendererImpl
                        float opacity = 1.0f;
                        if (progress > 0 && progress < 20) {
                            opacity = 1.0f - (progress * 0.05f);
                        }
                        middle.setOpacity(opacity);
                    });
                }
            });

            // Common rotation
            StateBindings.bindRotation(middle, state.sweepAngle.map(a -> (float) (idx * 4.5 + Math.toDegrees(a * 0.5))));
            middleRingsGroup.addChild(middle);
        }
        //centerLayout.addChild(middleRingsGroup);

        // Inner Ring
        ImageNode inner = new ImageNode(SCAN_RING_INNER, 84, 84);
        inner.setTextureHeight(84);
        inner.setTextureWidth(84);
        inner.setX(-42);
        inner.setY(-42);
        // Using AbstractModeLogic.innerRingRotation logic (simplified to bind to sweep or similar)
        StateBindings.bindRotation(inner, state.sweepAngle.map(a -> (float) Math.toDegrees(-a)));
        centerLayout.addChild(inner);

        // Minimal hotspot scan overlay (v1): text + progress bar while holding to scan
        LayoutNode scanHud = new LayoutNode();
        scanHud.setAnchor(Anchor.CENTER);
        scanHud.setOffsetY(64f);

        TextNode scanningText = new TextNode();
        scanningText.setCentered(true);
        scanningText.setGlow(true);
        scanningText.setColor(0x00FFFF);
        scanningText.setText(net.minecraft.network.chat.Component.literal("SCANNING…"));
        StateBindings.bindVisible(scanningText, state.hotspotScanning);
        scanHud.addChild(scanningText);

        // Progress bar (uses BarMeterNode like energy bar)
        LayoutNode barLayout = new LayoutNode();
        barLayout.setAnchor(Anchor.CENTER);
        barLayout.setOffsetY(78f);
        BarMeterNode scanBar = new BarMeterNode();
        scanBar.setWidth(96f);
        scanBar.setHeight(4f);
        scanBar.setX(-48f);
        StateBindings.bindFloat(scanBar::setFillAmount, state.hotspotScanProgress);
        StateBindings.bindVisible(scanBar, state.hotspotScanning);
        barLayout.addChild(scanBar);

        rings.addChild(scanHud);
        rings.addChild(barLayout);

        return rings;
    }
}
