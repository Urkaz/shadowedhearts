package com.jayemceekay.shadowedhearts.network.trail

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.jayemceekay.shadowedhearts.Shadowedhearts
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation

/**
 * Client -> Server: notifies that the player completed scanning the current hotspot.
 * For v1, payload-free (server validates proximity).
 */
class EvidenceScanCompleteC2SPacket : NetworkPacket<EvidenceScanCompleteC2SPacket> {
    override val id: ResourceLocation = ID

    override fun encode(buf: RegistryFriendlyByteBuf) { /* no payload */ }

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Shadowedhearts.MOD_ID, "evidence_scan_complete")

        @JvmStatic
        fun decode(buf: RegistryFriendlyByteBuf): EvidenceScanCompleteC2SPacket = EvidenceScanCompleteC2SPacket()
    }
}
