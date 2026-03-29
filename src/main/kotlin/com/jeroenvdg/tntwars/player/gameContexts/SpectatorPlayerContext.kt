package com.jeroenvdg.tntwars.player.gameContexts

import com.jeroenvdg.tntwars.interfaces.MapSelector
import com.jeroenvdg.tntwars.interfaces.ProfileInterface
import com.jeroenvdg.tntwars.interfaces.SettingsInterface
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.states.PlayerSpectatorState.Companion.config
import com.jeroenvdg.tntwars.player.states.playerGameStates.PlayerGameSpectateState
import com.jeroenvdg.tntwars.player.states.playerGameStates.PlayerGameStateMachine

class SpectatorPlayerContext(private val stateMachine: PlayerGameStateMachine) : IPlayerGameContext {
    private val player = stateMachine.player

    override fun onActivate() {
        stateMachine.gotoState(PlayerGameSpectateState::class.java)
    }

    override fun onDeactivate() {
        stateMachine.gotoNoState()
    }

    override fun onDeath(deathContext: PlayerDeathContext) { }
    override fun onRespawn() { }
    override fun onInventoryReset() {
        if(!config.gameConfig.tournamentMode.enabled) {
            player.inventory.setItem(4, ProfileInterface.makeProfileItem(player))
            player.inventory.setItem(8, MapSelector.mapSelectorItem)
        }

        player.inventory.setItem(7, SettingsInterface.settingsItem)
    }

    class Provider : IPlayerGameContext.IProvider {
        override fun getPlayerGameContext(user: TNTWarsPlayer, stateMachine: PlayerGameStateMachine): IPlayerGameContext {
            return SpectatorPlayerContext(stateMachine)
        }

    }

}