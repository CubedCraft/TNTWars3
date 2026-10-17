package com.addemann03.player_morphs

import com.addemann03.player_morphs.morphs.CreeperMorph
import com.addemann03.player_morphs.morphs.PiglinMorph
import com.addemann03.player_morphs.morphs.SkeletonMorph
import com.addemann03.player_morphs.morphs.ZombieMorph

class MorphTypes<T: Morph.MorphType<*>>(val name: String, val type: T, val ordinal: Int) {
    companion object {
        val REGISTRY = HashMap<String, Morph.MorphType<*>>()
        val VALUES = mutableListOf<MorphTypes<Morph.MorphType<*>>>()

        // Fördefinierade "enum-värden" med reified generics
        val PIGLIN = define("PIGLIN", PiglinMorph.Companion)
        val ZOMBIE = define<ZombieMorph>("ZOMBIE",ZombieMorph.Companion)
        val SKELETON = define<SkeletonMorph>("SKELETON", SkeletonMorph.Companion)
        val CREEPER = define("CREEPER", CreeperMorph.Companion)

        inline fun <reified M : Morph<*>> define(name: String,morphType: Morph.MorphType<M>): MorphTypes<Morph.MorphType<M>> {
            REGISTRY[name] = morphType

            val ordinal = REGISTRY.values.indexOf(morphType)
            val entry = MorphTypes(name, morphType, ordinal)
            VALUES.add(entry as MorphTypes<Morph.MorphType<*>>)
            return entry
        }

        // Fungerar som Enum.values()
        fun values(): List<MorphTypes<Morph.MorphType<*>>> = VALUES

        // Fungerar som Enum.valueOf()
        fun valueOf(name: String): MorphTypes<*>? = VALUES.find { it.name.equals(name, ignoreCase = true) }
    }
}