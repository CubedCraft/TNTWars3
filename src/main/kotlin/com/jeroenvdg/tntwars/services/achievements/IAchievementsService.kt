package com.jeroenvdg.tntwars.services.achievements

import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.IService
import com.jeroenvdg.tntwars.services.ServiceSingleton

interface IAchievementsService : IService {

    companion object : ServiceSingleton<IAchievementsService>(IAchievementsService::class.java)

    suspend fun getEnabledAchievements(): Result<Collection<AchievementData>>
    suspend fun getCompletedAchievements(user: TNTWarsPlayer): Result<Collection<CompletedAchievement>>
    suspend fun completeAchievement(user: TNTWarsPlayer, achievement: AchievementData) : Result<CompletedAchievement>
}

enum class TNTWarsAchievementType {
    KILLS,
    WINS,
    TEAM_BALANCER,
    KILLSTREAK,
    MVP,
    FLAWLESS,
    DROWNER,
    SCORE,
    CLICKEGG
}

data class AchievementData(
    val id: Int,
    val index: Int,
    val type: TNTWarsAchievementType,
    val requiredValue: Int,
    val title: String,
    val description: String,
    val enabled: Boolean = true
)

data class CompletedAchievement(val achievementId: Int, val receivedAt: Long) {
    private var cachedAchievement: AchievementData? = null

    fun getAchievementData(): AchievementData {
        if (cachedAchievement == null) {
            cachedAchievement = AchievementsManager.instance.getAchievement(achievementId)
        }
        return cachedAchievement!!
    }
}