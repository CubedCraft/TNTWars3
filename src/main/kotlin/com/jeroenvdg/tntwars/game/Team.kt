package com.jeroenvdg.tntwars.game

import com.jeroenvdg.tntwars.TNTWars

enum class Team(val primaryColor: Char, val isSpectatorTeam: Boolean) {
    Spectator('f', true),
    Queue('7', true),
    Red('c', false),
    Blue('9', false);

    val isGameTeam get() = !isSpectatorTeam
    fun usersInTeam() = TNTWars.instance.playerManager.findUsersInTeam(this)
}

