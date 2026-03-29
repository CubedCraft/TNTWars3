package com.jeroenvdg.tntwars.player.states

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.states.playerGameStates.PlayerGameStateMachine
import com.jeroenvdg.minigame_utilities.statemachine.StateMachine

abstract class BasePlayerState(val user: TNTWarsPlayer) : StateMachine() {

    open val flyEnabled = false

    val player get() = user.bukkitPlayer

    override fun onActivate() {
        setFlyEnabled(flyEnabled)
        user.onTeamChanged += ::handleTeamChanged
        user.onInventoryReset += ::onInventoryReset
        user.onPlayerDeath += ::onDeath

        EventBus.onUserVanishChanged += ::handleUserVanishChanged
        EventBus.onMapChanged += ::handleMapChanged
    }

    override fun onDeactivate() {
        user.onTeamChanged -= ::handleTeamChanged
        user.onInventoryReset -= ::onInventoryReset
        user.onPlayerDeath -= ::onDeath

        EventBus.onUserVanishChanged -= ::handleUserVanishChanged
        EventBus.onMapChanged -= ::handleMapChanged
    }

    override fun onNoState() {
    }

    protected abstract fun onDeath(deathContext: PlayerDeathContext)
    protected abstract fun onInventoryReset()

    protected open fun handleMapChanged(map: ActiveMap) {
        player.teleport(map.spawns[Team.Spectator]?.random() ?: return)
        user.team = Team.Spectator
        stateMachine.gotoState(PlayerSpectatorState::class.java)
    }

    private fun handleTeamChanged(old: Team, new: Team) {
        when {
            !old.isSpectatorTeam && new.isSpectatorTeam -> stateMachine.gotoState(PlayerSpectatorState::class.java)
            old.isSpectatorTeam && !new.isSpectatorTeam -> stateMachine.gotoState(PlayerGameStateMachine::class.java)
        }
    }

    private fun handleUserVanishChanged(user: TNTWarsPlayer) {
        if (user != this.user) return
        if (user.isVanishMode) {
            user.team = Team.Spectator
            stateMachine.gotoState(PlayerVanishState::class.java)
        } else {
            stateMachine.gotoState(PlayerSpectatorState::class.java)
        }
    }

    fun setFlyEnabled(enable: Boolean) {
        user.bukkitPlayer.allowFlight = enable
    }
}