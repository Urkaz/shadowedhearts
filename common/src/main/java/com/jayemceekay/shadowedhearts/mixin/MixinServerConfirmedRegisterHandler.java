package com.jayemceekay.shadowedhearts.mixin;

import com.cobblemon.mod.common.client.net.pokedex.ServerConfirmedRegisterHandler;
import com.cobblemon.mod.common.net.messages.client.pokedex.ServerConfirmedRegisterPacket;
import com.jayemceekay.shadowedhearts.client.gui.AuraReaderManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfirmedRegisterHandler.class)
public class MixinServerConfirmedRegisterHandler {

    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/client/pokedex/ServerConfirmedRegisterPacket;Lnet/minecraft/client/Minecraft;)V", at = @At("TAIL"))
    private void shadowedhearts$onRegister(ServerConfirmedRegisterPacket packet, Minecraft client, CallbackInfo ci) {
        if(AuraReaderManager.isActive() && AuraReaderManager.currentMode == AuraReaderManager.AuraScannerMode.POKEDEX_SCANNER) {
            AuraReaderManager.POKEDEX_USAGE_CONTEXT.onServerConfirmedRegister(packet);
        }
    }
}
