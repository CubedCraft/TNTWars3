package com.jeroenvdg.tntwars.game.states

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.TNTSpawnEvent
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.TeamSelectMode
import com.jeroenvdg.tntwars.managers.PlayerStatsManager
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.gameContexts.SpectatorPlayerContext
import com.jeroenvdg.minigame_utilities.Scheduler
import org.bukkit.entity.TNTPrimed

class MatchEndedState : BaseGameState() {

    override val playerContextProvider = SpectatorPlayerContext.Provider()
    override val teamSelectMode get() = TeamSelectMode.None

    override fun onActivate() {
        startCoroutine { countdownRoutine() }

        for (entity in GameManager.instance.activeMap.managedWorld.world!!.entities.filterIsInstance<TNTPrimed>()) {
            entity.remove()
        }

        val mostValuablePlayer = PlayerStatsManager.instance.getMostValuablePlayer()
        if (mostValuablePlayer != null) {
            PlayerStatsManager.instance.applyRewards(mostValuablePlayer, TNTWars.instance.config.rewardConfig.mvpRewards)
        }

        PlayerManager.instance.updatePlayerVisibility()
        EventBus.onTNTSpawnEvent += ::handleTNTSpawned
    }

    override fun onDeactivate() {
        EventBus.onTNTSpawnEvent -= ::handleTNTSpawned
    }

    private suspend fun countdownRoutine() {
        Scheduler.delay(10*20)
        gameManager.loadMapFromSelector()
        stateMachine.gotoState(WaitingState::class.java)
    }

    private fun handleTNTSpawned(event: TNTSpawnEvent) {
        event.isCancelled = true
    }
}