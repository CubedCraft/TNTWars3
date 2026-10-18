package com.addemann03.player_morphs

abstract class AbstractAbility<M: Morph<*>>(final override val name: String) : Ability<M> {
    private var morph: M? = null
    final override fun attach(morph: M) {
        this.morph = morph
    }

    final override fun detach(): M {
        val morph = this.morph
        this.morph = null
        return morph ?: throw Exception("")
    }

    override var activated = false
}