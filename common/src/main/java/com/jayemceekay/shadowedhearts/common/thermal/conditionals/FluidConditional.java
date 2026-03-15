package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public final class FluidConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/fluids");

    public static void register() { OperationalTempRegistry.register(ID, new FluidConditional()); }

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public int priority() { return 10; } // after ambience

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;
        var pos = player.blockPosition();
        FluidState fs = level.getFluidState(pos);

        // Immersed effects
        if (!fs.isEmpty()) {
            if (fs.is(FluidTags.WATER)) return -1.2f; // strong cooling when immersed
            if (fs.is(FluidTags.LAVA))  return +4.0f;  // very strong heating when immersed
        }

        float radiant = 0.0f;
        // Adjacent lava radiant heat (not immersed)
        for (var dir : net.minecraft.core.Direction.values()) {
            if (dir == net.minecraft.core.Direction.UP || dir == net.minecraft.core.Direction.DOWN) continue;
            var s = level.getBlockState(pos.relative(dir));
            if (s.getFluidState().is(FluidTags.LAVA)) {
                radiant += 0.2f;
            }
        }

        // Note: rain cooling is handled by WetnessConditional to avoid double counting.
        return radiant;
    }
}
