package com.jayemceekay.shadowedhearts.network

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import net.minecraft.client.Minecraft

object PokemonPropertyUpdateHandler : ClientNetworkPacketHandler<PokemonPropertyUpdatePacket> {
    override fun handle(packet: PokemonPropertyUpdatePacket, client: Minecraft) {
        val pokemon = packet.pokemon() ?: return
        packet.set(pokemon, packet.value)
    }
}
