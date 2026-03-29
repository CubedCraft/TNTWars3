package com.jeroenvdg.tntwars.player.gameContexts

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.interfaces.ItemSelector
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.behaviours.PlayerDirectionalPlaceAssistBehaviour
import com.jeroenvdg.tntwars.player.behaviours.PlayerInfiniteBucketBehaviour
import com.jeroenvdg.tntwars.player.behaviours.PlayerOffHandSelectorBehaviour
import com.jeroenvdg.tntwars.player.behaviours.PlayerRotateFenceBehaviour
import com.jeroenvdg.tntwars.player.states.playerGameStates.PlayerGamePlayingState
import com.jeroenvdg.tntwars.player.states.playerGameStates.PlayerGameRespawningState
import com.jeroenvdg.tntwars.player.states.playerGameStates.PlayerGameStateMachine
import org.bukkit.Bukkit

class ClassicGameContext(private val user: TNTWarsPlayer, private val stateMachine: PlayerGameStateMachine) : IPlayerGameContext {
    val player = user.bukkitPlayer

    private val infinteBucketBehaviour = user.behaviourCollection.getBehaviour(PlayerInfiniteBucketBehaviour::class.java)!!
    private val dispenserDirectionalPlaceAssistBehaviour = user.behaviourCollection.getBehaviour(PlayerDirectionalPlaceAssistBehaviour::class.java)!!
    private val rotateFenceBehaviour = user.behaviourCollection.getBehaviour(PlayerRotateFenceBehaviour::class.java)!!
    private val offHandSelectorBehaviour = user.behaviourCollection.getBehaviour(PlayerOffHandSelectorBehaviour::class.java)!!

    override fun onActivate() {
        stateMachine.gotoState(PlayerGamePlayingState::class.java)
        enableBehaviours()
    }

    override fun onDeactivate() {
        stateMachine.gotoNoState()
        disableBehaviours()
    }

    override fun onDeath(deathContext: PlayerDeathContext) {
        disableBehaviours()
        stateMachine.gotoState(PlayerGameRespawningState::class.java)
        Bukkit.broadcast(deathContext.message)
        EventBus.onPlayerDeath.invoke(deathContext)
    }

    override fun onRespawn() {
        enableBehaviours()
        stateMachine.gotoState(PlayerGamePlayingState::class.java)
    }

    override fun onInventoryReset() {
        if (!user.settings.offhandSelector) {
            player.inventory.setItem(8, ItemSelector.item)
        }
    }

    private fun enableBehaviours() {
        infinteBucketBehaviour.activate()
        dispenserDirectionalPlaceAssistBehaviour.activate()
        if (user.settings.offhandSelector) { offHandSelectorBehaviour.activate() }
        if (user.settings.rotateFence) { rotateFenceBehaviour.activate() }

    }

    private fun disableBehaviours() {
        infinteBucketBehaviour.deactivate()
        dispenserDirectionalPlaceAssistBehaviour.deactivate()
        offHandSelectorBehaviour.deactivate()
        rotateFenceBehaviour.deactivate()
    }

    class Provider : IPlayerGameContext.IProvider {
        override fun getPlayerGameContext(user: TNTWarsPlayer, stateMachine: PlayerGameStateMachine): IPlayerGameContext {
            return ClassicGameContext(user, stateMachine)
        }
    }
}
