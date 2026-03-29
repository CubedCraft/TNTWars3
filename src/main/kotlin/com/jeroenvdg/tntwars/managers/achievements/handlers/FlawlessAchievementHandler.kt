package com.jeroenvdg.tntwars.managers.achievements.handlers

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.achievements.AchievementHandler
import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.services.achievements.TNTWarsAchievementType
import com.jeroenvdg.minigame_utilities.Scheduler

class FlawlessAchievementHandler(achievementsManager: AchievementsManager) : AchievementHandler(TNTWarsAchievementType.FLAWLESS, achievementsManager) {

    init {
        EventBus.onFlawlessMatchEnded += ::handleMatchEnded
    }

    override fun dispose() {
        EventBus.onFlawlessMatchEnded -= ::handleMatchEnded
    }

    private fun handleMatchEnded(teamThatWon: Team) {
        for (tntWarsUser in teamThatWon.usersInTeam()) {
            for (achievement in achievements) {
                if (tntWarsUser.achievements[achievement.index] != null) continue

                // Make it run after the round summary message
                Scheduler.delayTick {
                    achievementsManager.achievementCompleted(tntWarsUser, achievement)
                }
            }
        }
    }
}