package com.jeroenvdg.tntwars.services.vanishService

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.IService
import com.jeroenvdg.tntwars.services.ServiceSingleton

interface IPlayerVanishService : IService {
    companion object : ServiceSingleton<IPlayerVanishService>(IPlayerVanishService::class.java)

    fun isPlayerVanish(user: TNTWarsPlayer): Boolean
}