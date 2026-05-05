package com.jeroenvdg.tntwars.managers.achievements

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo
import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.JobResult
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.managers.achievements.handlers.*
import com.jeroenvdg.tntwars.misc.Toast
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.achievements.AchievementData
import com.jeroenvdg.tntwars.services.achievements.CompletedAchievement
import com.jeroenvdg.tntwars.services.achievements.IAchievementsService
import com.jeroenvdg.tntwars.services.achievements.TNTWarsAchievementType
import io.papermc.paper.advancement.AdvancementDisplay
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.Material

class AchievementsManager {

    companion object {
        val instance get() = TNTWars.instance.achievementManager

        private val icons: Map<TNTWarsAchievementType, Material> = mapOf(
            TNTWarsAchievementType.KILLS to Material.DIAMOND_SWORD,
            TNTWarsAchievementType.WINS to Material.TOTEM_OF_UNDYING,
            TNTWarsAchievementType.TEAM_BALANCER to Material.FEATHER,
            TNTWarsAchievementType.KILLSTREAK to Material.BLAZE_POWDER, // "On fire" känsla
            TNTWarsAchievementType.MVP to Material.EMERALD,
            TNTWarsAchievementType.FLAWLESS to Material.DIAMOND,       // Symbol för perfektion
            TNTWarsAchievementType.DROWNER to Material.PRISMARINE_CRYSTALS, // Vatten-tema
            TNTWarsAchievementType.SCORE to Material.GOLD_INGOT,       // Klassisk valuta/poäng
            TNTWarsAchievementType.CLICKEGG to Material.DRAGON_EGG     // Det ultimata ägget
        )
    }

    var enabledAchievements: Collection<AchievementData> private set

    private val achievementHandlers = ArrayList<AchievementHandler>()

    init {
        runBlocking {
            val enabledAchievementsResult = IAchievementsService.current().getEnabledAchievements()
            if (enabledAchievementsResult.isFailure) {
                throw Exception("Unable to get all achievements", enabledAchievementsResult.exceptionOrNull())
            }

            enabledAchievements = enabledAchievementsResult.getOrThrow()
            createAchievementHandlers()
        }
    }

    fun dispose() {
        for (achievementHandler in achievementHandlers) {
            achievementHandler.dispose()
        }
    }

    fun getAchievement(achievementId: Int) = enabledAchievements.first { it.id == achievementId }

    fun achievementCompleted(user: TNTWarsPlayer, achievement: AchievementData): JobResult<CompletedAchievement> {
        broadcastAchievementGained(user, achievement)
        return JobResult {
            val result = IAchievementsService.current().completeAchievement(user, achievement)
            if (result.isFailure) {
                Debug.log("Failed to save achievement ${achievement.id} (${achievement.title}) for ${user.bukkitPlayer.name}")
                return@JobResult result
            }

            user.achievements[achievement.index] = result.getOrThrow()
            return@JobResult result
        }
    }

    private fun broadcastAchievementGained(user: TNTWarsPlayer, achievement: AchievementData) {
        Toast.show(user, icons[achievement.type]!!, achievement.title, AdvancementDisplay.Frame.GOAL)
        Bukkit.broadcast(Textial.bc.format("&p${user.bukkitPlayer.name}&r completed achievement &s${achievement.title}&r!"))
    }

    suspend fun loadPlayerAchievements(user: TNTWarsPlayer):Result<Array<CompletedAchievement?>>{
        val result = IAchievementsService.current().getCompletedAchievements(user)
        if (result.isFailure) return Result.failure(Exception("Unable to fetch achievements", result.exceptionOrNull()))
        val achievements = result.getOrThrow()

        val achievementsList = arrayOfNulls<CompletedAchievement>(enabledAchievements.size)
        for (enabledAchievement in enabledAchievements) {
            val playerAchievement = achievements.firstOrNull { it.achievementId == enabledAchievement.id } ?: continue
            achievementsList[enabledAchievement.index] = playerAchievement
        }

        return Result.success(achievementsList)
    }

    private fun createAchievementHandlers() {
        achievementHandlers.add(WinAchievementHandler(this))
        achievementHandlers.add(MVPAchievementHandler(this))
        achievementHandlers.add(KillAchievementHandler(this))
        achievementHandlers.add(TeamBalancerAchievementHandler(this))
        achievementHandlers.add(KillstreakAchievementHandler(this))
        achievementHandlers.add(FlawlessAchievementHandler(this))
    }
}