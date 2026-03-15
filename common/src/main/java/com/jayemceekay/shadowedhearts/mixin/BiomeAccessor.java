package com.jayemceekay.shadowedhearts.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to invoke the private (deprecated) Biome#getTemperature(BlockPos) method
 * which performs cached, height-adjusted temperature calculation in vanilla.
 */
@Mixin(Biome.class)
public interface BiomeAccessor {

    /**
     * Invokes the private getTemperature(BlockPos) method on Biome.
     *
     * Usage:
     *   float temp = ((BiomeAccessor) (Object) biome).shadowedhearts$invokeGetTemperature(pos);
     */
    @Deprecated
    @Invoker("getTemperature")
    float shadowedhearts$invokeGetTemperature(BlockPos pos);
}
