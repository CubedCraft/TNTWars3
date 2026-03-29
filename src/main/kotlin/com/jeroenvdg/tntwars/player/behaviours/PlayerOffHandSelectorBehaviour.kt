package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.interfaces.ItemSelector
import com.jeroenvdg.tntwars.player.PlayerBehaviour
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class PlayerOffHandSelectorBehaviour(user: TNTWarsPlayer) : PlayerBehaviour(user) {

    override fun onActivate() {
        user.onHandItemSwap += ::handleHandItemSwap
    }

    override fun onDeactivate() {
        user.onHandItemSwap -= ::handleHandItemSwap
    }

    private fun handleHandItemSwap(event: PlayerSwapHandItemsEvent) {
        ItemSelector.instance.open(player)
        event.isCancelled = true
    }

}