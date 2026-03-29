package com.jeroenvdg.tntwars.managers.achievements.handlers

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.achievements.AchievementHandler
import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.achievements.TNTWarsAchievementType

class TeamBalancerAchievementHandler(achievementsManager: AchievementsManager) : AchievementHandler(TNTWarsAchievementType.TEAM_BALANCER, achievementsManager) {
    init {
        EventBus.onPlayerTeamChanged += ::handlePlayerTeamChanged
    }

    override fun dispose() {
        EventBus.onPlayerTeamChanged -= ::handlePlayerTeamChanged
    }

    private fun handlePlayerTeamChanged(user: TNTWarsPlayer, oldTeam: Team) {
        if (!user.team.isGameTeam || !oldTeam.isGameTeam) return
        val teamBalancer = user.stats.teamBalances
        for (achievement in achievements) {
            if (teamBalancer < achievement.requiredValue) continue
            if (user.achievements[achievement.index] != null) continue
            achievementsManager.achievementCompleted(user, achievement)
        }
    }
}