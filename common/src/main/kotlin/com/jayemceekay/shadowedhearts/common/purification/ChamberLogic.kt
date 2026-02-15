package com.jayemceekay.shadowedhearts.common.purification

import com.cobblemon.mod.common.pokemon.Pokemon
import com.jayemceekay.shadowedhearts.common.shadow.ShadowAspectUtil
import com.jayemceekay.shadowedhearts.common.shadow.ShadowService
import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs
import com.jayemceekay.shadowedhearts.storage.purification.PurificationChamberStore

object ChamberLogic {

    fun advanceSteps(store: PurificationChamberStore, steps: Int, stepAccumulator: Int): Int {
        var currentAccumulator = stepAccumulator
        if (steps <= 0) return currentAccumulator
        val stepRequirement = ShadowedHeartsConfigs.getInstance().shadowConfig.purificationChamberStepRequirement()
        currentAccumulator += steps
        val affectedPokemon = mutableSetOf<Pokemon>()
        while (currentAccumulator >= stepRequirement) {
            currentAccumulator -= stepRequirement
            applyChamberTick(store, affectedPokemon)
        }
        affectedPokemon.forEach { ShadowService.syncAll(it) }
        return currentAccumulator
    }

    private fun applyChamberTick(store: PurificationChamberStore, affectedPokemon: MutableSet<Pokemon>? = null) {
        var anyChanged = false
        
        // We need to iterate over sets. Since sets are private in Store, we might need a way to access them.
        // For now, I'll assume we'll add a way to access sets in Store or move the logic into a more integrated way.
        // Actually, let's look at how we can get the sets.
        
        val setsCount = PurificationChamberStore.SETS
        val perfectCount = (0 until setsCount).count { setIndex ->
            val ring = store.getSupportRing(setIndex)
            PurificationMath.isPerfectSet(ring)
        }

        for (setIndex in 0 until setsCount) {
            val shadow = store.getShadow(setIndex)
            val supports = store.getSupports(setIndex)
            val delta = PurificationMath.computePurificationDeltaForSet(shadow, supports, perfectCount)
            
            if (delta != 0 && shadow != null) {
                val shadowMon = shadow as Pokemon
                val cur = ShadowAspectUtil.getHeartGaugeMeter(shadowMon)
                val next = cur + delta
                if (affectedPokemon != null) {
                    ShadowService.setHeartGauge(shadowMon, null, next, false)
                    affectedPokemon.add(shadowMon)
                } else {
                    ShadowService.setHeartGauge(shadowMon, null, next)
                }
                anyChanged = true
            }
        }
        
        if (anyChanged) {
            store.touch()
        }
    }
}
