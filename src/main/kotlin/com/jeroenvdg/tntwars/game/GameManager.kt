package com.jeroenvdg.tntwars.game

import com.google.gson.JsonObject
import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.Soundial
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.states.*
import com.jeroenvdg.tntwars.interfaces.MapSelector
import com.jeroenvdg.tntwars.interfaces.TeamSelector
import com.jeroenvdg.tntwars.managers.PlayerStatsManager
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.tntwars.managers.mapManager.MapManager
import com.jeroenvdg.tntwars.managers.mapManager.TNTWarsMap
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.playerStats.RoundData
import com.jeroenvdg.tntwars.services.webhookService.IWebhookService
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.time.Duration
import kotlin.random.Random

class GameManager(val mapManager: MapManager, val plugin: Plugin) {

    companion object {
        val instance get() = TNTWars.instance.gameManager
        val config get() = TNTWars.instance.config
    }

    var teamSelectMode = TeamSelectMode.None; private set
    val playerGameContext get() = (stateMachine.activeState as BaseGameState).playerContextProvider
    val activeMap get() = currentMap!!
    var currentMap: ActiveMap? = null; private set
    var isActive = false; private set

    private val stateMachine: GameStateMachine = GameStateMachine(this)

    fun activate() {
        if (isActive) return
        loadRandomMap()
        if (currentMap == null) {
            return
        }

        isActive = true
        stateMachine.activate()
    }

    fun deactivate() {
        if (!isActive) return
        isActive = false
        currentMap?.dispose()
        stateMachine.deactivate()
    }

    fun startMatch(): Boolean {
        if (stateMachine.activeState !is WaitingState && stateMachine.activeState !is CountdownState) return false
        stateMachine.gotoState(MatchState::class.java)
        return true
    }

    fun endMatch(matchEndReason: MatchEndReason): Boolean {
        if (stateMachine.activeState !is MatchState) return false

        val titleTimes = Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))
        val title = Title.title(matchEndReason.title, matchEndReason.subtitle, titleTimes)
        Audience.audience(PlayerManager.instance.players.map { it.bukkitPlayer }).showTitle(title)
        Bukkit.broadcast(matchEndReason.message)
        Soundial.playAll(Soundial.MatchEnded, 1f)

        val playerStatsManager = PlayerStatsManager.instance
        val mostValuablePlayer = playerStatsManager.getMostValuablePlayer()
        if (mostValuablePlayer != null) {
            mostValuablePlayer.stats.mvpCount++
            playerStatsManager.applyRewards(mostValuablePlayer, TNTWars.instance.config.rewardConfig.mvpRewards)
        }

        if (matchEndReason.teamThatWon != null) {
            for (user in matchEndReason.teamThatWon.usersInTeam()) {
                playerStatsManager.addWin(user)
            }
        }

        val roundData = RoundData(
            activeMap.name,
            matchEndReason.teamThatWon?.name,
            activeMap.getMapData().gamemodeName,
            mostValuablePlayer?.identifier,
            activeMap.startedTime,
            System.currentTimeMillis()
        )

        playerStatsManager.sendRoundSummaries()
        playerStatsManager.saveAllUsers()
        playerStatsManager.saveRoundSummary(roundData)
        playerStatsManager.resetRoundStatistics()

        TeamSelector.instance.create()

        EventBus.onMatchEnded.invoke(matchEndReason)

        if (config.discordConfig.enabled && config.gameConfig.tournamentMode.enabled) {
            val wh = IWebhookService.current();
            var message = "Top Players\n"

            for(player in playerStatsManager.getPlayers()) {
                message += "* ${player.bukkitPlayer.name}: ${player.stats.kills} kills\n"
            }

            wh.send(message);
        }

        stateMachine.gotoState(MatchEndedState::class.java)
        return true
    }

    fun getTeamLives(team: Team): Int {
        val state = stateMachine.activeState as? MatchState
        return state?.getTeamLives(team) ?: 0
    }

    fun getTimeLeft(): Int {
        val state = stateMachine.activeState as? MatchState
        return state?.getTimeLeft() ?: 0;
    }

    fun setTeamSelectMode(mode: TeamSelectMode) {
        if (mode == teamSelectMode) return
        teamSelectMode = mode
        EventBus.onTeamSelectorModeChanged.invoke(mode)
    }

    fun loadMap(map: TNTWarsMap) {
        val previousMap = currentMap
        currentMap = mapManager.activateMap(map)
        stateMachine.gotoState(WaitingState::class.java)

        MapSelector.instance.create()

        EventBus.onMapChanged.invoke(currentMap!!)

        Bukkit.broadcast(activeMap.mapMessage)

        previousMap?.dispose()
    }

    fun loadMapFromSelector() {
        val mapsAvailable = MapSelector.instance.maps
        val countedVotes = HashMap<TNTWarsMap, Int>()
        for (tntWarsMap in mapsAvailable) {
            countedVotes[tntWarsMap] = 0
        }

        var highestVotedMap = mapsAvailable.random()
        var highestVoteCount = 0

        for (vote in MapSelector.instance.votes) {
            val count = (countedVotes[vote.value.map] ?: 0) + vote.value.amount
            countedVotes[vote.value.map] = count

            if (count > highestVoteCount || (count == highestVoteCount && Random.nextBoolean())) {
                highestVotedMap = vote.value.map
                highestVoteCount = count
            }
        }

        loadMap(highestVotedMap)
    }

    fun loadRandomMap() {
        if (mapManager.enabledElements.isEmpty()) {
            Debug.broadcast("&cNo maps enabled!")
            Debug.broadcast("&cNo maps enabled!")
            Debug.broadcast("&cNo maps enabled!")
            return
        }
        loadMap(mapManager.enabledElements.random())
    }

    fun sendQueuedPlayersToTeams() {
        val unselectedPlayers = Team.Queue.usersInTeam().toMutableList()

        val usersToJoinRed = ArrayList<TNTWarsPlayer>(unselectedPlayers.size)
        val usersToJoinBlue = ArrayList<TNTWarsPlayer>(unselectedPlayers.size)

        for ((userIdentifier, teamPreference) in TeamSelector.instance.teamSelectPreferences.toList().shuffled()) {
            val user = PlayerManager.instance.get(userIdentifier) ?: continue
            unselectedPlayers.remove(user)
            if (teamPreference == Team.Blue) {
                usersToJoinBlue.add(user)
            } else {
                usersToJoinRed.add(user)
            }
        }

        TeamSelector.instance.teamSelectPreferences.clear()

        for (unselectedPlayer in unselectedPlayers) {
            if (Random.nextBoolean()) {
                usersToJoinRed.add(unselectedPlayer)
            } else {
                usersToJoinBlue.add(unselectedPlayer)
            }
        }

        usersToJoinRed.shuffle()
        usersToJoinBlue.shuffle()

        val teamCounts = arrayOf(
            Team.Red.usersInTeam().size + usersToJoinRed.size,
            Team.Blue.usersInTeam().size + usersToJoinBlue.size,
        )

        val redPtr = 0
        val bluePtr = 1

        val smallestList: ArrayList<TNTWarsPlayer>
        val biggestList: ArrayList<TNTWarsPlayer>

        val smallestCountPtr: Int
        val biggestCountPtr: Int

        if (teamCounts[redPtr] > teamCounts[bluePtr]) {
            smallestList = usersToJoinBlue
            biggestList = usersToJoinRed
            smallestCountPtr = bluePtr
            biggestCountPtr = redPtr
        } else {
            smallestList = usersToJoinRed
            biggestList = usersToJoinBlue
            smallestCountPtr = redPtr
            biggestCountPtr = bluePtr
        }

        while (teamCounts[biggestCountPtr] - 1 > teamCounts[smallestCountPtr] && biggestList.isNotEmpty()) {
            smallestList.add(biggestList.removeLast())
            teamCounts[smallestCountPtr] = teamCounts[smallestCountPtr]+1
            teamCounts[biggestCountPtr] = teamCounts[biggestCountPtr]-1
        }

        val joinRedComp = Textial.msg.parse("&cYou have joined the Red team")
        val joinBlueComp = Textial.msg.parse("&cYou have joined the Blue team")

        for (user in usersToJoinRed) {
            user.team = Team.Red
            user.bukkitPlayer.sendMessage(joinRedComp)
        }
        for (user in usersToJoinBlue) {
            user.team = Team.Blue
            user.bukkitPlayer.sendMessage(joinBlueComp)
        }
    }

    fun addPlayerToRandomTeam(user: TNTWarsPlayer) {
        user.team = Team.Spectator

        val redCount = Team.Red.usersInTeam().size
        val blueCount = Team.Blue.usersInTeam().size

        when {
            redCount > blueCount -> {
                user.team = Team.Blue
            } blueCount > redCount -> {
                user.team = Team.Red
            } else -> if (Random.nextBoolean()) {
                user.team = Team.Red
            } else {
                user.team = Team.Blue
            }
        }
    }
}