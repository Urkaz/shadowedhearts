package com.jayemceekay.shadowedhearts.network.trail

import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.jayemceekay.shadowedhearts.common.shadow.ShadowAspectUtil
import com.jayemceekay.shadowedhearts.common.shadow.ShadowMoveUtil
import com.jayemceekay.shadowedhearts.common.shadow.ShadowService
import com.jayemceekay.shadowedhearts.common.tracking.TrailManager
import com.jayemceekay.shadowedhearts.config.HeartGaugeConfig
import com.jayemceekay.shadowedhearts.network.ShadowedHeartsNetwork
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import kotlin.random.Random

/**
 * Progresses the player's trail session when they complete a hotspot scan.
 */
object EvidenceScanCompleteHandler : ServerNetworkPacketHandler<EvidenceScanCompleteC2SPacket> {
    override fun handle(packet: EvidenceScanCompleteC2SPacket, server: MinecraftServer, player: ServerPlayer) {
        val sessionOpt = TrailManager.get(player.uuid)
        if (sessionOpt.isEmpty) return
        val session = sessionOpt.get()
        val hotspot = session.currentHotspot ?: return

        val playerPos = player.blockPosition()
        val dx = (playerPos.x + 0.5) - (hotspot.pos().x + 0.5)
        val dy = (playerPos.y + 0.5) - (hotspot.pos().y + 0.5)
        val dz = (playerPos.z + 0.5) - (hotspot.pos().z + 0.5)
        val within = (dx * dx + dy * dy + dz * dz) <= (hotspot.radius() * hotspot.radius())
        if (!within) return

        // advance
        session.markScanned()
        if (session.hasMore()) {
            // Simple v1 hotspot event roll: 20% chance to drop a small item reward
            if (Random.nextFloat() < 0.2f) {
                val level = player.serverLevel()
                val dropPos = player.blockPosition()
                val stack = when (Random.nextInt(3)) {
                    0 -> net.minecraft.world.item.ItemStack(Items.GLOWSTONE_DUST, 2)
                    1 -> net.minecraft.world.item.ItemStack(Items.AMETHYST_SHARD, 1)
                    else -> net.minecraft.world.item.ItemStack(Items.QUARTZ, 1)
                }
                val itemEntity = ItemEntity(
                    level,
                    dropPos.x + 0.5,
                    dropPos.y + 1.0,
                    dropPos.z + 0.5,
                    stack
                )
                level.addFreshEntity(itemEntity)
            }
            val next = session.advanceToNextHotspot(2.5f)
            val nodes = session.nodes.subList(session.index, session.nodes.size).map { it.pos() }
            ShadowedHeartsNetwork.sendToPlayer(player, TrailSyncS2CPacket(nodes, next?.pos()))
        } else {
            // Final manifestation: spawn a nearby hostile shadow pokemon
            val level = player.serverLevel()
            val spawnAt = player.blockPosition().offset(2, 0, 2)

            val species = PokemonSpecies.random()
            val properties = PokemonProperties.parse("species=${species.resourceIdentifier} level=20")
            val entity = properties.createEntity(level)
            val pokemon = entity.pokemon

            entity.moveTo(spawnAt.x + 0.5, spawnAt.y + 1.0, spawnAt.z + 0.5, player.yBodyRot, 0f)

            ShadowService.setShadow(pokemon, entity, true)
            ShadowService.setHeartGauge(pokemon, entity, HeartGaugeConfig.getMax(pokemon))
            ShadowAspectUtil.ensureRequiredShadowAspects(pokemon)
            ShadowMoveUtil.assignShadowMoves(pokemon)

            level.addFreshEntity(entity)

            TrailManager.clear(player.uuid)
            // Clear trail display on client
            ShadowedHeartsNetwork.sendToPlayer(player, TrailSyncS2CPacket(emptyList(), null))
        }
    }
}
