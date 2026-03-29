package com.jeroenvdg.tntwars.player

import com.jeroenvdg.minigame_utilities.launchCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

abstract class PlayerBehaviour(val user: TNTWarsPlayer) {

    val player get() = user.bukkitPlayer
    var isActive = false; private set

    val coroutines = mutableListOf<Job>()

    fun activate() {
        if (isActive) return
        onActivate()
        isActive = true
    }

    fun deactivate() {
        if (!isActive) return
        for (coroutine in coroutines) {
            if (!coroutine.isActive) continue
            coroutine.cancel()
        }
        coroutines.clear()
        onDeactivate()
        isActive = false
    }

    protected abstract fun onActivate()
    protected abstract fun onDeactivate()

    protected fun startCoroutine(coroutine: suspend CoroutineScope.() -> Unit) {
        coroutines.add(launchCoroutine { coroutine(it) })
    }

}

