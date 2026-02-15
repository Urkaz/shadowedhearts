package com.jayemceekay.shadowedhearts.network.purification

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.jayemceekay.shadowedhearts.Shadowedhearts
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import java.util.*

/**
 * Client -> Server: requests to fully purify the Pokémon at the center of the purification chamber.
 */
class PurifyPokemonPacket(
    val purificationStoreID: UUID,
    val setIndex: Int
) : NetworkPacket<PurifyPokemonPacket> {
    override val id: ResourceLocation = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeUUID(purificationStoreID)
        buffer.writeInt(setIndex)
    }

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Shadowedhearts.MOD_ID, "purify_pokemon")
        fun decode(buffer: RegistryFriendlyByteBuf) = PurifyPokemonPacket(
            buffer.readUUID(),
            buffer.readInt()
        )
    }
}
