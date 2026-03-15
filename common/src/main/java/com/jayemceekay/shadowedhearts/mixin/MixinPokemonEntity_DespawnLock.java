package com.jayemceekay.shadowedhearts.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.jayemceekay.shadowedhearts.common.aura.AuraLockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonEntity.class)
public abstract class MixinPokemonEntity_DespawnLock {

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void shadowedhearts$preventAuraLockedDespawn(CallbackInfo ci) {
        PokemonEntity self = (PokemonEntity) (Object) this;
        long now = self.level().getGameTime();
        if (AuraLockManager.INSTANCE.isLocked(self, now)) {
            ci.cancel();
        }
    }
}
