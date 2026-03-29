package com.jeroenvdg.tntwars.player.states.playerGameStates

import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.statemachine.State

abstract class BasePlayerGameState(val user: TNTWarsPlayer) : State() {

    open val flyEnabled = false

    val player = user.bukkitPlayer
    val gameStateMachine: PlayerGameStateMachine get() = super.stateMachine as PlayerGameStateMachine

    override fun onActivate() {
        gameStateMachine.setFlyEnabled(flyEnabled)
    }

    override fun onDeactivate() {
    }

    abstract fun onDeath(deathContext: PlayerDeathContext)

}