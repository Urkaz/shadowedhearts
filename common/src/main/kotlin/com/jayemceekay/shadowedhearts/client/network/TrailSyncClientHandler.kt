package com.jayemceekay.shadowedhearts.client.network

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.jayemceekay.shadowedhearts.client.trail.TrailClientState
import com.jayemceekay.shadowedhearts.network.trail.TrailSyncS2CPacket
import net.minecraft.client.Minecraft
import net.minecraft.core.particles.ParticleTypes

/**
 * Applies TrailSyncS2CPacket to client state.
 */
object TrailSyncClientHandler : ClientNetworkPacketHandler<TrailSyncS2CPacket> {
    override fun handle(packet: TrailSyncS2CPacket, client: Minecraft) {
        client.execute {
            TrailClientState.sync(packet.nodes, packet.hotspot)
            // Small immediate visual hint so players see something right away
            val level = client.level ?: return@execute
            // Sprinkle a couple particles at each node (clamped for safety)
            val nodes = packet.nodes.take(12)
            nodes.forEach { p ->
                repeat(2) {
                    level.addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        p.x + 0.5 + (Math.random() - 0.5) * 0.2,
                        p.y + 0.4 + Math.random() * 0.6,
                        p.z + 0.5 + (Math.random() - 0.5) * 0.2,
                        0.0, 0.02, 0.0
                    )
                }
            }
            packet.hotspot?.let { h ->
                repeat(6) {
                    level.addParticle(
                        ParticleTypes.END_ROD,
                        h.x + 0.5 + (Math.random() - 0.5) * 0.4,
                        h.y + 0.8 + Math.random() * 0.4,
                        h.z + 0.5 + (Math.random() - 0.5) * 0.4,
                        0.0, 0.02, 0.0
                    )
                }
            }
        }
    }
}
