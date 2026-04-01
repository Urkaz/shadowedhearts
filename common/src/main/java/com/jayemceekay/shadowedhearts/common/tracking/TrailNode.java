package com.jayemceekay.shadowedhearts.common.tracking;

import net.minecraft.core.BlockPos;

/**
 * A simple waypoint in the world used to render/guide the Aura Reader trail.
 * V1 uses block-centered positions to keep serialization light.
 */
public record TrailNode(BlockPos pos) {
}
