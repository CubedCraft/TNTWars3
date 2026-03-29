package com.jeroenvdg.tntwars.player.states.playerGameStates

import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.GameMode
import org.bukkit.event.player.PlayerInteractEvent

class PlayerGameSpectateState(user: TNTWarsPlayer) : BasePlayerGameState(user) {

    override val flyEnabled = true

    override fun onActivate() {
        player.gameMode = GameMode.ADVENTURE
        super.onActivate()
        user.isGodMode = true
    }

    override fun onDeactivate() {
        super.onDeactivate()
        user.isGodMode = false
    }

    override fun onDeath(deathContext: PlayerDeathContext) { }

    private fun handleInteract(event: PlayerInteractEvent) {
        event.isCancelled = true
    }
}