package com.jeroenvdg.tntwars.services.playerSettings

import com.jeroenvdg.tntwars.player.TNTWarsPlayer

class SimplePlayerSettingsService : IPlayerSettingsService {

    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun loadSettings(user: TNTWarsPlayer): Result<PlayerSettings> {
        return Result.success(PlayerSettings())
    }

    override suspend fun saveSettings(user: TNTWarsPlayer, settings: PlayerSettings): Result<Unit> {
        return Result.success(Unit)
    }
}