package com.jayemceekay.shadowedhearts.common.thermal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/**
 * Public API facade for modders to extend the Aura Reader's operational temperature system.
 */
public final class OperationalTemperatureAPI {
    private OperationalTemperatureAPI() {}

    // Conditionals registry
    public static void registerConditional(ResourceLocation id, OperationalTempConditional conditional) {
        OperationalTempRegistry.register(id, conditional);
    }

    public static void unregisterConditional(ResourceLocation id) {
        OperationalTempRegistry.unregister(id);
    }

    // Thermal source registry (radiative inverse-square)
    public static void registerBlockHeatSource(Block block, float heatAt1mCPerTick, float maxRange) {
        ThermalSourceRegistry.registerBlock(block, heatAt1mCPerTick, maxRange);
    }

    public static void unregisterBlockHeatSource(Block block) {
        ThermalSourceRegistry.unregisterBlock(block);
    }

    public static void registerEntityHeatSource(EntityType<?> type, float heatAt1mCPerTick, float maxRange) {
        ThermalSourceRegistry.registerEntity(type, heatAt1mCPerTick, maxRange);
    }

    public static void unregisterEntityHeatSource(EntityType<?> type) {
        ThermalSourceRegistry.unregisterEntity(type);
    }
}
