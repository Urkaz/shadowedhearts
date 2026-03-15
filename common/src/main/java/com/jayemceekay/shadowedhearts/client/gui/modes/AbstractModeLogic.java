package com.jayemceekay.shadowedhearts.client.gui.modes;

import com.jayemceekay.shadowedhearts.client.ModKeybinds;
import com.jayemceekay.shadowedhearts.client.aura.AuraPulseRenderer;
import com.jayemceekay.shadowedhearts.client.gui.AuraReaderManager;
import com.jayemceekay.shadowedhearts.client.gui.AuraReaderModeScreen;
import com.jayemceekay.shadowedhearts.common.aura.AuraReaderCharge;
import com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext;
import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs;
import com.jayemceekay.shadowedhearts.content.items.AuraReaderItem;
import com.jayemceekay.shadowedhearts.integration.accessories.SnagAccessoryBridgeHolder;
import com.jayemceekay.shadowedhearts.network.ShadowedHeartsNetwork;
import com.jayemceekay.shadowedhearts.network.aura.AuraScannerC2SPacket;
import com.jayemceekay.shadowedhearts.network.aura.AuraTrackingStateC2SPacket;
import com.jayemceekay.shadowedhearts.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractModeLogic implements AuraScannerModeLogic {

    /** Duration of the boot sequence in seconds. */
    public static final float BOOT_DURATION = 0.5f;
    public static final float RENDER_UPDATES_PER_SECOND = (float) (1.0 / 0.0175);
    /** Whether the HUD is currently active. */
    public static boolean active = false;

    /** Current opacity for the HUD fade-in/out effect. */
    public static float fadeAmount = 0.0f;
    /** Previous fadeAmount for interpolation. */
    public static float prevFadeAmount = 0.0f;
    /** Current progress of the boot sequence (0..1). */
    public static float bootTimer = 0.0f;
    /** Previous bootTimer for interpolation. */
    public static float prevBootTimer = 0.0f;
    /** Current angle for the scanning sweep effect. */
    public static float sweepAngle = 0.0f;
    /** Previous sweepAngle for interpolation. */
    public static float prevSweepAngle = 0.0f;
    /** Timer for the visual glitch effect when legendary Pokemon are nearby. */
    public static float glitchTimer = 0.0f;
    /** Previous glitchTimer for interpolation. */
    public static float prevGlitchTimer = 0.0f;
    /** Timer for the periodic beeping sound. */
    public static int beepTimer = 0;
    /** Number of aura pulses queued to be spawned. */
    public static int pulseQueue = 0;
    /** Timer for spacing out pulses in a queue. */
    public static int pulseTimer = 0;
    /** Cooldown timer between full scanner pulses. */
    public static int pulseCooldown = 0;
    /** Cooldown timer between HUD activations. */
    public static int scannerCooldown = 0;
    /** Ticks the Pokedex has been used for scanning. */
    public static int pokedexTicksInUse = 0;

    /** Default cooldown for pulse activations in ticks. */
    public static final int PULSE_COOLDOWN_TICKS = 400; // 10 seconds cooldown between pulse activations
    /** Default cooldown for HUD activations in ticks. */
    public static final int SCANNER_COOLDOWN_TICKS = 20; // 1 second cooldown between HUD activations

    /** Operational temperature in Celsius (persistent per-item). */
    public static float operationalTempC = 0.0f;
    /** Previous operationalTempC for interpolation. */
    public static float prevOperationalTempC = 0.0f;

    /** Whether the mode selection radial menu is open. */
    public static boolean modeMenuOpen = false;
    /** Current opacity for the mode menu. */
    public static float modeMenuAlpha = 0.0f;
    /** Previous modeMenuAlpha for interpolation. */
    public static float prevModeMenuAlpha = 0.0f;

    /** Maximum intensity from any signal, used for UI brightness and audio beeping. */
    public static float maxIntensity = 0;



    public static float usageIntervals = 0.0F;
    public static float innerRingRotation = 0.0F;

    public static void updateAnimations(float tickDelta) {
        if (!active) return;
        float updateInterval = (tickDelta / 20.0F) * RENDER_UPDATES_PER_SECOND;
        usageIntervals += updateInterval;

        PokedexUsageContext usageContext = AuraReaderManager.POKEDEX_USAGE_CONTEXT;
        if (usageContext.getScanningGuiOpen() && usageContext.getViewInfoTicks() < PokedexUsageContext.VIEW_INFO_BUFFER_TICKS) {
            if (usageContext.getScannableEntityInFocus() != null) {
                innerRingRotation = (innerRingRotation + (updateInterval * 10.0F)) % 360;
            } else {
                innerRingRotation = (innerRingRotation + updateInterval) % 360;
            }
        } else {
            innerRingRotation = (innerRingRotation + updateInterval) % 360;
        }
    }

    public static void updateShared(Minecraft mc) {
        prevFadeAmount = fadeAmount;
        prevBootTimer = bootTimer;
        prevSweepAngle = sweepAngle;
        prevGlitchTimer = glitchTimer;
        prevOperationalTempC = operationalTempC;
        prevModeMenuAlpha = modeMenuAlpha;
        maxIntensity = 0;

        if (modeMenuOpen) {
            modeMenuAlpha = Math.min(1.0f, modeMenuAlpha + 0.15f);
        } else {
            modeMenuAlpha = Math.max(0.0f, modeMenuAlpha - 0.15f);
        }

        boolean hasAuraReader = SnagAccessoryBridgeHolder.INSTANCE.isAuraReaderEquipped(mc.player);

        if (active && ModKeybinds.AURA_MODE_SELECTOR.isDown()) {
            if (!modeMenuOpen && !(mc.screen instanceof AuraReaderModeScreen)) {
                modeMenuOpen = true;
                mc.setScreen(new AuraReaderModeScreen());
            }
        }

        // Sync operational temperature from the equipped itemstack (CUSTOM_DATA, Celsius)
        {
            ItemStack tempStack = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(mc.player);
            if (!tempStack.isEmpty() && tempStack.getItem() instanceof AuraReaderItem) {
                operationalTempC = AuraReaderItem.getOperationalTempC(tempStack);
            }
        }

        if (ModKeybinds.consumeAuraScannerPress() && scannerCooldown <= 0) {
            if (hasAuraReader && ShadowedHeartsConfigs.getInstance().getClientConfig().auraScannerEnabled()) {
                ItemStack auraReader = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(mc.player);

                if (!auraReader.isEmpty() && AuraReaderCharge.get(auraReader) > 0) {
                    active = !active;
                    scannerCooldown = SCANNER_COOLDOWN_TICKS;
                    ShadowedHeartsNetwork.sendToServer(new AuraScannerC2SPacket(active));
                    if (active) {
                        Minecraft.getInstance().player.playSound(ModSounds.AURA_READER_EQUIP.get(), ShadowedHeartsConfigs.getInstance().getClientConfig().soundConfig().auraReaderEquipVolume(), 1.0f);
                        bootTimer = BOOT_DURATION;
                        sweepAngle = 0.0f;
                        prevSweepAngle = 0.0f;
                        AuraReaderManager.MODE_LOGICS.get(AuraReaderManager.currentMode).onActivate(mc);
                    } else {
                        Minecraft.getInstance().player.playSound(ModSounds.AURA_READER_UNEQUIP.get(), ShadowedHeartsConfigs.getInstance().getClientConfig().soundConfig().auraReaderUnequipVolume(), 1.0f);
                        // Clear tracking on server when deactivating
                        ShadowedHeartsNetwork.sendToServer(new AuraTrackingStateC2SPacket(false));
                        AuraReaderManager.MODE_LOGICS.get(AuraReaderManager.currentMode).onDeactivate(mc);
                    }
                }
            } else {
                if (active) {
                    active = false;
                    ShadowedHeartsNetwork.sendToServer(new AuraScannerC2SPacket(false));
                    ShadowedHeartsNetwork.sendToServer(new AuraTrackingStateC2SPacket(false));
                    AuraReaderManager.MODE_LOGICS.get(AuraReaderManager.currentMode).onDeactivate(mc);
                }
            }
        }

        if (active && !hasAuraReader) {
            active = false;
            ShadowedHeartsNetwork.sendToServer(new AuraScannerC2SPacket(false));
            ShadowedHeartsNetwork.sendToServer(new AuraTrackingStateC2SPacket(false));
            AuraReaderManager.MODE_LOGICS.get(AuraReaderManager.currentMode).onDeactivate(mc);
        }

        // Cooldown tick down (regardless of active state)
        if (pulseCooldown > 0) {
            pulseCooldown--;
        }
        if (scannerCooldown > 0) {
            scannerCooldown--;
        }

        if (active) {
          AuraReaderManager.MODE_LOGICS.get(AuraReaderManager.currentMode).handleInput(mc);
        }

        if (active) {
            AuraReaderManager.MODE_LOGICS.get(AuraReaderManager.currentMode).tick(mc);
            // Temperature is now updated and persisted server-side. Client only reads and displays it.

            if (pulseQueue > 0) {
                if (pulseTimer <= 0) {
                    AuraPulseRenderer.spawnPulse(mc.player.position());
                    pulseQueue--;
                    pulseTimer = 10; // 0.5s between pulses
                } else {
                    pulseTimer--;
                }
            }


            fadeAmount = Math.min(1.0f, fadeAmount + 0.1f);
            if (bootTimer > 0) {
                bootTimer -= 0.05f;
            }
            sweepAngle += 0.1f;
            if (sweepAngle > (float) Math.PI * 2) {
                sweepAngle -= (float) Math.PI * 2;
                prevSweepAngle -= (float) Math.PI * 2;
            }

            // Deactivate if charge is empty (client side check)
            ItemStack auraReader = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(mc.player);
            if (!auraReader.isEmpty() && auraReader.getItem() instanceof AuraReaderItem) {
                if (AuraReaderCharge.get(auraReader) <= 0) {
                    active = false;
                }
            }

            // Hot/Cold Audio Cues
            if (beepTimer > 0) {
                beepTimer--;
            } else {
                if (maxIntensity > 0) {
                    // maxIntensity is 0.0 to 1.0 based on distance (0 to 64 blocks)
                    // We want faster beeps when closer (higher intensity)
                    // min delay: 5 ticks (0.25s) at max intensity
                    // max delay: 60 ticks (3s) at min intensity
                    int delay = (int) Mth.lerp(maxIntensity, 60, 5);
                    float pitch = Mth.lerp(maxIntensity, 0.8f, 1.5f);
                    mc.level.playSound(mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(), ModSounds.AURA_SCANNER_BEEP.get(), SoundSource.PLAYERS, 0.3f * ShadowedHeartsConfigs.getInstance().getClientConfig().soundConfig().auraScannerBeepVolume(), pitch);
                    beepTimer = delay;
                }
            }
        } else {
            if (pokedexTicksInUse > 0) {
                AuraReaderManager.POKEDEX_USAGE_CONTEXT.stopUsing(pokedexTicksInUse, null);
                pokedexTicksInUse = 0;
            }
            fadeAmount = Math.max(0.0f, fadeAmount - 0.1f);
            beepTimer = 0;
            maxIntensity = 0;
            pulseQueue = 0;
            pulseTimer = 0;
            // Do NOT reset pulseCooldown here; it should continue ticking down even when HUD is closed
            AuraPulseRenderer.clearPulses();
            AuraReaderManager.MODE_LOGICS.get(AuraReaderManager.currentMode).onDeactivate(mc);
            // Temperature relax/update handled on server; client remains read-only when inactive.
        }
    }
}
