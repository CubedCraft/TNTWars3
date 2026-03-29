package com.jeroenvdg.tntwars.services.achievements

import com.jeroenvdg.tntwars.player.TNTWarsPlayer

class SimpleAchievementService : IAchievementsService {

    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun getEnabledAchievements(): Result<Collection<AchievementData>> {
        var index = 0
        fun nextId(): Int = (index + 5) * 5 + 125
        fun nextIndex(): Int = index++
        return Result.success(arrayListOf(
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.WINS, 1,
                title = "Gain a win",
                description = "Win 1 match",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.WINS, 2,
                title = "Gain 2 wins",
                description = "Win 2 matches",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.WINS, 5,
                title = "Gain 5 wins",
                description = "Win 5 matches",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.KILLS, 1,
                title = "Gain a kill",
                description = "Gain a kill",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.KILLS, 5,
                title = "Gain 5 kills",
                description = "Gain 5 kills",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.KILLS, 10,
                title = "Gain 10 kills",
                description = "Gain 10 kills",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.TEAM_BALANCER, 1,
                title = "Team balancer I",
                description = "Balance the team once",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.TEAM_BALANCER, 2,
                title = "Team balancer II",
                description = "Balance the team twice",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.TEAM_BALANCER, 3,
                title = "Team balancer III",
                description = "Balance the team thrice",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.KILLSTREAK, 2,
                title = "Killstreak I",
                description = "Big kill streak",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.KILLSTREAK, 4,
                title = "Killstreak II",
                description = "Big kill streak",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.KILLSTREAK, 6,
                title = "Killstreak III",
                description = "Big kill streak",
            ),
            AchievementData(
                nextId(), nextIndex(),
                TNTWarsAchievementType.FLAWLESS, 1,
                title = "Flawless",
                description = "Win without anyone dying in your team",
            ),
        ))
    }

    override suspend fun getCompletedAchievements(user: TNTWarsPlayer): Result<Collection<CompletedAchievement>> {
        return Result.success(emptyList())
    }

    override suspend fun completeAchievement(user: TNTWarsPlayer, achievement: AchievementData): Result<CompletedAchievement> {
        if (user.achievements[achievement.index] != null) return Result.failure(Exception("User already has achievement"))
        return Result.success(CompletedAchievement(achievement.id, System.currentTimeMillis()))
    }
}