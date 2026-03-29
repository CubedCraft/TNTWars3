package com.jeroenvdg.tntwars.managers.achievements.handlers

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.managers.achievements.AchievementHandler
import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.services.achievements.TNTWarsAchievementType

class KillAchievementHandler(achievementsManager: AchievementsManager) : AchievementHandler(TNTWarsAchievementType.KILLS, achievementsManager) {

    init {
        EventBus.onPlayerDeath += ::handlePlayerDeath
    }

    override fun dispose() {
        EventBus.onPlayerDeath -= ::handlePlayerDeath
    }

    private fun handlePlayerDeath(deathContext: PlayerDeathContext) {
        if (!deathContext.hasDamager) return

        val damager = deathContext.damager
        val kills = damager.stats.kills

        for (achievement in achievements) {
            if (kills < achievement.requiredValue) continue
            if (damager.achievements[achievement.index] != null) continue
            achievementsManager.achievementCompleted(damager, achievement)
        }
    }
}