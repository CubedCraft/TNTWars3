package com.jeroenvdg.tntwars.services.vanishService

import com.jeroenvdg.tntwars.player.TNTWarsPlayer

class SimplePlayerVanishService : IPlayerVanishService {
    override fun init() {
    }

    override fun dispose() {
    }

    override fun isPlayerVanish(user: TNTWarsPlayer): Boolean {
        return false
    }
}