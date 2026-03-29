package com.jeroenvdg.tntwars.game

enum class TeamSelectMode(val isJoinable: Boolean) {
    Queue(true),
    Join(true),
    None(false),
    TempDisable(false)
}