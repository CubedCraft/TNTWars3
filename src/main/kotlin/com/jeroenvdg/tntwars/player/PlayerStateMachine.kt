package com.jeroenvdg.tntwars.player

import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.player.behaviours.PlayerDamageBehaviour
import com.jeroenvdg.tntwars.player.states.PlayerSpectatorState
import com.jeroenvdg.tntwars.player.states.PlayerVanishState
import com.jeroenvdg.tntwars.player.states.playerGameStates.PlayerGameStateMachine
import com.jeroenvdg.minigame_utilities.statemachine.StateMachine

class PlayerStateMachine(val user: TNTWarsPlayer) : StateMachine() {

    override val logChanges = true

    init {
        addState(PlayerSpectatorState(user))
        addState(PlayerVanishState(user))
        addState(PlayerGameStateMachine(user))
    }

    override fun onActivate() {
        user.bukkitPlayer.teleport(GameManager.instance.activeMap.spawns[Team.Spectator]!!.random())
        if (user.isVanishMode) {
            gotoState(PlayerVanishState::class.java)
        } else {
            gotoState(PlayerSpectatorState::class.java)
        }
        user.behaviourCollection.activateBehaviour(PlayerDamageBehaviour::class.java)
    }

    override fun onDeactivate() {
        user.behaviourCollection.deactivateBehaviour(PlayerDamageBehaviour::class.java)
    }

    override fun onNoState() {
    }
}