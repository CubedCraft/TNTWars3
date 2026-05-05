package com.jeroenvdg.tntwars.services.griefDetectionService

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.vanishService.IPlayerVanishService

class GriefDetectionService : IPlayerVanishService {
    override fun init() {
    }

    override fun dispose() {
    }

    override fun isPlayerVanish(user: TNTWarsPlayer): Boolean {
        return false
    }
}