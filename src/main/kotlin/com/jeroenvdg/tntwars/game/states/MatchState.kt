package com.jeroenvdg.tntwars.game.states

import com.jeroenvdg.minigame_utilities.Scheduler
import com.jeroenvdg.minigame_utilities.SoundHelper
import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.InfluenceType
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.MatchEndReason
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.game.TeamSelectMode
import com.jeroenvdg.tntwars.managers.mapManager.RegionType
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.gameContexts.ClassicGameContext
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import kotlin.math.max
import kotlin.math.round

class MatchState : BaseGameState() {

    companion object {
        val tournamentModeConfig get() = TNTWars.instance.config.gameConfig.tournamentMode;
    }

    override val playerContextProvider = ClassicGameContext.Provider()
    override val teamSelectMode get() = TeamSelectMode.TempDisable

    private var maxLives = 0
    private val teamLives = hashMapOf<Team, Int>()
    private var bossbar: BossBar? = null
    private var timeLeft = 0

    override fun onActivate() {
        GameManager.instance.sendQueuedPlayersToTeams()
        PlayerManager.instance.updatePlayerVisibility()

        val playerCount = Team.Red.usersInTeam().size + Team.Blue.usersInTeam().size

        maxLives = if(tournamentModeConfig.enabled) {
            tournamentModeConfig.lives
        } else {
            when {
                playerCount <= 6 -> 8
                playerCount <= 10 -> 12
                else -> 15
            }
        }

        teamLives[Team.Red] = maxLives
        teamLives[Team.Blue] = maxLives

        EventBus.onPlayerDeath += ::handlePlayerDeath
        EventBus.onPlayerTeamChanged += ::handleTeamChanged
        EventBus.onPlayerJoined += ::handlePlayerJoined
        EventBus.onPlayerLeft += ::handlePlayerLeft
        EventBus.onAdminGameInfluence += ::handleGameInfluence

        gameManager.activeMap.startedTime = System.currentTimeMillis()

        timeLeft = TNTWars.instance.config.gameConfig.matchTime

        startCoroutine { countdownRoutine() }
        startCoroutine { disableJoinRoutine() }
    }

    override fun onDeactivate() {
        teamLives.clear()
        if (bossbar != null) {
            bossbar!!.removeAll()
            bossbar!!.isVisible = false
            bossbar = null
        }
        EventBus.onPlayerDeath -= ::handlePlayerDeath
        EventBus.onPlayerTeamChanged -= ::handleTeamChanged
        EventBus.onPlayerJoined -= ::handlePlayerJoined
        EventBus.onPlayerLeft -= ::handlePlayerLeft
        EventBus.onAdminGameInfluence -= ::handleGameInfluence
    }

    fun getTeamLives(team: Team): Int? {
        return teamLives[team];
    }

    fun getTimeLeft(): Int {
        return timeLeft;
    }

    private suspend fun countdownRoutine() {
        val map = gameManager.activeMap
        // Using while, so I can break out of this since kotlin doesn't support GOTO, only runs once tho
        while (map.gracePeriodTicks > 0) {
            val walls = map.getMapData().regions.filter { it.type == RegionType.Wall }
            if (walls.isEmpty()) {
                Bukkit.broadcast(TextHelper.error("Warning! Could not start grace period: No walls defined"))
                break
            }

            Bukkit.broadcast(TextHelper.deserialize("The match has started with a &p${round(map.gracePeriodTicks / 60.0 / 20.0 * 10.0) / 10.0}&r minute grace period"))
            map.protectedRegions.addAll(walls)

            val name = Component.text("Grace Period: ")
            bossbar = Bukkit.createBossBar(name.content(), BarColor.GREEN, BarStyle.SEGMENTED_6)
            val bossbar = this.bossbar!!

            for (user in PlayerManager.instance.players) {
                bossbar.addPlayer(user.bukkitPlayer)
            }
            bossbar.isVisible = true

            for (i in map.gracePeriodTicks downTo 0) {
                val format = when (i) {
                    2 * 60 * 20, 3 * 60 * 20, 4 * 60 * 20, 5 * 60 * 20 -> "&p${i / 60 / 20}&r minutes"
                    2 * 20, 3 * 20, 4 * 20, 5 * 20, 10 * 20, 30 * 20, 60 * 20 -> "&p${i / 20}&r seconds"
                    1 * 20 -> "&p1&r second"
                    else -> null
                }

                if (format != null) {
                    Bukkit.broadcast(TextHelper.deserialize("Grace period ends in $format"))
                    SoundHelper.playAll(SoundHelper.Sounds.Countdown)
                }

                if (i > 60 * 20) {
                    bossbar.setTitle("Grace Period: §a${round(i / 60f / 20f * 10) / 10}§r minutes")
                } else {
                    bossbar.setTitle("Grace Period: §a${max(i / 20, 1)}§r seconds")
                }
                bossbar.progress = i / map.gracePeriodTicks.toDouble()
                Scheduler.delay(1)
            }

            map.protectedRegions.removeAll(walls)
            bossbar.removeAll()
            bossbar.isVisible = false
            this.bossbar = null
            break
        }

        Bukkit.broadcast(TextHelper.deserialize("The match has started"))
        SoundHelper.playAll(SoundHelper.Sounds.MatchStarted)

        while (timeLeft > 0) {
            timeLeft--
            Scheduler.delay(20)

            val didDoMessage = when (timeLeft) {
                20*60 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p20&r minutes"))
                15*60 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p15&r minutes"))
                10*60 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p10&r minutes"))
                5*60 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p5&r minutes"))
                1*60 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p1&r minutes"))
                30 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p30&r seconds"))
                10 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p10&r seconds"))
                5 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p5&r seconds"))
                4 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p4&r seconds"))
                3 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p3&r seconds"))
                2 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p2&r seconds"))
                1 -> Bukkit.broadcast(TextHelper.deserialize("Game ends in &p1&r second"))
                else -> null
            } != null

            if (didDoMessage) {
                SoundHelper.playAll(SoundHelper.Sounds.Countdown)
            }

        }

        gameManager.endMatch(MatchEndReason.TimeLimitReached)
    }

    private suspend fun disableJoinRoutine() {
        gameManager.setTeamSelectMode(TeamSelectMode.TempDisable)
        Scheduler.delay(1 * 60 * 20)
        gameManager.setTeamSelectMode(TeamSelectMode.Join)
    }

    private fun handlePlayerDeath(deathContext: PlayerDeathContext) {
        val user = deathContext.user
        teamLives[user.team] = teamLives[user.team]!! - 1
        if (teamLives[user.team]!! > 0) return

        if (user.team == Team.Blue) {
            val isFlawless = teamLives[Team.Red] == maxLives
            if (isFlawless) EventBus.onFlawlessMatchEnded.invoke(Team.Red)
            gameManager.endMatch(MatchEndReason.RedTeamWon)
        } else {
            val isFlawless = teamLives[Team.Blue] == maxLives
            if (isFlawless) EventBus.onFlawlessMatchEnded.invoke(Team.Blue)
            gameManager.endMatch(MatchEndReason.BlueTeamWon)
        }
    }

    private fun handleTeamChanged(user: TNTWarsPlayer, oldTeam: Team) {
        PlayerManager.instance.updatePlayerVisibility(user)
    }

    private fun handlePlayerJoined(user: TNTWarsPlayer) {
        if (bossbar == null) return
        bossbar!!.addPlayer(user.bukkitPlayer)
    }

    private fun handlePlayerLeft(user: TNTWarsPlayer) {
        if (bossbar != null) bossbar!!.removePlayer(user.bukkitPlayer)

        if (!user.team.isGameTeam) return
        if (user.team.usersInTeam().isNotEmpty()) return
        gameManager.endMatch(MatchEndReason.NotEnoughPlayers)
    }

    private fun handleGameInfluence(influenceType: InfluenceType, data: List<Any>) {
        when (influenceType) {
            InfluenceType.setLives -> {
                val team = data[0] as Team
                val lives = data[1] as Int
                teamLives[team] = lives
            }
            InfluenceType.setTimer -> {
                timeLeft = (data[0] as Int) * 60
            }
            else -> {}
        }
    }
}