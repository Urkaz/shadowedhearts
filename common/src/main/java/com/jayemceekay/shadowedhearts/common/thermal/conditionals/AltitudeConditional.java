package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AltitudeConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/altitude");
    public static void register() { OperationalTempRegistry.register(ID, new AltitudeConditional()); }

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return -30; }

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;
        int sea = level.getSeaLevel();
        double dy = player.getY() - sea;
        // Only apply above sea level; below handled by underground logic
        if (dy <= 0) return 0.0f;
        double targetOffset = -0.0065 * dy; // °C relative to sea-level ambient
        float coeff = 0.02f;
        float delta = (float) (targetOffset * coeff);
        if (delta < -0.5f) delta = -0.5f; // clamp extremes at very high altitudes
        return delta;
    }
}
