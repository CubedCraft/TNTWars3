package com.jeroenvdg.tntwars.managers.achievements.handlers

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.game.MatchEndReason
import com.jeroenvdg.tntwars.managers.achievements.AchievementHandler
import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.services.achievements.TNTWarsAchievementType

class MVPAchievementHandler(achievementManager: AchievementsManager) : AchievementHandler(TNTWarsAchievementType.MVP, achievementManager) {

    init {
        EventBus.onMatchEnded += ::handleMatchEnded
    }

    override fun dispose() {
        EventBus.onMatchEnded -= ::handleMatchEnded
    }

    private fun handleMatchEnded(matchEndReason: MatchEndReason) {
        for (user in PlayerManager.instance.players) {
            var mvps = user.stats.mvpCount
            for (achievement in achievements) {
                if (mvps < achievement.requiredValue) continue
                if (user.achievements[achievement.index] != null) continue
                AchievementsManager.instance.achievementCompleted(user, achievement)
            }
        }
    }
}