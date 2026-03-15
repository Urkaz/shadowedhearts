package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Simulates insulation provided by worn armor/clothing.
 * Pulls operational temperature gently toward a nominal body temperature (~25°C),
 * with strength proportional to the player's total armor value.
 */
public class ArmorInsulationConditional implements OperationalTempConditional {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("shadowedhearts", "armor_insulation");

    public static void register() { OperationalTempRegistry.register(ID, new ArmorInsulationConditional()); }

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public int priority() {
        // Run late so it counteracts prior deltas slightly
        return 900;
    }

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (player == null) return 0.0f;

        // Total armor points (typically 0..20+). Map to 0..1 factor with soft cap.
        float armor = Math.max(0.0f, player.getArmorValue());
        float factor = Math.min(1.0f, armor / 20.0f);
        if (factor <= 0.0f) return 0.0f;

        // Nominal comfortable operating temperature (mid-range of stable band)
        float nominalC = 25.0f;

        // Wetness reduces effective insulation
        float wet = 0.0f;
        try { wet = WetnessConditional.getWetness(player); } catch (Throwable ignored) {}
        float wetPenalty = Math.max(0.5f, 1.0f - (wet * 0.6f));

        // Small pull toward nominal; higher armor strengthens the pull (acts like insulation/thermal inertia)
        // Base coefficient tuned conservatively so it does not overpower environment
        float coeff = 0.02f; // per tick at full armor
        float delta = (nominalC - currentTempC) * coeff * factor * wetPenalty;

        // Clamp to avoid excessive correction in one tick
        if (delta > 0.5f) delta = 0.5f;
        if (delta < -0.5f) delta = -0.5f;
        return delta;
    }
}
