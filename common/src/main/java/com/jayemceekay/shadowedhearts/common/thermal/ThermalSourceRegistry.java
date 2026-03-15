package com.jayemceekay.shadowedhearts.common.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for thermal sources (blocks and entities) that radiate heat using a smooth quadratic falloff.
 * Heat values are additive contributions in °C at contact (d = 0), and fall off to 0 at the source's radius.
 * Values can be negative to represent cold sources.
 */
public final class ThermalSourceRegistry {

    public static final class SourceDef {
        public final float contactAdditiveC; // positive = heat, negative = cold
        public final float maxRange; // in blocks (acts as radius of influence)
        public SourceDef(float contactAdditiveC, float maxRange) {
            this.contactAdditiveC = contactAdditiveC;
            this.maxRange = Math.max(0.5f, maxRange);
        }
    }

    private static final Map<Block, SourceDef> BLOCK_SOURCES = new HashMap<>();
    private static final Map<ResourceLocation, SourceDef> ENTITY_SOURCES = new HashMap<>();
    private static volatile boolean defaultsRegistered = false;

    private ThermalSourceRegistry() {}

    public static void registerBlock(Block block, float heatAt1mCPerTick, float maxRange) {
        if (block == null) return;
        BLOCK_SOURCES.put(block, new SourceDef(heatAt1mCPerTick, maxRange));
    }

    public static void unregisterBlock(Block block) {
        BLOCK_SOURCES.remove(block);
    }

    public static void registerEntity(EntityType<?> type, float heatAt1mCPerTick, float maxRange) {
        if (type == null) return;
        ResourceLocation key = EntityType.getKey(type);
        if (key != null) ENTITY_SOURCES.put(key, new SourceDef(heatAt1mCPerTick, maxRange));
    }

    public static void unregisterEntity(EntityType<?> type) {
        if (type == null) return;
        ResourceLocation key = EntityType.getKey(type);
        if (key != null) ENTITY_SOURCES.remove(key);
    }

    public static Map<Block, SourceDef> blockSources() {
        ensureDefaults();
        return BLOCK_SOURCES;
    }

    public static Map<ResourceLocation, SourceDef> entitySources() {
        ensureDefaults();
        return ENTITY_SOURCES;
    }

    private static void ensureDefaults() {
        if (defaultsRegistered) return;
        synchronized (ThermalSourceRegistry.class) {
            if (defaultsRegistered) return;
            registerDefaults();
            defaultsRegistered = true;
        }
    }

    /**
     * Calculate net additive °C contribution from registered sources near the given center position.
     * This method is agnostic to lit state; the caller may zero out contributions for unlit blocks.
     *
     * The falloff used is:
     *   falloff(d, r) = max(0, 1 - d/r)^2
     * so it is 1 at contact (d=0) and 0 at/after the radius r, with a smooth curve.
     */
    public static float computeAdditiveFromSources(Level level, Vector3f center) {
        ensureDefaults();
        if (level == null || center == null) return 0.0f;

        float maxBlockRange = 0.0f;
        for (SourceDef def : BLOCK_SOURCES.values()) maxBlockRange = Math.max(maxBlockRange, def.maxRange);
        int radius = Math.max(1, (int)Math.ceil(maxBlockRange));

        float additive = 0.0f;

        // Blocks
        Vector3f mutable = new Vector3f();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(center.x() + dx, center.y() + dy, center.z() + dz);
                    BlockPos p = new BlockPos((int) mutable.x, (int) mutable.y, (int) mutable.z);
                    BlockState state = level.getBlockState(p);
                    if (state == null || state.isAir()) continue;
                    SourceDef def = BLOCK_SOURCES.get(state.getBlock());
                    if (def == null) continue;
                    // Distance from query center to the center of the block
                    double bx = p.getX() + 0.5;
                    double by = p.getY() + 0.5;
                    double bz = p.getZ() + 0.5;
                    double dxw = bx - center.x();
                    double dyw = by - center.y();
                    double dzw = bz - center.z();
                    double dist = Math.sqrt(dxw*dxw + dyw*dyw + dzw*dzw);
                    if (dist > def.maxRange) continue;
                    float t = falloff((float) dist, def.maxRange);
                    additive += def.contactAdditiveC * t;
                }
            }
        }

        // Entities
        float maxEntityRange = 0.0f;
        for (SourceDef def : ENTITY_SOURCES.values()) maxEntityRange = Math.max(maxEntityRange, def.maxRange);
        if (maxEntityRange > 0.5f) {
            double r = maxEntityRange;
            var aabb = new net.minecraft.world.phys.AABB(new BlockPos((int) center.x(), (int) center.y(), (int) center.z())).inflate(r, r, r);
            for (Entity e : level.getEntities(null, aabb)) {
                ResourceLocation key = EntityType.getKey(e.getType());
                SourceDef def = ENTITY_SOURCES.get(key);
                if (def == null) continue;
                double dx = (e.getX() - center.x());
                double dy = (e.getY() - center.y());
                double dz = (e.getZ() - center.z());
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist > def.maxRange) continue;
                float t = falloff((float) dist, def.maxRange);
                additive += def.contactAdditiveC * t;
            }
        }

        return additive;
    }

    private static float falloff(float d, float radius) {
        if (radius <= 0f) return 0f;
        float t = 1.0f - (d / radius);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t * t;
    }

    private static void registerDefaults() {
        // Baseline sources; values are additive °C at contact and quadratic falloff by radius
        // Heat sources
        registerBlock(net.minecraft.world.level.block.Blocks.FIRE, 20.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.LAVA, 45.0f, 5.0f); // decays fast
        registerBlock(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK, 10.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.TORCH, 4.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.WALL_TORCH, 4.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.REDSTONE_TORCH, 2.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.REDSTONE_WALL_TORCH, 2.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.SOUL_TORCH, 3.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.SOUL_WALL_TORCH, 3.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.CAMPFIRE, 20.0f, 7.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE, 20.0f, 7.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.FURNACE, 12.0f, 6.0f); // treated as lit; caller may dampen when unlit
        registerBlock(net.minecraft.world.level.block.Blocks.BLAST_FURNACE, 12.0f, 6.0f);
        registerBlock(net.minecraft.world.level.block.Blocks.SMOKER, 12.0f, 6.0f);

        // Cold sources (negative heat) — keep disabled until tuned on Celsius scale
        //registerBlock(net.minecraft.world.level.block.Blocks.POWDER_SNOW, -8.0f, 4.0f);
        //registerBlock(net.minecraft.world.level.block.Blocks.ICE, -5.0f, 3.0f);
        //registerBlock(net.minecraft.world.level.block.Blocks.PACKED_ICE, -6.0f, 3.0f);
        //registerBlock(net.minecraft.world.level.block.Blocks.BLUE_ICE, -8.0f, 3.0f);
        //registerBlock(net.minecraft.world.level.block.Blocks.SNOW_BLOCK, -3.0f, 3.0f);
    }
}
