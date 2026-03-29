package com.jeroenvdg.tntwars.services.boosterService

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.IService
import com.jeroenvdg.tntwars.services.ServiceSingleton
import com.jeroenvdg.tntwars.services.userIdentifier.UserIdentifier

interface IBoosterService : IService {

    companion object : ServiceSingleton<IBoosterService>(IBoosterService::class.java)

    suspend fun getActiveBoosters(): Result<List<ActiveBooster>>
    suspend fun getBoostersForPlayer(user: TNTWarsPlayer): Result<List<Booster>>
    suspend fun activateBooster(activator: TNTWarsPlayer, booster: Booster): Result<ActiveBooster>
    suspend fun removeActiveBooster(activeBooster: ActiveBooster): Result<Unit>
}

data class Booster(val id: Int, val owner: UserIdentifier) {
    var hasBeenActivated = false
}
data class ActiveBooster(val id: Int, val activatedAt: Long, val isActive: Boolean)
