package com.jayemceekay.shadowedhearts.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokedex.scanning.PokemonScannedEvent;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.pokedex.ServerConfirmedRegisterPacket;
import com.cobblemon.mod.common.net.messages.server.pokedex.scanner.FinishScanningPacket;
import com.cobblemon.mod.common.net.serverhandling.pokedex.scanner.FinishScanningHandler;
import com.cobblemon.mod.common.pokedex.scanner.PlayerScanningDetails;
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData;
import com.cobblemon.mod.common.pokedex.scanner.PokemonScanner;
import com.cobblemon.mod.common.pokedex.scanner.ScannableEntity;
import com.jayemceekay.shadowedhearts.client.gui.AuraReaderManager;
import com.jayemceekay.shadowedhearts.common.aura.PokedexUsageContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FinishScanningHandler.class)
public class MixinFinishScanningHandler {

    @WrapMethod(method = "handle(Lcom/cobblemon/mod/common/net/messages/server/pokedex/scanner/FinishScanningPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V")
    private void shadowedhearts$onFinishScanning(FinishScanningPacket packet, MinecraftServer server, ServerPlayer player, Operation<Void> original) {
        if(AuraReaderManager.isActive() && AuraReaderManager.currentMode == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER) {
            Entity targetEntity = player.level().getEntity(packet.getTargetedId());
            if (targetEntity == null) {
                return;
            }

            if (PokemonScanner.INSTANCE.isEntityInRange(player, targetEntity, packet.getZoomLevel())) {
                java.util.UUID inProgressUUID = PlayerScanningDetails.INSTANCE.getPlayerToEntityMap().get(player.getUUID());
                Integer progressTick = PlayerScanningDetails.INSTANCE.getPlayerToTickMap().get(player.getUUID());

                if (progressTick == null) {
                    return;
                }

                int ticksScan = server.getTickCount() - progressTick;

                if (targetEntity.getUUID().equals(inProgressUUID) && ticksScan >= PokedexUsageContext.SUCCESS_SCAN_SERVER_TICKS) {
                    if (!(targetEntity instanceof ScannableEntity scannableEntity)) {
                        return;
                    }

                    PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
                    PokedexEntityData pokedexEntityData = scannableEntity.resolvePokemonScan();

                    if (pokedexEntityData != null) {
                        var newInformation = dex.getNewInformation(pokedexEntityData);

                        if (scannableEntity instanceof PokemonEntity pokemonEntity && pokemonEntity.getOwner() == player) {
                            dex.obtain(pokemonEntity.getPokemon());
                        } else {
                            dex.encounter(pokedexEntityData);
                        }

                        CobblemonEvents.POKEMON_SCANNED.post(new PokemonScannedEvent(player, pokedexEntityData, scannableEntity));
                        new ServerConfirmedRegisterPacket(pokedexEntityData.getApparentSpecies().getResourceIdentifier(), newInformation).sendToPlayer(player);
                    }
                }
            }
        } else {
            original.call(packet, server, player);
        }
    }
}
