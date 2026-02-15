package com.jayemceekay.shadowedhearts.network.snag

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.jayemceekay.shadowedhearts.client.snag.ClientSnagState
import net.minecraft.client.Minecraft

object SnagArmedHandler : ClientNetworkPacketHandler<SnagArmedPacket> {
    override fun handle(packet: SnagArmedPacket, client: Minecraft) {
        ClientSnagState.setArmed(packet.armed)
    }
}
