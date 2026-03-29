package com.jeroenvdg.tntwars.services.achievements

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.parseEnum
import com.jeroenvdg.minigame_utilities.runAsync
import com.zaxxer.hikari.HikariDataSource

class HikariPersistentAchievementService(private val hikari: HikariDataSource, private val serverId: Int) : IAchievementsService {

    private val ACHIEVEMENTS_TABLE = "achievements"
    private val PLAYER_ACHIEVEMENTS_TABLE = "player_achievements"

    override fun init() { }
    override fun dispose() { }

    override suspend fun getEnabledAchievements(): Result<Collection<AchievementData>> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT * FROM $ACHIEVEMENTS_TABLE WHERE server_id = ? AND enabled = 1")
            statement.setInt(1, serverId)
            val results = statement.executeQuery()
            val achievements = ArrayList<AchievementData>(results.fetchSize)
            var index = 0
            while (results.next()) {
                achievements.add(
                    AchievementData(
                    results.getInt("id"),
                    index++,
                    type = parseEnum<TNTWarsAchievementType>(results.getString("type"))!!,
                    requiredValue = results.getInt("score"),
                    title = results.getString("title"),
                    description = results.getString("description"),
                )
                )
            }
            return@runAsync Result.success(achievements)
        }
    }

    override suspend fun getCompletedAchievements(user: TNTWarsPlayer): Result<Collection<CompletedAchievement>> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT achievement_id, received FROM $PLAYER_ACHIEVEMENTS_TABLE WHERE user_id = ? AND server_id = ?")
            statement.setInt(1, user.identifier.intId)
            statement.setInt(2, serverId)

            val result = statement.executeQuery()
            val achievements = ArrayList<CompletedAchievement>(result.fetchSize)
            while (result.next()) {
                achievements.add(CompletedAchievement(result.getInt("achievement_id"), result.getLong("received")))
            }
            return@runAsync Result.success(achievements)
        }
    }

    override suspend fun completeAchievement(user: TNTWarsPlayer, achievement: AchievementData): Result<CompletedAchievement> = runAsync {
        hikari.connection.use { connection ->
            val statement = connection.prepareStatement("INSERT INTO $PLAYER_ACHIEVEMENTS_TABLE (achievement_id, user_id, server_id, received) VALUES (?, ?, ?, ?)")
            statement.setInt(1, achievement.id)
            statement.setInt(2, user.identifier.intId)
            statement.setInt(3, serverId)
            statement.setLong(4, System.currentTimeMillis())
            statement.executeUpdate()
            return@runAsync Result.success(CompletedAchievement(achievement.id, System.currentTimeMillis()))
        }
    }
}