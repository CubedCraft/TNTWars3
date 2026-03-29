package com.jeroenvdg.tntwars.services.playerStats

import com.jeroenvdg.minigame_utilities.runAsync
import com.jeroenvdg.tntwars.managers.PlayerRoundSummary
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.userIdentifier.UserIdentifier
import com.zaxxer.hikari.HikariDataSource
import java.sql.Statement

class HikariPersistentPlayerStatsService(private val hikari: HikariDataSource) : IPlayerStatsService {

    val PLAYER_STATS_TABLE = "tntwars_player_stats"
    val GAMES_TABLE = "tntwars_games"
    val PLAYER_GAMES_TABLE = "tntwars_player_games"

    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun load(user: TNTWarsPlayer): Result<PlayerStats> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT * FROM $PLAYER_STATS_TABLE WHERE player_id = ?")
            statement.setInt(1, user.identifier.intId)
            val result = statement.executeQuery()

            val stats = PlayerStats()
            stats.lastJoinedAt = System.currentTimeMillis()

            if (result.next()) {
                stats.wins = result.getInt("wins")
                stats.kills = result.getInt("kills")
                stats.deaths = result.getInt("deaths")
                stats.coins = result.getInt("coins")
                stats.score = result.getInt("exp")
                stats.mvpCount = result.getInt("mvp_count")
                stats.teamBalances = result.getInt("balancer")
                stats.lastJoinedAt = result.getLong("last_join")
                stats.playTimeBeforeJoin = result.getLong("playtime")
            } else {
                val query = connection.prepareStatement("INSERT INTO $PLAYER_STATS_TABLE (player_id, first_join, last_join) VALUES (?, ?, ?)")
                query.setInt(1, user.identifier.intId)
                query.setLong(2, System.currentTimeMillis())
                query.setLong(3, System.currentTimeMillis())
                query.executeUpdate()
            }
            return@runAsync Result.success(stats)
        }
    }

    override suspend fun save(user: TNTWarsPlayer, stats: PlayerStats): Result<Unit> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("UPDATE $PLAYER_STATS_TABLE SET wins = ?, kills = ?, deaths = ?, coins = ?, exp = ?, balancer = ?, last_join = ?, playtime = ? where player_id = ?")
            statement.setInt(1, stats.wins)
            statement.setInt(2, stats.kills)
            statement.setInt(3, stats.deaths)
            statement.setInt(4, stats.coins)
            statement.setInt(5, stats.score)
            statement.setInt(6, stats.teamBalances)
            statement.setLong(7, stats.lastJoinedAt)
            statement.setLong(8, stats.playTimeBeforeJoin + (System.currentTimeMillis() - stats.lastJoinedAt))
            statement.setInt(9, user.identifier.intId)
            statement.executeUpdate()
            return@runAsync Result.success(Unit)
        }
    }

    override suspend fun saveBulk(pairs: List<Pair<TNTWarsPlayer, PlayerStats>>): Result<Unit> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("UPDATE $PLAYER_STATS_TABLE SET wins = ?, kills = ?, deaths = ?, coins = ?, exp = ?, balancer = ?, last_join = ?, playtime = ?, mvp_count = ?" +
                    " " +
                    "where player_id = ?")
            for ((user, stats) in pairs) {
                statement.setInt(1, stats.wins)
                statement.setInt(2, stats.kills)
                statement.setInt(3, stats.deaths)
                statement.setInt(4, stats.coins)
                statement.setInt(5, stats.score)
                statement.setInt(6, stats.teamBalances)
                statement.setLong(7, stats.lastJoinedAt)
                statement.setLong(8, stats.playTimeBeforeJoin + (System.currentTimeMillis() - stats.lastJoinedAt))
                statement.setInt(9, stats.mvpCount)
                statement.setInt(10, user.identifier.intId)
                statement.addBatch()
            }
            statement.executeBatch()
            return@runAsync Result.success(Unit)
        }
    }

    override suspend fun saveRoundSummary(roundData: RoundData, users: List<Pair<UserIdentifier, PlayerRoundSummary>>): Result<Unit> = runAsync {
        hikari.connection.use { connection ->
            val insertGameStatement = connection.prepareStatement("INSERT INTO $GAMES_TABLE (map, gamemode, winner, mvp, start_date, finish_date) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
            insertGameStatement.setString(1, roundData.mapName)
            insertGameStatement.setString(2, roundData.gamemodeName)
            insertGameStatement.setString(3, roundData.winningTeamName)
            insertGameStatement.setInt(4, roundData.mvp?.intId ?: -1)
            insertGameStatement.setLong(5, roundData.startedTime)
            insertGameStatement.setLong(6, roundData.endedTime)
            insertGameStatement.executeUpdate()
            val result = insertGameStatement.generatedKeys

            if (!result.next()) throw Exception("Could not save the game summary")
            val gameId = result.getInt(1)

            val insertPlayersStatement = connection.prepareStatement("INSERT INTO $PLAYER_GAMES_TABLE (game_id, player_id, kills, deaths, team) VALUES (?,?,?,?,?)")
            for ((userIdentifier, roundStats) in users) {
                insertPlayersStatement.setInt(1, gameId)
                insertPlayersStatement.setInt(2, userIdentifier.intId)
                insertPlayersStatement.setInt(3, roundStats.kills)
                insertPlayersStatement.setInt(4, roundStats.deaths)
                insertPlayersStatement.setString(5, roundStats.team.name)
                insertPlayersStatement.addBatch()
            }

            insertPlayersStatement.executeBatch()
            return@runAsync Result.success(Unit)
        }
    }
}