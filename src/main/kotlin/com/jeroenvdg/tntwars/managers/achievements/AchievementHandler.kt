package com.jeroenvdg.tntwars.managers.achievements

import com.jeroenvdg.tntwars.services.achievements.TNTWarsAchievementType

abstract class AchievementHandler(type: TNTWarsAchievementType, val achievementsManager: AchievementsManager) {

    val achievementType = type
    val achievements = achievementsManager.enabledAchievements.filter { it.type == achievementType }

    abstract fun dispose()

}