package com.jayemceekay.shadowedhearts.network.trail

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.jayemceekay.shadowedhearts.Shadowedhearts
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation

/**
 * Server -> Client: sync current trail nodes and the active evidence hotspot (if any).
 */
class TrailSyncS2CPacket(
    val nodes: List<BlockPos>,
    val hotspot: BlockPos?
) : NetworkPacket<TrailSyncS2CPacket> {
    override val id: ResourceLocation = ID

    override fun encode(buf: RegistryFriendlyByteBuf) {
        buf.writeVarInt(nodes.size)
        for (p in nodes) {
            buf.writeBlockPos(p)
        }
        if (hotspot != null) {
            buf.writeBoolean(true)
            buf.writeBlockPos(hotspot)
        } else {
            buf.writeBoolean(false)
        }
    }

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Shadowedhearts.MOD_ID, "trail_sync")

        @JvmStatic
        fun decode(buf: RegistryFriendlyByteBuf): TrailSyncS2CPacket {
            val count = buf.readVarInt()
            val nodes = ArrayList<BlockPos>(count)
            repeat(count) {
                nodes.add(buf.readBlockPos())
            }
            val hasHotspot = buf.readBoolean()
            val hotspot = if (hasHotspot) buf.readBlockPos() else null
            return TrailSyncS2CPacket(nodes, hotspot)
        }
    }
}
