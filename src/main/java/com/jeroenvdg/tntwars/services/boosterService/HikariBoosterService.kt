package com.jeroenvdg.tntwars.services.boosterService

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.runAsync
import com.zaxxer.hikari.HikariDataSource

class HikariBoosterService(private val hikari: HikariDataSource, private val serverName: String) : IBoosterService {

    private val ACTIVE_BOOSTERS_TABLE = "tntwars_active_boosters"
    private val BOOSTERS_TABLE = "player_boosters"

    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun getActiveBoosters(): Result<List<ActiveBooster>> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT booster_id, activated FROM $ACTIVE_BOOSTERS_TABLE")
            val results = statement.executeQuery()

            val boosters = ArrayList<ActiveBooster>(results.fetchSize)
            while (results.next()) {
                val boosterId = results.getInt("booster_id")
                val activatedAt = results.getLong("activated")
                boosters.add(ActiveBooster(boosterId, activatedAt, true))
            }

            return@runAsync Result.success(boosters)
        }
    }

    override suspend fun getBoostersForPlayer(user: TNTWarsPlayer): Result<List<Booster>> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT id FROM $BOOSTERS_TABLE WHERE user_id = ? AND server = ?")
            statement.setInt(1, user.identifier.intId)
            statement.setString(2, serverName)

            val result = statement.executeQuery()
            val boosters = ArrayList<Booster>(result.fetchSize)
            while (result.next()) {
                val boosterId = result.getInt(1)
                boosters.add(Booster(boosterId, user.identifier))
            }

            return@runAsync Result.success(boosters)
        }
    }

    override suspend fun activateBooster(activator: TNTWarsPlayer, booster: Booster): Result<ActiveBooster> = runAsync {
        val activatedAt = System.currentTimeMillis()
        hikari.connection.use { connection ->
            consumeBooster(connection, booster.id, activator.identifier.intId, serverName) {
                connection.prepareStatement(
                    "INSERT INTO $ACTIVE_BOOSTERS_TABLE (player_id, booster_id, activated) VALUES (?,?,?)"
                ).use { statement ->
                    statement.setInt(1, activator.identifier.intId)
                    statement.setInt(2, booster.id)
                    statement.setLong(3, activatedAt)
                    statement.executeUpdate()
                }
            }
        }

        Result.success(ActiveBooster(booster.id, activatedAt, true))
    }

    override suspend fun removeActiveBooster(activeBooster: ActiveBooster): Result<Unit> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("DELETE FROM $ACTIVE_BOOSTERS_TABLE WHERE booster_id = ?")
            statement.setInt(1, activeBooster.id)
            statement.execute()
            return@runAsync Result.success(Unit)
        }
    }
}
