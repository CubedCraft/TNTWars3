package com.jeroenvdg.tntwars.managers.achievements.handlers

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.managers.achievements.AchievementHandler
import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.services.achievements.TNTWarsAchievementType

class KillstreakAchievementHandler(achievementsManager: AchievementsManager) : AchievementHandler(TNTWarsAchievementType.KILLSTREAK, achievementsManager) {

    init {
        EventBus.onPlayerDeath += ::handlePlayerDeath
    }

    override fun dispose() {
        EventBus.onPlayerDeath -= ::handlePlayerDeath
    }

    private fun handlePlayerDeath(deathContext: PlayerDeathContext) {
        if (!deathContext.hasDamager) return
        val user = deathContext.damager
        val killStreak = user.stats.killSteak
        for (achievement in achievements) {
            if (killStreak < achievement.requiredValue) continue
            if (user.achievements[achievement.index] != null) continue
            AchievementsManager.instance.achievementCompleted(user, achievement)
        }
    }
}