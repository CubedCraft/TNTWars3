package com.jeroenvdg.tntwars.player.gameContexts

import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.states.playerGameStates.PlayerGameStateMachine

interface IPlayerGameContext {
    fun onActivate()
    fun onDeactivate()
    fun onDeath(deathContext: PlayerDeathContext)
    fun onRespawn()
    fun onInventoryReset()

    interface IProvider {
        fun getPlayerGameContext(user: TNTWarsPlayer, stateMachine: PlayerGameStateMachine): IPlayerGameContext
    }
}