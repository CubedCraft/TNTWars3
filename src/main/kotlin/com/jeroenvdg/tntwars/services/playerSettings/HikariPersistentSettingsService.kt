package com.jeroenvdg.tntwars.services.playerSettings

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.runAsync
import com.zaxxer.hikari.HikariDataSource

class HikariPersistentSettingsService(val hikari: HikariDataSource) : IPlayerSettingsService {

    private val SETTINGS_TABLE = "tntwars_player_settings"

    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun loadSettings(user: TNTWarsPlayer): Result<PlayerSettings> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT * FROM $SETTINGS_TABLE WHERE player_id = ?")
            statement.setInt(1, user.identifier.intId)

            val settings = PlayerSettings()
            val result = statement.executeQuery()
            if (result.next()) {
                settings.offhandSelector = result.getBoolean("offhand_selector")
                settings.rotateTools = result.getBoolean("rotate_tools")
                settings.rotateFence = result.getBoolean("rotate_fences")
                settings.friendlyTNTPushing = result.getBoolean("friendly_tnt_pushing")
                settings.dispenserAssistLevel = DispenserPlaceAssistLevel.entries[result.getInt("dispenser_place_assist")]
            } else {
                val query = connection.prepareStatement("INSERT INTO $SETTINGS_TABLE (player_id) VALUES (?)")
                query.setInt(1, user.identifier.intId)
                query.executeUpdate()
            }

            return@runAsync Result.success(settings)
        }
    }

    override suspend fun saveSettings(user: TNTWarsPlayer, settings: PlayerSettings): Result<Unit> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("UPDATE $SETTINGS_TABLE SET offhand_selector = ?, rotate_tools = ?, rotate_fences = ?, friendly_tnt_pushing = ?, dispenser_place_assist = ? WHERE player_id = ?")
            statement.setBoolean(1, settings.offhandSelector)
            statement.setBoolean(2, settings.rotateTools)
            statement.setBoolean(3, settings.rotateFence)
            statement.setBoolean(4, settings.friendlyTNTPushing)
            statement.setInt(5, settings.dispenserAssistLevel.ordinal)
            statement.setInt(6, user.identifier.intId)
            statement.executeUpdate()
            return@runAsync Result.success(Unit)
        }
    }
}