package com.jeroenvdg.minigame_utilities

import java.util.ArrayList


class Event0 : BaseEvent<() -> Unit>() {
    fun invoke() {
        isEvoking = true

        for (listener in listeners) {
            listener()
        }

        postInvoke()
        isEvoking = false
    }
}


class Event1<T> : BaseEvent<(T) -> Unit>() {
    fun invoke(a: T) {
        isEvoking = true

        for (listener in listeners) {
            listener(a)
        }

        postInvoke()
        isEvoking = false
    }
}


class Event2<T1, T2> : BaseEvent<(T1, T2) -> Unit>() {
    fun invoke(a: T1, b: T2) {
        isEvoking = true

        for (listener in listeners) {
            listener(a, b)
        }

        postInvoke()
        isEvoking = false
    }
}



abstract class BaseEvent<Action> {
    protected var isEvoking = false
    protected val listeners = ArrayList<Action>(2)
    private val newListeners = ArrayList<Action>(1)
    private var newListenersSet = false


    operator fun plusAssign(action: Action) {
        if (isEvoking) {
            mimicListeners()
            newListeners.add(action)
        } else {
            listeners.add(action)
        }
    }


    operator fun minusAssign(action: Action) {
        if (isEvoking) {
            mimicListeners()
            newListeners.remove(action)
        } else {
            listeners.remove(action)
        }
    }


    fun clear() {
        if (isEvoking) {
            mimicListeners()
            newListeners.clear()
        } else {
            listeners.clear()
        }
    }


    protected fun postInvoke() {
        if (newListenersSet) {
            listeners.clear()
            listeners.addAll(newListeners)
            newListeners.clear()
            newListenersSet = false
        }
    }


    private fun mimicListeners() {
        if (newListenersSet) return
        newListenersSet = true
        newListeners.addAll(listeners)
    }
}
