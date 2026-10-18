package com.addemann03.player_morphs.abilities

import com.addemann03.player_morphs.Ability
import com.addemann03.player_morphs.AbstractAbility
import com.addemann03.player_morphs.morphs.ZombieMorph
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.entity.Player

class BreakDoorAbility : AbstractAbility<ZombieMorph>("break_door") {
    override val cooldownMs = 3000L

    override fun onActivate(player: Player): Boolean {
        TODO("Not yet implemented")
    }

    override fun onDeactivate(player: Player) {
        TODO("Not yet implemented")
    }
}