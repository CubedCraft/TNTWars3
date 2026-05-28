package com.jeroenvdg.tntwars.services.userIdentifier

import com.cubedcraft.core.user.UserManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer

class CubedcraftUserIdentifierService : IUserIdentifierService {
    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun getIdentifier(user: TNTWarsPlayer): Result<UserIdentifier> {
        val userId = UserManager.getInstance().getID(user.bukkitPlayer.uniqueId)
        return Result.success(UserIdentifier(user.bukkitPlayer.uniqueId, userId))
    }
}