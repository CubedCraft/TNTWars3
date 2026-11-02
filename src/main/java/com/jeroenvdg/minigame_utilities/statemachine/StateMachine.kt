package com.jeroenvdg.minigame_utilities.statemachine

import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.Event2

abstract class StateMachine : State() {
    val states = ArrayList<State>(4)
    private val queue = ArrayList<State>()
    
    var activeState: State? = null; private set
    val onStateChanged = Event2<State?, State?>();

    open val logPrefix get() = "[SM]"
    open val logChanges = false
    

    fun addState(state : State) {
        state.stateMachine = this
        states.add(state)
    }


    fun <T : State> addToQueue(type: Class<T>) {
        addToQueue(states.first { it.javaClass == type })
    }


    fun <T : State> gotoState(type: Class<T>) {
        gotoState(states.first { it.javaClass == type })
    }


    fun addToQueue(state: State) {
        if (state.stateMachine != this) throw Exception("State-machines did not match!")
        queue.add(state)
    }


    fun gotoState(state: State) {
        if (state.stateMachine != this) throw Exception("State-machines did not match!")
        clearQueue()
        addToQueue(state)
        continueQueue()
    }


    fun gotoNoState(skipNoState: Boolean = false) {
        clearQueue()
        continueQueue(skipNoState)
    }


    fun continueQueue(skipNoState: Boolean = false) {
        val currentState = activeState

        if (activeState != null) {
            activeState!!.deactivate()
            log("Deactivated state ${activeState!!.javaClass.name}")
        }

        if (queue.size == 0) {
            if (!skipNoState) {
                log("Calling noState")
                onNoState()
                onStateChanged.invoke(currentState, null)
            } else {
                activeState = null
            }
            return
        }

        activeState = queue.removeFirst()
        if (activeState == null) return continueQueue() // If state should ever be null (it shouldn't), pick the next state
        log("Activated state ${activeState!!.javaClass.name}")
        activeState!!.activate()
        onStateChanged.invoke(currentState, activeState)
    }


    fun clearQueue() {
        queue.clear()
    }


    override fun onLateDeactivate() {
        if (activeState != null) {
            log("Please call gotoNoState(true) to deactivate the current state in onDeactivate")
            gotoNoState(true)
        } else {
            clearQueue()
        }
    }


    protected abstract fun onNoState()
    protected fun log(msg: String) {
        if (logChanges) Debug.log("$logPrefix $msg")
    }
}
