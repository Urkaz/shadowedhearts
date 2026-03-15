package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import com.jayemceekay.shadowedhearts.content.items.AuraReaderItem;
import com.jayemceekay.shadowedhearts.registry.util.ModItemComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Adds operational "load heat" from using the Aura Scanner.
 * - While HUD open: +2.0 °C toward target per second
 * - While tracking/locked: +4.0 °C toward target per second
 * These are additive to the implied equilibrium, converted to per-tick delta via K=0.03.
 * When overheating, efficiency drops by up to 60% (throttling).
 */
public final class ScannerLoadConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/scanner_load");

    public static void register() { OperationalTempRegistry.register(ID, new ScannerLoadConditional()); }

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public int priority() { return 415; } // alongside other personal modifiers (after holding source 420-ish)

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;
        if (auraReader == null || auraReader.isEmpty()) return 0.0f;

        boolean open = Boolean.TRUE.equals(auraReader.get(ModItemComponents.AURA_SCANNER_ACTIVE.get()));
        boolean tracking = Boolean.TRUE.equals(auraReader.get(ModItemComponents.AURA_SCANNER_TRACKING.get()));

        if (!open && !tracking) return 0.0f;

        float tLoad = 0.0f;
        if (open) tLoad += 2.0f;
        if (tracking && open) tLoad += 4.0f; // tracking load only applies while scanner is open

        // Throttle when overheating
        float severity = AuraReaderItem.getHeatSeverity(auraReader); // 0..1 as temp exceeds SAFE_MAX
        float efficiency = Mth.clamp(1.0f - 0.6f * severity, 0.2f, 1.0f); // at most 60% reduction, never below 20%
        tLoad *= efficiency;

        // Convert additive °C toward target into per-tick delta via assumed K
        final float K_ASSUMED_PER_TICK = 0.03f;

        // Optional: keep implied target within reasonable bounds to avoid runaway stacking
        float targetEq = Mth.clamp(currentTempC + tLoad, -40.0f, 130.0f);
        float clampedAdd = targetEq - currentTempC;
        return clampedAdd * K_ASSUMED_PER_TICK;
    }
}
