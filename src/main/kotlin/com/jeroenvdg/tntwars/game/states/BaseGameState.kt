package com.jeroenvdg.tntwars.game.states

import com.jeroenvdg.minigame_utilities.statemachine.State
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.TeamSelectMode
import com.jeroenvdg.tntwars.player.gameContexts.IPlayerGameContext

abstract class BaseGameState : State() {
    abstract val playerContextProvider: IPlayerGameContext.IProvider
    abstract val teamSelectMode: TeamSelectMode
    val gameManager get()= TNTWars.instance.gameManager
}