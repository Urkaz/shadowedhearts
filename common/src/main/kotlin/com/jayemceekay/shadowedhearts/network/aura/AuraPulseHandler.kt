package com.jayemceekay.shadowedhearts.network.aura

import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.jayemceekay.shadowedhearts.common.aura.AuraReaderCharge
import com.jayemceekay.shadowedhearts.common.shadow.ShadowPokemonData
import com.jayemceekay.shadowedhearts.common.tracking.TrailManager
import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs
import com.jayemceekay.shadowedhearts.content.items.AuraReaderItem
import com.jayemceekay.shadowedhearts.integration.accessories.SnagAccessoryBridgeHolder
import com.jayemceekay.shadowedhearts.network.AuraBroadcastQueue
import com.jayemceekay.shadowedhearts.network.ShadowedHeartsNetwork
import com.jayemceekay.shadowedhearts.network.trail.TrailSyncS2CPacket
import com.jayemceekay.shadowedhearts.registry.util.ModItemComponents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object AuraPulseHandler : ServerNetworkPacketHandler<AuraPulsePacket> {
    override fun handle(packet: AuraPulsePacket, server: MinecraftServer, player: ServerPlayer) {
        val auraReader = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(player)

        if (!auraReader.isEmpty && auraReader.item is AuraReaderItem) {
            val isActive = auraReader.get(ModItemComponents.AURA_SCANNER_ACTIVE.get()) ?: false
            if (isActive) {
                // Pulse costs some charge, say 200 ticks worth (10 seconds)
                AuraReaderCharge.consume(auraReader, 200, AuraReaderItem.MAX_CHARGE)

                // Start/restart a short trail session for this player (2-4 steps)
                val steps = 2 + (Math.random() * 3.0).toInt() // 2..4
                val session = TrailManager.startOrReset(player, steps)
                
                // After a few seconds, reveal the trail and send the message
                val executor = CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
                CompletableFuture.runAsync({
                    server.execute {
                        player.sendSystemMessage(
                            Component.literal("The Aura Reader detects faint traces of shadow energy. A Shadow Pokemon is near!")
                                .withStyle(ChatFormatting.DARK_PURPLE)
                        )
                        val hotspot = session.advanceToNextHotspot(2.5f)
                        val nodes = session.nodes.subList(session.index, session.nodes.size).map { it.pos() }
                        ShadowedHeartsNetwork.sendToPlayer(player, TrailSyncS2CPacket(nodes, hotspot?.pos()))
                    }
                }, executor)

                // Maintain legacy: also ping nearby shadow entities with a delayed aura pulse for compatibility
                val shadowRange = ShadowedHeartsConfigs.getInstance().shadowConfig.auraScannerShadowRange()
                val entities: List<Entity> = player.level().getEntities(null, player.boundingBox.inflate(shadowRange.toDouble()))
                for (entity in entities) {
                    if (entity is PokemonEntity && ShadowPokemonData.isShadow(entity)) {
                        AuraBroadcastQueue.queueBroadcast(entity, 2.5f, 100, 100)
                    }
                }
            }
        }
    }
}
