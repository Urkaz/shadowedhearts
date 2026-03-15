package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FluidState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks a lightweight per-player wetness value (0..1) and applies evaporative cooling.
 * Wetness increases in water and while standing in rain with sky exposure, and dries over time
 * faster in warm/sunny conditions.
 */
public final class WetnessConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/wetness");
    public static void register() { OperationalTempRegistry.register(ID, new WetnessConditional()); }

    private static final Map<UUID, Float> WETNESS = new ConcurrentHashMap<>();

    public static float getWetness(Player p) {
        if (p == null) return 0.0f;
        return clamp01(WETNESS.getOrDefault(p.getUUID(), 0.0f));
    }

    private static void setWetness(Player p, float v) {
        if (p == null) return;
        WETNESS.put(p.getUUID(), clamp01(v));
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 12; }

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;

        float w = getWetness(player);

        // Update wetness state
        var pos = player.blockPosition();
        FluidState fs = level.getFluidState(pos);
        boolean inWater = !fs.isEmpty() && fs.is(FluidTags.WATER);
        boolean skyExposed = level.getBrightness(LightLayer.SKY, pos) > 0;
        boolean raining = level.isRaining() || level.isThundering();

        if (inWater) {
            w += 0.12f; // fills quickly in water
        } else if (skyExposed && raining) {
            w += 0.02f; // slowly accumulates in rain
        }

        // Drying: faster when warm or in sun, otherwise slow decay
        double angle = (level.getTimeOfDay(1.0f) * 2 * Math.PI) - Math.PI;
        float sunAlt = (float) Math.max(0.0, Math.cos(angle));
        float warmFactor = currentTempC > 22 ? Math.min(1.0f, (currentTempC - 22.0f) / 20.0f) : 0.0f; // 0..1
        float sunFactor = skyExposed ? (0.2f + 0.6f * sunAlt) : 0.0f; // 0..0.8
        float dryRate = 0.005f + 0.03f * warmFactor + 0.02f * sunFactor; // baseline + boosts
        if (!inWater && !raining) {
            w -= dryRate;
        }

        setWetness(player, w);

        // Apply evaporative cooling proportional to wetness
        float delta = -w * 0.25f;
        // Clamp
        if (delta < -0.3f) delta = -0.3f;
        return delta;
    }
}
