package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import com.jayemceekay.shadowedhearts.mixin.BiomeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

public final class DiurnalCycleConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/diurnal");

    public static void register() {
        OperationalTempRegistry.register(ID, new DiurnalCycleConditional());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return -40;
    } // after biome (-100) and before fluids/light

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;

        // Day fraction (0..1), 0 at sunrise.
        float dayFrac = level.getTimeOfDay(1.0f); // 0..1
        // Map to [-π, π] so noon ~ 0, dawn/dusk at ±π/2
        double x = (dayFrac * 2.0 * Math.PI) - Math.PI;
        // Base diurnal curve: warmest at noon, coldest at midnight
        double curve = -Math.cos(x); // -1 at midnight, +1 at noon

        // Biome scaling: dry/hot biomes swing more; humid/wet swing less
        BlockPos pos = player.blockPosition();
        Biome b = level.getBiome(pos).value();
        float baseTemp;
        try {
            baseTemp = ((BiomeAccessor) (Object) b).shadowedhearts$invokeGetTemperature(pos);
        } catch (Throwable t) {
            baseTemp = 0.8f;
        }

        // Heuristic amplitudes in °C (half peak-to-peak)
// Tuned to the provided chart:
//  - ≤0.0 (snow/ice): very small swing
//  - 0.2–0.5 (taiga, windswept, oceans): small to modest swing
//  - 0.6–0.8 (birch/forest/plains/swamp): moderate swing
//  - 0.95 (jungle, humid): moderate-low swing
//  - 1.0–1.1 (stony peaks / windswept savanna): moderately high
//  - ≥2.0 (desert/badlands/savanna and also End/Nether temps): high (will be 0 in End/Nether due to skyFactor=0)
        float maxSwing; // amplitude in °C

        if (baseTemp >= 2.0f) {            // very hot / arid (Overworld deserts); End/Nether also report 2.0
            maxSwing = 10.0f;               // largest daily swing
        } else if (baseTemp >= 1.1f) {     // windswept savanna
            maxSwing = 8.0f;
        } else if (baseTemp >= 1.0f) {     // stony peaks (rainy, snow at height)
            maxSwing = 6.5f;
        } else if (baseTemp >= 0.95f) {    // jungles (humid, canopy)
            maxSwing = 5.0f;
        } else if (baseTemp >= 0.8f) {     // plains, swamp, beach, warm oceans
            maxSwing = 5.5f;
        } else if (baseTemp >= 0.7f) {     // forests
            maxSwing = 5.0f;
        } else if (baseTemp >= 0.6f) {     // birch forests
            maxSwing = 4.5f;
        } else if (baseTemp >= 0.5f) {     // meadow, river, ocean, deep ocean, cherry grove
            maxSwing = 4.0f;                // water-heavy biomes: slightly lower swing
        } else if (baseTemp >= 0.3f) {     // old growth pine taiga
            maxSwing = 3.5f;
        } else if (baseTemp >= 0.2f) {     // windswept taiga/hills/forest, stony shore, taiga
            maxSwing = 2.5f;
        } else {                            // ≤0.0 (frozen peaks, snowy plains/taiga/etc.)
            maxSwing = 0.25f;                // minimal swing in freezing conditions
        }
        // Sky exposure moderates the swing
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        float skyFactor = skyLight / 15.0f; // 0 under solid cover, 1 in open air

        // Weather moderates swing (clouds): rain/snow flatten it
        float weatherDamp = (level.isRaining() || level.isThundering()) ? 0.5f : 1.0f;

        float swing = (float) (curve * maxSwing * skyFactor * weatherDamp);

        // Blend gently toward the swing offset
        float coeff = 0.02f;
        float delta = (swing - 0.0f) * coeff;
        // Clamp
        if (delta > 0.3f) delta = 0.3f;
        if (delta < -0.3f) delta = -0.3f;
        return delta;
    }
}
