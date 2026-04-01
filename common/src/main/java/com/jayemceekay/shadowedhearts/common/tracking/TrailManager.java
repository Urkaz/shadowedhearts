package com.jayemceekay.shadowedhearts.common.tracking;

import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Manages per-player trail sessions and simple v1 generation.
 */
public final class TrailManager {
    private static final Map<UUID, TrailSession> SESSIONS = new HashMap<>();
    private static final Random RANDOM = new Random();

    private TrailManager() {}

    public static Optional<TrailSession> get(UUID playerId) {
        return Optional.ofNullable(SESSIONS.get(playerId));
    }

    public static TrailSession startOrReset(ServerPlayer player, int steps) {
        TrailSession session = new TrailSession(player.getUUID());
        session.setNodes(generatePath((ServerLevel) player.level(), player.blockPosition(), steps));
        SESSIONS.put(player.getUUID(), session);
        return session;
    }

    public static void clear(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    private static List<TrailNode> generatePath(ServerLevel level, BlockPos start, int steps) {
        steps = Math.max(2, Math.min(4, steps));
        List<TrailNode> out = new ArrayList<>(steps);
        BlockPos cursor = start;
        for (int i = 0; i < steps; i++) {
            // pick a direction and distance
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            int minDist = ShadowedHeartsConfigs.getInstance().getShadowConfig().trailMinNodeDistance();
            int maxDist = ShadowedHeartsConfigs.getInstance().getShadowConfig().trailMaxNodeDistance();
            int dist = minDist + RANDOM.nextInt(Math.max(1, maxDist - minDist + 1));
            int dx = (int) Math.round(Math.cos(angle) * dist);
            int dz = (int) Math.round(Math.sin(angle) * dist);
            BlockPos candidate = cursor.offset(dx, 0, dz);
            // find surface height around candidate (within small search window)
            BlockPos surface = findSurface(level, candidate);
            out.add(new TrailNode(surface));
            cursor = surface;
        }
        return out;
    }

    private static BlockPos findSurface(Level level, BlockPos around) {
        int x = around.getX();
        int z = around.getZ();
        // Use world height query if available, else simple downward scan
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= 0) {
            // Fallback: scan downward from near world top
            y = Math.min(level.getMaxBuildHeight() - 1, around.getY() + 16);
            while (y > level.getMinBuildHeight() && level.getBlockState(new BlockPos(x, y, z)).isAir()) y--;
        }
        // Place node slightly above ground for visibility
        return new BlockPos(x, Math.max(y + 1, level.getMinBuildHeight() + 1), z);
    }
}
