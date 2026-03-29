package com.jeroenvdg.tntwars.managers.achievements.handlers

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.game.MatchEndReason
import com.jeroenvdg.tntwars.managers.achievements.AchievementHandler
import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.services.achievements.TNTWarsAchievementType

class WinAchievementHandler(achievementManager: AchievementsManager) : AchievementHandler(TNTWarsAchievementType.WINS, achievementManager) {

    init {
        EventBus.onMatchEnded += ::handleMatchEnded
    }

    override fun dispose() {
        EventBus.onMatchEnded -= ::handleMatchEnded
    }

    private fun handleMatchEnded(matchEndReason: MatchEndReason) {
        val teamThatWon = matchEndReason.teamThatWon ?: return

        for (user in teamThatWon.usersInTeam()) {
            val wins = user.stats.wins
            for (achievement in achievements) {
                if (wins < achievement.requiredValue) continue
                if (user.achievements[achievement.index] != null) continue
                AchievementsManager.instance.achievementCompleted(user, achievement)
            }
        }
    }
}