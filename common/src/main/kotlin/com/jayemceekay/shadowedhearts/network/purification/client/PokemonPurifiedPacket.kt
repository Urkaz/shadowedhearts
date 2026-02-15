package com.jayemceekay.shadowedhearts.network.purification.client

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.jayemceekay.shadowedhearts.Shadowedhearts
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import java.util.*

/**
 * Server -> Client: Informs the client that a Pokémon in the purification chamber has been purified.
 */
class PokemonPurifiedPacket(
    val purificationStoreID: UUID,
    val setIndex: Int,
    val slotIndex: Int
) : NetworkPacket<PokemonPurifiedPacket> {
    override val id: ResourceLocation = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeUUID(purificationStoreID)
        buffer.writeInt(setIndex)
        buffer.writeInt(slotIndex)
    }

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Shadowedhearts.MOD_ID, "pokemon_purified")
        fun decode(buffer: RegistryFriendlyByteBuf) = PokemonPurifiedPacket(
            buffer.readUUID(),
            buffer.readInt(),
            buffer.readInt()
        )
    }
}
