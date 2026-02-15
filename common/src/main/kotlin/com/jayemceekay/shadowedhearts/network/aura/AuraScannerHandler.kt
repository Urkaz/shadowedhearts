package com.jayemceekay.shadowedhearts.network.aura

import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.jayemceekay.shadowedhearts.integration.accessories.SnagAccessoryBridgeHolder
import com.jayemceekay.shadowedhearts.registry.util.ModItemComponents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object AuraScannerHandler : ServerNetworkPacketHandler<AuraScannerC2SPacket> {
    override fun handle(packet: AuraScannerC2SPacket, server: MinecraftServer, player: ServerPlayer) {
        val auraReader = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(player)
        if (!auraReader.isEmpty) {
            auraReader.set(ModItemComponents.AURA_SCANNER_ACTIVE.get(), packet.active)
        }
    }
}
