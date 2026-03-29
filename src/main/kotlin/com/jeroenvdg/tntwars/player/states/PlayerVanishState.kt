package com.jeroenvdg.tntwars.player.states

import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.GameMode

class PlayerVanishState(user: TNTWarsPlayer) : BasePlayerState(user) {

    override val flyEnabled = true

    override fun onActivate() {
        player.clearActivePotionEffects()
        player.gameMode = GameMode.SPECTATOR
        super.onActivate()
    }

    override fun onDeactivate() {
        player.gameMode = GameMode.ADVENTURE
        super.onDeactivate()
    }

    override fun onDeath(deathContext: PlayerDeathContext) { }
    override fun onInventoryReset() { }

    override fun handleMapChanged(map: ActiveMap) {
        player.teleport(map.spawns[Team.Spectator]?.random() ?: return)
        stateMachine.gotoState(PlayerVanishState::class.java) // Reactivate in case of something breaking
    }
}