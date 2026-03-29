package com.jeroenvdg.tntwars.services.playerStats

import com.jeroenvdg.tntwars.managers.PlayerRoundSummary
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.IService
import com.jeroenvdg.tntwars.services.ServiceSingleton
import com.jeroenvdg.tntwars.services.userIdentifier.UserIdentifier

interface IPlayerStatsService : IService {
    companion object : ServiceSingleton<IPlayerStatsService>(IPlayerStatsService::class.java)

    suspend fun load(user: TNTWarsPlayer): Result<PlayerStats>
    suspend fun save(user: TNTWarsPlayer, stats: PlayerStats): Result<Unit>
    suspend fun saveBulk(pairs: List<Pair<TNTWarsPlayer, PlayerStats>>): Result<Unit>
    suspend fun saveRoundSummary(roundData: RoundData, users: List<Pair<UserIdentifier, PlayerRoundSummary>>): Result<Unit>
}

class RoundData (
    val mapName: String,
    val winningTeamName: String?,
    val gamemodeName: String,
    val mvp: UserIdentifier?,
    val startedTime: Long,
    val endedTime: Long
)

class PlayerStats {
    var wins = 0
    var score = 0
    var coins = 0
    var kills = 0
    var deaths = 0
    var mvpCount = 0
    var teamBalances = 0
    var killSteak = 0 // Do not save in DB
    var lastJoinedAt = 0L
    var playTimeBeforeJoin = 0L
}