package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public final class UndergroundConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/underground");
    public static void register() { OperationalTempRegistry.register(ID, new UndergroundConditional()); }
    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return -20; }

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;
        int sky = level.getBrightness(LightLayer.SKY, player.blockPosition());
        if (sky > 0) return 0.0f; // only apply when fully out of sky

        // Target a buffered temperature around 14°C plus small biome bias
        float biomeBase;
        try { biomeBase = level.getBiome(player.blockPosition()).value().getBaseTemperature(); }
        catch (Throwable t) { biomeBase = 0.8f; }
        float target = 14.0f + (biomeBase - 0.8f) * 8.0f; // small bias
        float coeff = 0.03f; // caves equilibrate a bit quicker
        float delta = (target - currentTempC) * coeff;
        if (delta > 0.5f) delta = 0.5f;
        if (delta < -0.5f) delta = -0.5f;
        return delta;
    }
}
