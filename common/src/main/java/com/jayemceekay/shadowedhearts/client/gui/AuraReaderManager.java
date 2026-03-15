package com.jayemceekay.shadowedhearts.client.gui;

import com.jayemceekay.shadowedhearts.client.gui.modes.*;
import com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext;
import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs;
import com.jayemceekay.shadowedhearts.content.items.AuraReaderItem;
import com.jayemceekay.shadowedhearts.integration.accessories.SnagAccessoryBridgeHolder;
import com.jayemceekay.shadowedhearts.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HUD logic for the Aura Reader, handling scanner states, detection cycles, and player interaction.
 */
public class AuraReaderManager {

    /** Logics for different scanner modes. */
    public static final Map<AuraScannerMode, AuraScannerModeLogic> MODE_LOGICS = new EnumMap<>(AuraScannerMode.class);

    static {
        MODE_LOGICS.put(AuraScannerMode.AURA_READER, new AuraReaderLogic());
        MODE_LOGICS.put(AuraScannerMode.POKEDEX_SCANNER, new PokedexScannerLogic());
        MODE_LOGICS.put(AuraScannerMode.DOWSING_MACHINE, new DowsingMachineLogic());
    }

    /** Context for tracking Pokedex usage during scans. */
    public static final PokedexUsageContext POKEDEX_USAGE_CONTEXT = new PokedexUsageContext();
    /** Different operational modes for the scanner HUD. */
    public enum AuraScannerMode {AURA_READER, POKEDEX_SCANNER, DOWSING_MACHINE}

    /** The currently selected scanner mode. */
    public static AuraScannerMode currentMode = AuraScannerMode.AURA_READER;

    /**
     * Platform hook: called from client mouse scroll listeners.
     * When the player holds Shift and the HUD is in acquisition mode (no lock),
     * use the mouse wheel to cycle between available signals.
     *
     * @param scrollY positive for scroll up, negative for scroll down (platform default)
     * @return true if the event was consumed by the HUD and should be cancelled
     */
    public static boolean handleShiftScroll(double scrollY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;

        boolean shiftDown = mc.options.keyShift.isDown();

        if (AbstractModeLogic.active && currentMode == AuraScannerMode.DOWSING_MACHINE && shiftDown) {
            int dir = scrollY > 0 ? -1 : 1;
            DowsingMachineLogic.selectedDowsingMaterialIndex = (DowsingMachineLogic.selectedDowsingMaterialIndex + dir + DowsingMachineLogic.DOWSING_MATERIALS.size()) % DowsingMachineLogic.DOWSING_MATERIALS.size();
            return true;
        }

        // Only while active, in acquisition, not locked, with signals, and Shift held
        if (!AbstractModeLogic.active || !AuraReaderLogic.acquisitionMode || AuraReaderLogic.lockedTarget != null || AuraReaderLogic.CURRENT_SIGNALS.isEmpty() || !shiftDown) {
            return false;
        }

        int dir = 0;
        if (scrollY > 0.0) dir = 1;
        else if (scrollY < 0.0) dir = -1;
        if (dir == 0) return false;

        // Natural mapping: scroll up → next, scroll down → previous
        if (dir > 0) {
            AuraReaderLogic.selectedSignalIndex = (AuraReaderLogic.selectedSignalIndex + 1) % AuraReaderLogic.CURRENT_SIGNALS.size();
        } else {
            AuraReaderLogic.selectedSignalIndex = (AuraReaderLogic.selectedSignalIndex - 1 + AuraReaderLogic.CURRENT_SIGNALS.size()) % AuraReaderLogic.CURRENT_SIGNALS.size();
        }

        // Subtle feedback
        mc.player.playSound(ModSounds.AURA_SCANNER_BEEP.get(), 0.08f * ShadowedHeartsConfigs.getInstance().getClientConfig().soundConfig().auraScannerBeepVolume(), 1.3f);
        return true;
    }

    /**
     * Main update loop for the Aura Reader HUD.
     * Handles timer updates, input processing, network synchronization, and signal detection management.
     */
    public static void tick() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            AbstractModeLogic.updateShared(mc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Entry point for rendering the Aura Reader HUD. */
    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        AuraReaderRenderer.render(guiGraphics, deltaTracker);
    }

    /** Checks if the HUD is currently visible. */
    public static boolean isActive() {
        return AbstractModeLogic.active && AbstractModeLogic.fadeAmount > 0;
    }

    /** Checks if a specific shadow entity has been detected and is currently within its HUD lifetime. */
    public static boolean isDetected(UUID uuid) {
        if (uuid == null) return false;
        return AuraReaderLogic.DETECTED_SHADOWS.containsKey(uuid);
    }

    /** Programmatically sets the HUD's active state. */
    public static void setActive(boolean activeIn) {
        AbstractModeLogic.active = activeIn;
        if (AbstractModeLogic.active) {
            AbstractModeLogic.bootTimer = AbstractModeLogic.BOOT_DURATION;
            AbstractModeLogic.sweepAngle = 0.0f;
            AbstractModeLogic.prevSweepAngle = 0.0f;
        }
    }

    /**
     * Adds meteoroid positions to the pending response map.
     * Called when the server returns potential meteoroid locations from a pulse scan.
     */
    public static void enqueueMeteoroidCenters(java.util.List<BlockPos> centers) {
        if (centers == null || centers.isEmpty()) return;
        synchronized (AuraReaderLogic.PENDING_METEOROID_RESPONSES) {
            for (BlockPos pos : centers) {
                if (pos != null) {
                    AuraReaderLogic.PENDING_METEOROID_RESPONSES.put(pos.immutable(), AuraReaderLogic.RESPONSE_DELAY);
                }
            }
        }
    }

    /** Checks if the equipped Aura Reader has a specific upgrade for the given mode. */
    public static boolean hasUpgrade(AuraScannerMode mode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        ItemStack reader = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(mc.player);
        if (reader.isEmpty()) return false;

        List<ItemStack> upgrades = AuraReaderItem.readUpgrades(reader, mc.level.registryAccess());
        for (ItemStack upgrade : upgrades) {
            if (mode == AuraScannerMode.POKEDEX_SCANNER) {
                if (upgrade.getItem() instanceof com.jayemceekay.shadowedhearts.content.items.PokedexIntegratorItem) {
                    return true;
                }
            }
            // Add other upgrades here if needed
        }
        return false;
    }
}
