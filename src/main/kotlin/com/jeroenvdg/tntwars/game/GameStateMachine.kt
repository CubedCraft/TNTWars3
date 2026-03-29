package com.jeroenvdg.tntwars.game

import com.jeroenvdg.minigame_utilities.statemachine.State
import com.jeroenvdg.minigame_utilities.statemachine.StateMachine
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.game.states.*

class GameStateMachine(private val gameManager: GameManager) : StateMachine() {

    override val logChanges = true

    init {
        addState(WaitingState())
        addState(CountdownState())
        addState(MatchState())
        addState(MatchEndedState())
    }

    override fun onActivate() {
        onStateChanged += ::handleStateChanged
        gotoState(WaitingState::class.java)
    }

    override fun onDeactivate() {
        onStateChanged -= ::handleStateChanged
    }

    override fun onNoState() {
    }

    private fun handleStateChanged(old: State?, current: State?) {
        if (current !is BaseGameState) return
        gameManager.setTeamSelectMode(current.teamSelectMode)
        EventBus.onPlayerGameContextProviderChanged.invoke(current.playerContextProvider)
    }
}