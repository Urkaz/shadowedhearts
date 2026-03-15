package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import com.jayemceekay.shadowedhearts.common.thermal.ThermalSourceRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class NearbyThermalSourcesConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/nearby_sources");

    public static void register() {
        OperationalTempRegistry.register(ID, new NearbyThermalSourcesConditional());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;
        var pos = player.position().toVector3f();
        float additive = ThermalSourceRegistry.computeAdditiveFromSources(level, pos);

        // Zero out contributions for unlit blocks that were registered as heat sources
        // (approximate correction): subtract if nearby block is present but unlit
        // We apply a small correction only when directly adjacent to reduce double counting.
        var state = level.getBlockState(player.blockPosition());
        if (state.hasProperty(BlockStateProperties.LIT) && !Boolean.TRUE.equals(state.getValue(BlockStateProperties.LIT))) {
            // subtract a small amount since registry counts general presence
            additive *= 0.9f;
        }

        // Furnaces/campfires at feet or head level: if unlit, reduce local heat slightly
        for (int dy = 0; dy <= 1; dy++) {
            var s = level.getBlockState(player.blockPosition().above(dy));
            if ((s.is(Blocks.FURNACE) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.SMOKER) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE))
                    && s.hasProperty(BlockStateProperties.LIT) && !Boolean.TRUE.equals(s.getValue(BlockStateProperties.LIT))) {
                additive *= 0.85f;
            }
        }
        // Simple short-range occlusion and soft falloff dampening
        var basePos = player.blockPosition();
        for (var dir : net.minecraft.core.Direction.values()) {
            if (dir == net.minecraft.core.Direction.UP || dir == net.minecraft.core.Direction.DOWN) continue;
            var p1 = basePos.relative(dir);
            if (!level.isEmptyBlock(p1)) {
                // A solid block between player and sources tends to block; dampen slightly
                additive *= 0.95f;
            }
        }
        // Global soft damping to avoid overstacking from many sources
        additive *= 0.9f;

        // Convert additive °C into per-tick delta using assumed proportionality,
        // and cap the implied equilibrium to prevent runaway stacking.
        final float K_ASSUMED_PER_TICK = 0.03f;
        float targetEq = Mth.clamp(currentTempC + additive, -40.0f, 130.0f);
        float clampedAdditive = targetEq - currentTempC;
        return clampedAdditive * K_ASSUMED_PER_TICK;
    }
}
