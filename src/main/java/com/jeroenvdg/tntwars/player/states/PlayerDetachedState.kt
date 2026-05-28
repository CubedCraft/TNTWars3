package com.jeroenvdg.tntwars.player.states

import com.jeroenvdg.minigame_utilities.statemachine.State
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer

class PlayerDetachedState(tntWarsPlayer: TNTWarsPlayer) : BasePlayerState(tntWarsPlayer) {
    override fun onDeactivate() {
        super.onDeactivate()

        val gameManager = GameManager.instance
        if (!gameManager.isGameWorld(user.bukkitPlayer.world)) {
            user.bukkitPlayer.teleport(gameManager.activeMap.spawns[Team.Spectator]?.random() ?: return)
        }
    }

    override fun onDeath(deathContext: PlayerDeathContext) {}

    override fun onInventoryReset() {}

    override fun handleMapChanged(map: ActiveMap) {}
}