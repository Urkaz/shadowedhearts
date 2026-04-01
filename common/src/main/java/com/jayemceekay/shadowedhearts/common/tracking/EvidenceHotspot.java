package com.jayemceekay.shadowedhearts.common.tracking;

import net.minecraft.core.BlockPos;

/**
 * Invisible world interaction point that the player must scan to progress the trail.
 */
public record EvidenceHotspot(BlockPos pos, float radius) {
    public boolean isWithin(BlockPos other) {
        if (other == null) return false;
        double dx = other.getX() + 0.5 - (pos.getX() + 0.5);
        double dy = other.getY() + 0.5 - (pos.getY() + 0.5);
        double dz = other.getZ() + 0.5 - (pos.getZ() + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= (double) (radius * radius);
    }
}
