package com.jeroenvdg.minigame_utilities.statemachine

import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.launchCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

abstract class State {
    lateinit var stateMachine: StateMachine
    var isActive = false; private set

    private val coroutines = ArrayList<Job>()

    fun activate() {
        if (isActive) return
        isActive = true
        onPreActivate()
        onActivate()
    }

    fun deactivate() {
        if (!isActive) return
        onPreDeactivate()
        onDeactivate()
        for (coroutine in coroutines) {
            coroutine.cancel()
        }
        coroutines.clear()
        onLateDeactivate()

        isActive = false
    }

    protected fun startCoroutine(action: suspend (scope: CoroutineScope) -> Unit) {
        if (!isActive) {
            Debug.error("Tried to start coroutine while state is not active! (${this.javaClass.name})")
            return
        }
        coroutines.add(launchCoroutine(action))
    }

    protected open fun onPreActivate() {}
    protected abstract fun onActivate()
    protected open fun onPreDeactivate() {}
    protected abstract fun onDeactivate()
    protected open fun onLateDeactivate() {}
}
