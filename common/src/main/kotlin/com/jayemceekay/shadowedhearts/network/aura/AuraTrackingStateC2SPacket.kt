package com.jayemceekay.shadowedhearts.network.aura

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.jayemceekay.shadowedhearts.Shadowedhearts
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation

/**
 * Client -> Server: notify current tracking (lock) active state for the Aura Scanner.
 */
data class AuraTrackingStateC2SPacket(val tracking: Boolean) : NetworkPacket<AuraTrackingStateC2SPacket> {
    override val id: ResourceLocation = ID

    override fun encode(buf: RegistryFriendlyByteBuf) {
        buf.writeBoolean(tracking)
    }

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Shadowedhearts.MOD_ID, "aura_tracking_state")

        @JvmStatic
        fun decode(buf: RegistryFriendlyByteBuf): AuraTrackingStateC2SPacket {
            return AuraTrackingStateC2SPacket(buf.readBoolean())
        }
    }
}
