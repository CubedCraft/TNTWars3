package com.jeroenvdg.minigame_utilities.statemachine

import com.jeroenvdg.minigame_utilities.Debug

open class BehaviourManager : State() {
    val behaviours = ArrayList<Behaviour>(4)
    val activeBehaviours = ArrayList<Behaviour>(4)

    open val logPrefix get() = "[BM]"
    open val logChanges = false


    fun addBehaviour(behaviour: Behaviour) {
        behaviour.manager = this
        behaviours.add(behaviour)
    }


    inline fun <reified T : Behaviour> getBehaviour() : T {
        return behaviours.find { it is T }!! as T
    }


    inline fun <reified T : Behaviour> activateBehaviour() {
        val behaviour = getBehaviour<T>()
        activateBehaviour(behaviour)
    }


    inline fun <reified T : Behaviour> deactivateBehaviour() {
        val behaviour = getBehaviour<T>()
        deactivateBehaviour(behaviour)
    }


    fun activateBehaviour(behaviour: Behaviour) {
        if (behaviour.isActive) return
        behaviour.activate()
        activeBehaviours.add(behaviour)
    }


    fun deactivateBehaviour(behaviour: Behaviour) {
        if (!behaviour.isActive) return
        behaviour.deactivate()
        activeBehaviours.remove(behaviour)
    }


    fun deactivateAllBehaviours() {
        for (activeBehaviour in activeBehaviours) {
            activeBehaviour.deactivate()
        }
        activeBehaviours.clear()
    }

    override fun onActivate() {
    }

    override fun onDeactivate() {
        deactivateAllBehaviours()
    }


    override fun onLateDeactivate() {
        if (activeBehaviours.size > 0) {
            Debug.warn("-----------------------------------------")
            Debug.warn("Behaviour manager has been deactivated but not all states")
            Debug.warn("-----------------------------------------")
        }
    }


    protected fun log(msg: String) {
        if (logChanges) Debug.log("$logPrefix $msg")
    }
}