package com.addemann03.player_morphs

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.entity.Player

interface Ability<M: Morph<*>> {
    var activated: Boolean

    /** Namnet på förmågan */
    val name: String

    /** Cooldown i millisekunder (0 om ingen cooldown) */
    val cooldownMs: Long

    /** 
     * Körs när spelaren aktiverar förmågan (t.ex. vid högerklick eller kommando/sneak).
     * @return true om förmågan aktiverades framgångsrikt, annars false.
     */
    fun onActivate(player: Player): Boolean

    /** Körs när morfen aktiveras (bra för passiva effekter, potions etc.) */
    fun onMorph(player: Player) {}

    /** Körs när morfen avaktiveras (ta bort passiva effekter etc.) */
    fun onUnmorph(player: Player) {}
    fun attach(morph: M)
    fun detach(): M
    fun onDeactivate(player: Player)
}