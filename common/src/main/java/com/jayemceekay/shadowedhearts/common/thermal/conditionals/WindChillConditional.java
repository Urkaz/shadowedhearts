package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public final class WindChillConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/wind_chill");
    public static void register() { OperationalTempRegistry.register(ID, new WindChillConditional()); }
    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 15; }

    private double lastX, lastZ; private boolean init = false;

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;
        double vx = 0, vz = 0;
        if (init) {
            vx = player.getX() - lastX; vz = player.getZ() - lastZ;
        }
        lastX = player.getX(); lastZ = player.getZ(); init = true;

        double horizSpeed = Math.hypot(vx, vz); // blocks per tick
        int sky = level.getBrightness(LightLayer.SKY, player.blockPosition());
        float exposure = sky / 15.0f;
        // Stronger when cold; negligible when warm
        float coldFactor = currentTempC < 10 ? (10 - currentTempC) / 20.0f : 0.0f; // 0..0.5
        float delta = (float)(-horizSpeed * 0.4f * exposure * coldFactor);
        if (delta < -0.15f) delta = -0.15f; // cap per tick
        return delta;
    }
}
