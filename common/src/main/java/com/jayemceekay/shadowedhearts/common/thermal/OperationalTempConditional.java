package com.jayemceekay.shadowedhearts.common.thermal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A pluggable conditional that contributes a temperature delta (in °C per tick)
 * for the Aura Reader's operational temperature system.
 */
public interface OperationalTempConditional {

    /**
     * Unique identifier for this conditional.
     */
    ResourceLocation id();

    /**
     * Determines execution order. Lower priority values run first.
     */
    default int priority() { return 0; }

    /**
     * Compute the temperature change to apply this tick.
     *
     * @param level current level
     * @param player current player
     * @param auraReader the Aura Reader stack
     * @param currentTempC the current temperature in Celsius
     * @return delta in Celsius (can be positive or negative)
     */
    float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC);
}
