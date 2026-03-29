package com.jeroenvdg.minigame_utilities.manager

abstract class Manageable(val id: String, var name: String) {
    var enabled = false
        get() = field && isReady()
        set(value) {
            field = value && isReady()
        }

    abstract fun isReady(): Boolean

    fun statusColorCode() = when {
        enabled -> "&a"
        !isReady() -> "&c"
        else -> "&7"
    }
}