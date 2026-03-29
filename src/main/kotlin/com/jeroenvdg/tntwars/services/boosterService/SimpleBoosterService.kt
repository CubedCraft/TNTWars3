package com.jeroenvdg.tntwars.services.boosterService

import com.jeroenvdg.tntwars.player.TNTWarsPlayer

class SimpleBoosterService : IBoosterService {

    private var index = 0

    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun getActiveBoosters(): Result<List<ActiveBooster>> {
        return Result.success(emptyList())
    }

    override suspend fun getBoostersForPlayer(user: TNTWarsPlayer): Result<List<Booster>> {
        return Result.success(emptyList())
    }

    override suspend fun activateBooster(activator: TNTWarsPlayer, booster: Booster): Result<ActiveBooster> {
        return Result.success(ActiveBooster(index++, System.currentTimeMillis(), true))
    }

    override suspend fun removeActiveBooster(activeBooster: ActiveBooster): Result<Unit> {
        return Result.success(Unit)
    }
}