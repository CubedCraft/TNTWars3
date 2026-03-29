package com.jeroenvdg.tntwars.managers

import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.launchCoroutine
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.RewardConfig
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.playerStats.IPlayerStatsService
import com.jeroenvdg.tntwars.services.playerStats.RoundData
import com.jeroenvdg.tntwars.services.userIdentifier.UserIdentifier
import kotlinx.coroutines.Job
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

class PlayerStatsManager {

    companion object {
        val instance get() = TNTWars.instance.playerStatsManager
        val winRewards get() = TNTWars.instance.config.rewardConfig.winRewards
        val killRewards get() = TNTWars.instance.config.rewardConfig.killRewards
        val config get() = TNTWars.instance.config;
    }

    private val summaryMap = HashMap<UserIdentifier, PlayerRoundSummary>()

    init {
        EventBus.onPlayerDeath += ::handlePlayerDeath
        EventBus.onPlayerJoined += ::handlePlayerJoined
        EventBus.onPlayerTeamChanged += ::handlePlayerTeamChanged
    }

    fun dispose() {
        EventBus.onPlayerDeath -= ::handlePlayerDeath
        EventBus.onPlayerJoined -= ::handlePlayerJoined
        EventBus.onPlayerTeamChanged -= ::handlePlayerTeamChanged
    }

    private fun circledNumberUnicode(n: Int): String {
        return if (n in 1..10) {
            (0x2775 + n).toChar().toString()
        } else {
            n.toString()
        }
    }

    fun getMvpColor(index: Int): String {
        return when (index) {
            0 -> "&6"
            1 -> "&7"
            2 -> "&3"
            else -> "&8"
        }
    }

    fun sendRoundSummaries() {
        val lineComponent = TextHelper.format("&7&m        &r &6Round Summary &8&m        &r")
        for ((userIdentifier, summary) in summaryMap) {
            val component = Component.text()
                .append(lineComponent).appendNewline()

            if(config.gameConfig.tournamentMode.enabled) {
                val mostValuablePlayers = getMostValuablePlayers(3)
                for ((index, mvp) in mostValuablePlayers.withIndex()) {
                    component.append(
                        TextHelper.format(" ${getMvpColor(index)}${circledNumberUnicode(index + 1)} &${mvp.team.primaryColor}${mvp.bukkitPlayer.name} &7(${summary.kills} kills)")
                    ).appendNewline()
                }
            } else {
                val mostValuablePlayers = getMostValuablePlayer();
                component
                    .append(TextHelper.format(" &fMatch MVP: &p${mostValuablePlayers?.bukkitPlayer?.name}")).appendNewline()
                    .append(TextHelper.format(" ")).appendNewline()
                    .append(TextHelper.format(" &fKills: &p${summary.kills}")).appendNewline()
                    .append(TextHelper.format(" &fDeaths: &p${summary.deaths}")).appendNewline()
                    .append(TextHelper.format(" &fCoins: &p${summary.coins}")).appendNewline()
                    .append(TextHelper.format(" &fScore: &p${summary.score}")).appendNewline()
            }

            component.append(lineComponent)

            Bukkit.getPlayer(userIdentifier.uuid)?.sendMessage(component)
        }
    }

    fun getMostValuablePlayer(): TNTWarsPlayer? {
        val availableMVPs = summaryMap.filter { PlayerManager.instance.get(it.key) != null }
        var mostValuablePlayer = availableMVPs.keys.firstOrNull() ?: return null
        var mvpWeight = -1
        for ((player, summary) in availableMVPs) {
            val achievedKills = summary.kills
            val currentWeight = achievedKills
            if (currentWeight > mvpWeight) {
                mostValuablePlayer = player
                mvpWeight = currentWeight
            }
        }

        return PlayerManager.instance.get(mostValuablePlayer)!!
    }

    private fun getMostValuablePlayers(amount: Int): List<TNTWarsPlayer> {
        val availableMVPs = summaryMap.filter { PlayerManager.instance.get(it.key) != null }
        return availableMVPs
            .entries
            .sortedByDescending { it.value.kills }
            .take(amount)
            .mapNotNull { (uuid, _) -> PlayerManager.instance.get(uuid) }
    }

    fun getPlayers(): List<TNTWarsPlayer> {
        return summaryMap
            .entries
            .sortedByDescending { it.value.kills }
            .toList()
            .mapNotNull { (uuid, _) -> PlayerManager.instance.get(uuid) }
    }

    fun saveAllUsers(): Job {
        return launchCoroutine {
            val users = summaryMap.filter { PlayerManager.instance.get(it.key) != null }.map { val user = PlayerManager.instance.get(it.key)!!; Pair(user, user.stats) }
            val saveResult = IPlayerStatsService.current().saveBulk(users)
            if (saveResult.isFailure) {
                Debug.error(Exception("Bulk save failed! THE ROOM IS ON FIRE", saveResult.exceptionOrNull()))
            }
        }
    }

    fun saveOne(user: TNTWarsPlayer): Job {
        return launchCoroutine {
            val saveResult = IPlayerStatsService.current().save(user, user.stats)
            if (saveResult.isFailure) {
                Debug.error(Exception("Save failed! THE ROOM IS ON FIRE", saveResult.exceptionOrNull()))
            }
        }
    }

    fun saveRoundSummary(roundData: RoundData): Job {
        return launchCoroutine {
            val saveResult = IPlayerStatsService.current().saveRoundSummary(roundData, summaryMap.filter { it.value.team.isGameTeam }.toList())
            if (saveResult.isFailure) {
                Debug.error(Exception("Save failed! THE ROOM IS ON FIRE", saveResult.exceptionOrNull()))
            }
        }
    }

    fun resetRoundStatistics() {
        summaryMap.clear()
        for (user in PlayerManager.instance.players) {
            summaryMap[user.identifier] = PlayerRoundSummary(user.team)
        }
    }

    fun addKill(user: TNTWarsPlayer) {
        user.stats.kills++
        user.stats.killSteak++
        summaryMap[user.identifier]!!.kills++
        applyRewards(user, killRewards)
    }

    fun addDeath(user: TNTWarsPlayer) {
        user.stats.deaths++
        user.stats.killSteak = 0
        summaryMap[user.identifier]!!.deaths++
    }

    fun addWin(user: TNTWarsPlayer) {
        user.stats.wins++
        applyRewards(user, winRewards)
    }

    fun addTeamBalance(user: TNTWarsPlayer) {
        user.stats.teamBalances++
    }

    fun removeCoins(user: TNTWarsPlayer, coins: Int) {
        user.stats.coins -= coins
    }

    fun applyRewards(user: TNTWarsPlayer, reward: RewardConfig, refreshScoreboard: Boolean = true) {
        addScore(user, reward.score * BoosterManager.instance.multiplier)
        addCoins(user, reward.coins * BoosterManager.instance.multiplier)
    }

    private fun addCoins(user: TNTWarsPlayer, coins: Int) {
        user.stats.coins += coins
        summaryMap[user.identifier]!!.coins += coins
    }

    private fun addScore(user: TNTWarsPlayer, score: Int) {
        user.stats.score += score
        summaryMap[user.identifier]!!.score += score
        user.updateRank()
    }

    private fun addRoundStats(user: TNTWarsPlayer) {
        if (summaryMap[user.identifier] != null) return
        summaryMap[user.identifier] = PlayerRoundSummary(user.team)
    }

    private fun removeRoundStats(user: TNTWarsPlayer) {
        summaryMap.remove(user.identifier)
    }

    private fun handlePlayerDeath(deathContext: PlayerDeathContext) {
        if (deathContext.hasDamager) addKill(deathContext.damager)
        addDeath(deathContext.user)
    }

    private fun handlePlayerJoined(user: TNTWarsPlayer) {
        addRoundStats(user)
    }

    private fun handlePlayerTeamChanged(user: TNTWarsPlayer, oldTeam: Team) {
        if (!user.team.isGameTeam || !oldTeam.isGameTeam) return
        summaryMap[user.identifier]!!.team = user.team
        addTeamBalance(user)
    }
}

class PlayerRoundSummary(var team: Team) {
    var kills = 0
    var deaths = 0
    var coins = 0
    var score = 0
}