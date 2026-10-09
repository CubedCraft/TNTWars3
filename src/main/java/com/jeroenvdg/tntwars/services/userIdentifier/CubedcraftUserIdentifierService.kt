package com.jeroenvdg.tntwars.services.userIdentifier

import com.cubedcraft.cubedcore.Core
import com.jeroenvdg.tntwars.player.TNTWarsPlayer

class CubedcraftUserIdentifierService : IUserIdentifierService {
    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun getIdentifier(user: TNTWarsPlayer): Result<UserIdentifier> {
        println(Core.instance.playerManager.getPlayers())
        val userId = Core.instance.playerManager.getPlayer(user.bukkitPlayer.uniqueId)?.identifier?.intId ?: return Result.failure<UserIdentifier>(Exception(""))
        return Result.success(UserIdentifier(user.bukkitPlayer.uniqueId, userId))
    }
}