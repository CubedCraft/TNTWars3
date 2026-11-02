package com.jeroenvdg.tntwars.services.playerSettings

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.IService
import com.jeroenvdg.tntwars.services.ServiceSingleton

interface IPlayerSettingsService : IService {
    companion object : ServiceSingleton<IPlayerSettingsService>(IPlayerSettingsService::class.java)

    suspend fun loadSettings(user: TNTWarsPlayer) : Result<PlayerSettings>
    suspend fun saveSettings(user: TNTWarsPlayer, settings: PlayerSettings) : Result<Unit>
}

class PlayerSettings {
    var offhandSelector: Boolean = false
    var rotateTools: Boolean = false
    var rotateFence: Boolean = false
    var friendlyTNTPushing: Boolean = true
    var dispenserAssistLevel: DispenserPlaceAssistLevel = DispenserPlaceAssistLevel.Full
}

enum class DispenserPlaceAssistLevel {
    None,
    ShiftOnly,
    Full
}