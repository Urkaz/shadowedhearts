package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public final class LightConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/light");

    public static void register() { OperationalTempRegistry.register(ID, new LightConditional()); }

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public int priority() { return 20; }

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;
        int blockLight = level.getBrightness(LightLayer.BLOCK, player.blockPosition());
        int skyLight = level.getBrightness(LightLayer.SKY, player.blockPosition());
        boolean isDay = level.isDay();
        float sunAlt = (float) Math.cos(((level.getTimeOfDay(1.0f) * 2 * Math.PI) - Math.PI)); // ~1 at noon
        sunAlt = Math.max(0.0f, sunAlt);
        float clouds = (level.isRaining() || level.isThundering()) ? 0.5f : 1.0f;

        float warmFromBlocks = (blockLight / 15.0f) * 0.08f; // stronger: torches etc.
        float solar = isDay ? (skyLight / 15.0f) * sunAlt * 0.10f * clouds : 0.0f;
        float nocturnalRadiative = (!isDay && skyLight > 0) ? -(skyLight / 15.0f) * 0.07f : 0.0f;
        return warmFromBlocks + solar + nocturnalRadiative;
    }
}
