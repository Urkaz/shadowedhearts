package com.jayemceekay.shadowedhearts.network.aura

import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.jayemceekay.shadowedhearts.common.aura.AuraLockManager
import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object AuraLockHandler : ServerNetworkPacketHandler<AuraLockC2SPacket> {
    override fun handle(packet: AuraLockC2SPacket, server: MinecraftServer, player: ServerPlayer) {
        val level = player.level()
        val target = level.getEntity(packet.entityId) ?: return

        if (target !is PokemonEntity) return
        // Only wild non-NPC Pokémon
        if (target.pokemon.getOwnerUUID() != null || target.pokemon.isNPCOwned()) return

        val cfg = ShadowedHeartsConfigs.getInstance().shadowConfig
        val maxRange = cfg.auraLockRange().toDouble()

        if (player.distanceTo(target) > maxRange) return

        val now = level.gameTime
        AuraLockManager.applyLock(target, now, packet.durationTicks)
    }
}
