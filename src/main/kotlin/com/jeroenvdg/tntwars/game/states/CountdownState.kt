package com.jeroenvdg.tntwars.game.states

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.game.TeamSelectMode
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.gameContexts.SpectatorPlayerContext
import com.jeroenvdg.minigame_utilities.Scheduler
import com.jeroenvdg.minigame_utilities.Soundial
import com.jeroenvdg.minigame_utilities.Textial
import org.bukkit.Bukkit

class CountdownState : BaseGameState() {

    companion object {
        val config get() = TNTWars.instance.config
    }

    override val playerContextProvider = SpectatorPlayerContext.Provider()
    override val teamSelectMode get() = TeamSelectMode.Queue

    override fun onActivate() {
        if(!config.gameConfig.tournamentMode.enabled) {
            startCoroutine { countdownRoutine() }
        }

        EventBus.onPlayerLeft += ::handlePlayerLeft
        EventBus.onPlayerTeamChanged += ::handlePlayerTeamChanged

        PlayerManager.instance.updatePlayerVisibility()
    }

    override fun onDeactivate() {
        EventBus.onPlayerLeft -= ::handlePlayerLeft
        EventBus.onPlayerTeamChanged -= ::handlePlayerTeamChanged
    }

    private suspend fun countdownRoutine() {
        for (i in TNTWars.instance.config.gameConfig.countdownTime downTo 0) {
            Scheduler.delay(20)

            when (i) {
                30, 20, 15, 10, 5, 4, 3, 2 -> {
                    Bukkit.broadcast(Textial.bc.format("Game starting in &p$i&r seconds"))
                    Soundial.playAll(Soundial.Countdown)
                }
                1 -> {
                    Bukkit.broadcast(Textial.bc.format("Game starting in &p$i&r seconds"))
                    Soundial.playAll(Soundial.Countdown)
                }
            }

            for (user in PlayerManager.instance.players) {
                user.bukkitPlayer.level = i
            }
        }

        gameManager.startMatch()
    }

    private fun handlePlayerLeft(user: TNTWarsPlayer) {
        if (Team.Queue.usersInTeam().size <= 1) {
            stateMachine.gotoState(WaitingState::class.java)
        }
    }

    private fun handlePlayerTeamChanged(user: TNTWarsPlayer, team: Team) {
        if (Team.Queue.usersInTeam().size <= 1) {
            stateMachine.gotoState(WaitingState::class.java)
        }
    }

}