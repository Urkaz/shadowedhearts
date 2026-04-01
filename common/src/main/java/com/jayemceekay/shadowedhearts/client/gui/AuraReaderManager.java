package com.jayemceekay.shadowedhearts.client.gui;

import com.jayemceekay.fluxui.hud.core.HudManager;
import com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext;
import com.jayemceekay.shadowedhearts.content.items.AuraReaderItem;
import com.jayemceekay.shadowedhearts.integration.accessories.SnagAccessoryBridgeHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * HUD logic for the Aura Reader, handling scanner states, detection cycles, and player interaction.
 */
public class AuraReaderManager {

    public static final AuraScannerHudState HUD_STATE = new AuraScannerHudState();

    /**
     * Context for tracking Pokedex usage during scans.
     */
    public static final PokedexUsageContext POKEDEX_USAGE_CONTEXT = new PokedexUsageContext();

    /**
     * Different operational modes for the scanner HUD.
     */
    public enum AuraScannerMode {AURA_READER, POKEDEX_SCANNER, DOWSING_MACHINE}

    /**
     * The currently selected scanner mode.
     */
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
        return HUD_STATE.handleShiftScroll(scrollY);
    }

    /**
     * Main update loop for the Aura Reader HUD.
     * Handles timer updates, input processing, network synchronization, and signal detection management.
     */
    public static void tick() {
        try {
            HUD_STATE.tick();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Entry point for rendering the Aura Reader HUD.
     */
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        HUD_STATE.update(partialTick);
        com.jayemceekay.fluxui.hud.core.HudContext ctx = HudManager.createContext(partialTick);
        HudManager.tick(ctx);
        HudManager.render(ctx, guiGraphics);
    }

    /**
     * Checks if the HUD is currently visible.
     */
    public static boolean isActive() {
        return HUD_STATE.isActive && HUD_STATE.fadeAmountVal > 0;
    }

    /**
     * Checks if a specific shadow entity has been detected and is currently within its HUD lifetime.
     */
    public static boolean isDetected(UUID uuid) {
        if (uuid == null) return false;
        return HUD_STATE.detectedShadows.containsKey(uuid);
    }

    /**
     * Programmatically sets the HUD's active state.
     */
    public static void setActive(boolean activeIn) {
        HUD_STATE.isActive = activeIn;
        if (HUD_STATE.isActive) {
            HUD_STATE.bootTimerVal = AuraScannerHudState.BOOT_DURATION;
            HUD_STATE.sweepAngleVal = 0.0f;
            HUD_STATE.prevSweepAngleVal = 0.0f;
        }
    }

    /**
     * Adds meteoroid positions to the pending response map.
     * Called when the server returns potential meteoroid locations from a pulse scan.
     */
    public static void enqueueMeteoroidCenters(java.util.List<BlockPos> centers) {
        if (centers == null || centers.isEmpty()) return;
        synchronized (HUD_STATE.pendingMeteoroidResponses) {
            for (BlockPos pos : centers) {
                if (pos != null) {
                    HUD_STATE.pendingMeteoroidResponses.put(pos.immutable(), AuraScannerHudState.RESPONSE_DELAY);
                }
            }
        }
    }

    /**
     * Checks if the equipped Aura Reader has a specific upgrade for the given mode.
     */
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
