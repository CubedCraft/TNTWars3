package com.jeroenvdg.tntwars.game

abstract class GameMode(val id: String, val displayName: String) {
    abstract fun init(manager: GameManager)
}
