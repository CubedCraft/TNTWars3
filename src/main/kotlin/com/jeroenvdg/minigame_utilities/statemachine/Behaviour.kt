package com.jeroenvdg.minigame_utilities.statemachine

import com.jeroenvdg.minigame_utilities.Scheduler

abstract class Behaviour {
    lateinit var manager: BehaviourManager
    var isActive = false; private set

    protected open val useActiveUpdate = false
    protected open val tickInterval = 1L

    private var scheduleId = -1

    constructor()
    constructor(manager: BehaviourManager) {
        this.manager = manager
    }

    fun activate() {
        if (isActive) return
        isActive = true
        onPreActivate()
        onActivate()

        if (useActiveUpdate) {
            scheduleId = Scheduler.delayRepeating(1, tickInterval) {
                onUpdate(tickInterval / 20.0)
            }
        }
    }

    fun deactivate() {
        if (!isActive) return
        onPreDeactivate()
        onDeactivate()
        onLateDeactivate()
        if (useActiveUpdate) Scheduler.stop(scheduleId)
        isActive = false
    }

    protected open fun onPreActivate() {}
    protected abstract fun onActivate()
    protected open fun onUpdate(dt: Double) {}
    protected open fun onPreDeactivate() {}
    protected abstract fun onDeactivate()
    protected open fun onLateDeactivate() {}
}