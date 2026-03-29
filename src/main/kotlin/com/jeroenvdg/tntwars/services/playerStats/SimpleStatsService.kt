package com.jeroenvdg.tntwars.services.playerStats

import com.jeroenvdg.tntwars.managers.PlayerRoundSummary
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.userIdentifier.UserIdentifier

class SimpleStatsService : IPlayerStatsService {

    override fun init() { }
    override fun dispose() { }

    override suspend fun load(user: TNTWarsPlayer): Result<PlayerStats> {
        return Result.success(PlayerStats())
    }

    override suspend fun save(user: TNTWarsPlayer, stats: PlayerStats): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun saveBulk(pairs: List<Pair<TNTWarsPlayer, PlayerStats>>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun saveRoundSummary(roundData: RoundData, users: List<Pair<UserIdentifier, PlayerRoundSummary>>): Result<Unit> {
        return Result.success(Unit)
    }
}