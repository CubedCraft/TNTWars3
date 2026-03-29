package com.jeroenvdg.tntwars.player

import com.jeroenvdg.tntwars.player.behaviours.*

class PlayerBehaviourCollection(user: TNTWarsPlayer) {

    val behaviours = HashMap<Class<*>, PlayerBehaviour>()

    init {
        registerBehaviour(PlayerDamageBehaviour(user))
        registerBehaviour(PlayerTeamBorderBehaviour(user))
        registerBehaviour(PlayerInfiniteBucketBehaviour(user))
        registerBehaviour(PlayerOffHandSelectorBehaviour(user))
        registerBehaviour(PlayerRotateFenceBehaviour(user))
        registerBehaviour(PlayerDirectionalPlaceAssistBehaviour(user))
    }

    fun registerBehaviour(behaviour: PlayerBehaviour) {
        behaviours[behaviour.javaClass] = behaviour
    }

    fun <T : PlayerBehaviour> getBehaviour(type: Class<T>): T? {
        return behaviours[type] as? T
    }

    fun <T : PlayerBehaviour> activateBehaviour(type: Class<T>) {
        getBehaviour(type)?.activate()
    }

    fun <T : PlayerBehaviour> deactivateBehaviour(type: Class<T>) {
        getBehaviour(type)?.deactivate()
    }

}