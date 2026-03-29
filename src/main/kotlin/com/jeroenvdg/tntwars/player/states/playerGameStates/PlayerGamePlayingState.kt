package com.jeroenvdg.tntwars.player.states.playerGameStates

import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.behaviours.PlayerDamageBehaviour
import com.jeroenvdg.tntwars.player.behaviours.PlayerTeamBorderBehaviour
import com.jeroenvdg.minigame_utilities.Scheduler
import org.bukkit.GameMode
import org.bukkit.util.Vector

class PlayerGamePlayingState(user: TNTWarsPlayer) : BasePlayerGameState(user) {

    private val damageBehaviour = user.behaviourCollection.getBehaviour(PlayerDamageBehaviour::class.java)!!
    private val teamBorderBehaviour = user.behaviourCollection.getBehaviour(PlayerTeamBorderBehaviour::class.java)!!

    override fun onActivate() {
        super.onActivate()
        player.velocity = Vector(0,0,0)
        player.fallDistance = 0f
        player.teleport(GameManager.instance.activeMap.spawns[user.team]!!.random())
        player.gameMode = GameMode.SURVIVAL

        damageBehaviour.canTakeDamage = false
        startCoroutine {
            Scheduler.delay(5*20)
            damageBehaviour.canTakeDamage = true
        }

        gameStateMachine.applyArmor()
        teamBorderBehaviour.activate()
        user.heal()
    }

    override fun onDeactivate() {
        super.onDeactivate()
        damageBehaviour.setToDefaults()
        teamBorderBehaviour.deactivate()
    }

    override fun onDeath(deathContext: PlayerDeathContext) {
        gameStateMachine.playerContext.onDeath(deathContext)
    }
}