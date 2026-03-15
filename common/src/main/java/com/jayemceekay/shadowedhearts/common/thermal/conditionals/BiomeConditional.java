package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import com.jayemceekay.shadowedhearts.mixin.BiomeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BiomeConditional implements OperationalTempConditional {
    public static final ResourceLocation ID = new ResourceLocation("shadowedhearts", "cond/biome");

    public static void register() { OperationalTempRegistry.register(ID, new BiomeConditional()); }

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public int priority() { return -100; } // early baseline

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;

        // Accentuate biome blending over a wider area (up to ~64 blocks),
        // sampling center plus 8 directions across multiple radii.
        // This mirrors the water->land shore logic range for consistency.
        double sumWeights = 0.0;
        double sumTemps   = 0.0;
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // Directions: NESW + diagonals
        final int[] dir8 = new int[] { 1,0, -1,0, 0,1, 0,-1, 1,1, 1,-1, -1,1, -1,-1 };
        // Radii in blocks to sample. 0=center, then rings out to 64.
        final int[] radii = new int[] { 0, 16, 32};

        for (int ri = 0; ri < radii.length; ri++) {
            int r = radii[ri];

            if (r == 0) {
                // Center sample once
                BlockPos samplePos = BlockPos.containing(px, py, pz);
                var holder = level.getBiome(samplePos);
                var b = holder.value();
                float t;
                try {
                    t = ((BiomeAccessor) (Object) b).shadowedhearts$invokeGetTemperature(samplePos);
                } catch (Throwable e) {
                    t = 0.8f;
                }
                double w = 1.0; // maximum weight at center
                // Preserve historical normalization pattern
                sumWeights += Math.sqrt(w);
                sumTemps   += w * t;
                continue;
            }

            for (int d = 0; d < dir8.length; d += 2) {
                int dx = dir8[d] * r;
                int dz = dir8[d + 1] * r;

                BlockPos samplePos = BlockPos.containing(px + dx, py, pz + dz);
                var holder = level.getBiome(samplePos);
                var b = holder.value();

                float t;
                try {
                    t = ((BiomeAccessor) (Object) b).shadowedhearts$invokeGetTemperature(samplePos);
                } catch (Throwable e) {
                    t = 0.8f;
                }

                // Smooth radial falloff by distance (scaled to 16-block steps)
                double scaled = (double) r / 32.0;
                double w = 1.0 / (1.0 + scaled * scaled);
                sumWeights += Math.sqrt(w);
                sumTemps   += w * t;
            }
        }

        float tBlend = sumWeights > 0 ? (float) (sumTemps / sumWeights) : 0.8f;

        // Map blended vanilla temp (~0..2) to ambient °C using the user-provided scale
        float ambient = mapVanillaTempToAmbientC(tBlend);

        // — Make water biomes depend more on nearby land —
        var centerHolder = level.getBiome(player.blockPosition());
        var keyOpt = centerHolder.unwrapKey();
        if (keyOpt.isPresent()) {
            String path = keyOpt.get().location().getPath();
            boolean isWater = path.contains("river") || path.contains("ocean") || path.contains("beach");

            if (isWater) {
                // 1) Probe in 8 directions out to a max radius to find nearest land biomes
                final int step = 8;            // blocks per step (cheap)
                final int maxR = 64;           // search up to 64 blocks from the player

                float landTempSum = 0.0f;
                float landWeightSum = 0.0f;
                int nearestLandDist = Integer.MAX_VALUE;

                for (int d = 0; d < dir8.length; d += 2) {
                    int vx = dir8[d];
                    int vz = dir8[d + 1];

                    for (int r = step; r <= maxR; r += step) {
                        BlockPos p = BlockPos.containing(player.getX() + vx * r, player.getY(), player.getZ() + vz * r);
                        var h = level.getBiome(p);
                        var kOpt = h.unwrapKey();
                        if (kOpt.isEmpty()) continue;
                        String pth = kOpt.get().location().getPath();
                        boolean waterHere = pth.contains("river") || pth.contains("ocean") || pth.contains("beach");

                        if (!waterHere) {
                            // Found land in this direction; record and stop along this ray
                            float tHere;
                            try {
                                // use vanilla full temperature at position (includes altitude)
                                tHere = ((BiomeAccessor) (Object) h.value()).shadowedhearts$invokeGetTemperature(p);
                            } catch (Throwable e) {
                                tHere = 0.8f;
                            }
                            float ambHere = mapVanillaTempToAmbientC(tHere);

                            // Weight closer shores higher. Quadratic falloff out to maxR.
                            float w = 1.0f - (float)r / (float)maxR; // 1 near, 0 far
                            if (w < 0f) w = 0f;
                            w = w * w; // emphasize proximity

                            landTempSum += ambHere * w;
                            landWeightSum += w;

                            if (r < nearestLandDist) nearestLandDist = r;
                            break; // stop marching this direction
                        }
                    }
                }

                if (landWeightSum > 0.0f) {
                    float landAmbient = landTempSum / landWeightSum;

                    // 2) Convert nearest shore distance into an influence factor (0..~1)
                    // Closer than 16 blocks → very strong land pull; further than 64 → weak.
                    float shoreInfluence;
                    if (nearestLandDist == Integer.MAX_VALUE) {
                        shoreInfluence = 0.0f; // open ocean, no land found nearby
                    } else {
                        float d = Math.max(0f, Math.min(maxR, nearestLandDist));
                        // piecewise: very strong within 16, taper to ~0 by 64
                        if (d <= 16f) shoreInfluence = 0.75f;
                        else if (d <= 32f) shoreInfluence = 0.5f;
                        else if (d <= 48f) shoreInfluence = 0.25f;
                        else shoreInfluence = 0.05f;
                    }

                    // 3) Blend water baseline toward nearby land baseline by shore influence
                    ambient = Mth.lerp(shoreInfluence, ambient, landAmbient);
                }
            }
        }
        // Determine aridity from the CENTER biome via precipitation (mod-friendly desert check)
        var centerBiome = level.getBiome(player.blockPosition()).value();
        boolean arid = false;
        try { arid = !centerBiome.hasPrecipitation(); } catch (Throwable ignored) {}
        //if (arid) ambient += 3.0f; // small baseline boost in arid biomes

        // Night cooling applies everywhere; stronger in arid biomes
        /*if (level.isNight()) {
            ambient -= arid ? 8.0f : 4.0f;
        }*/

        // Adaptive drift: larger differences converge faster, but clamp to stay stable
        float diff = Math.abs(ambient - currentTempC);
        float targetBias = Mth.clamp(diff * 0.002f, 0.01f, 0.05f);

        return (ambient - currentTempC) * targetBias;
    }

    // Maps Minecraft biome temperature to ambient °C baselines aligned with vanilla biome temps:
    // Snow biomes ~[-0.7..0.0], temperate rains ~[0.2..0.8], jungles ~0.95, deserts/savannas ~2.0.
    // Piecewise anchors (°C):
    //  t=-0.7 -> -25, 0.0 -> -8, 0.3 -> 5, 0.7 -> 15, 1.0 -> 24, 1.5 -> 34, 2.0 -> 44
    private static float mapVanillaTempToAmbientC(float t) {
        // Extreme cold below Frozen/Jagged Peaks baseline
        if (t <= -0.7f) return -27.0f;

        // Deep-freeze to freezing range: -0.7 .. 0.0 → -25 .. -8 °C
        if (t <= 0.0f) {
            float f = (t - (-0.7f)) / (0.0f - (-0.7f));
            return Mth.lerp(f, -25.0f, -8.0f);
        }

        // Chilly rain to cool: 0.0 .. 0.3 → -8 .. 5 °C
        if (t <= 0.3f) {
            float f = (t - 0.0f) / (0.3f - 0.0f);
            return Mth.lerp(f, -8.0f, 5.0f);
        }

        // Cool to mild temperate: 0.3 .. 0.7 → 5 .. 15 °C
        if (t <= 0.7f) {
            float f = (t - 0.3f) / (0.7f - 0.3f);
            return Mth.lerp(f, 5.0f, 15.0f);
        }

        // Mild to warm: 0.7 .. 1.0 → 15 .. 24 °C
        if (t <= 1.0f) {
            float f = (t - 0.7f) / (1.0f - 0.7f);
            return Mth.lerp(f, 15.0f, 24.0f);
        }

        // Warm to hot: 1.0 .. 1.5 → 24 .. 34 °C
        if (t <= 1.5f) {
            float f = (t - 1.0f) / (1.5f - 1.0f);
            return Mth.lerp(f, 24.0f, 34.0f);
        }

        // Very hot: 1.5 .. 2.0 → 34 .. 44 °C (clamped beyond)
        float f = Mth.clamp((t - 1.5f) / (2.0f - 1.5f), 0.0f, 1.0f);
        float mapped = Mth.lerp(f, 34.0f, 44.0f);
        return Mth.clamp(mapped, -35.0f, 50.0f);
    }
}
