package com.addemann03.player_morphs.managers

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.MorphTypes
import org.bukkit.entity.Player
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

data class MorphData<M: Morph<*>>(
    val clazz: KClass<out M>,
    val morph: M
) {
    companion object {
        fun <M: Morph<*>>of(morph: M): MorphData<M> {
            return MorphData(morph::class, morph)
        }
    }
}

object MorphManager {
    val morphs = mutableListOf<MorphData<Morph<*>>>()
    fun isMorphed(player: Player) = morphs.find { it.morph.player == player } != null
    fun morph(morphType: MorphTypes<*>, player: Player) {
        val morphClass = morphType.type.morphClass
        val morph = morphClass.createInstance()
        morph.morph(player)
        morphs.add(MorphData.of(morph))
    }

    fun unmorph(player: Player) {
        val morphData = getMorphData(player)
        if(morphData != null) {
            val (clazz, morph) = morphData
            morph.unmorph()
            morphs.remove(morphData)
        }
    }

    fun getMorphData(player: Player): MorphData<Morph<*>>? {
        return morphs.find { it.morph.player == player }
    }
}