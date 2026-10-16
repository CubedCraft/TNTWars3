package com.addemann03.player_morphs.managers

import com.addemann03.player_morphs.Morph

class AbilityManager {
    fun activateAbilities(morph: Morph<*>) {
        morph.activateAbilities()
    }

    fun deactivateAbilities(morph: Morph<*>) {
        morph.deactivateAbilities()
    }
}