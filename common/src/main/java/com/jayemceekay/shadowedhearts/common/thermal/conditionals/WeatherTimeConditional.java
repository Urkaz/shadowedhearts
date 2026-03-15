package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class WeatherTimeConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/weather_time");

    public static void register() { OperationalTempRegistry.register(ID, new WeatherTimeConditional()); }

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public int priority() { return -50; }

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null) return 0.0f;
        float delta = 0.0f;
        // Weather-only effects here; diurnal cycle handled by DiurnalCycleConditional
        if (level.isRaining()) delta -= 0.10f;     // rain cools
        if (level.isThundering()) delta -= 0.06f;  // thunderstorm adds a bit more cooling
        return delta;
    }
}
