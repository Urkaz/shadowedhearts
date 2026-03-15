package com.jayemceekay.shadowedhearts.network.aura

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.jayemceekay.shadowedhearts.Shadowedhearts
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation

data class AuraLockC2SPacket(val entityId: Int, val durationTicks: Int) : NetworkPacket<AuraLockC2SPacket> {
    override val id: ResourceLocation = ID

    override fun encode(buf: RegistryFriendlyByteBuf) {
        buf.writeVarInt(entityId)
        buf.writeVarInt(durationTicks)
    }

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Shadowedhearts.MOD_ID, "aura_lock_request")

        @JvmStatic
        fun decode(buf: RegistryFriendlyByteBuf): AuraLockC2SPacket {
            val id = buf.readVarInt()
            val dur = buf.readVarInt()
            return AuraLockC2SPacket(id, dur)
        }
    }
}
