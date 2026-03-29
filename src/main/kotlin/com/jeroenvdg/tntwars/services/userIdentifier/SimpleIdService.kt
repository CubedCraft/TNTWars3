package com.jeroenvdg.tntwars.services.userIdentifier

import com.jeroenvdg.tntwars.player.TNTWarsPlayer

class SimpleIdService : IUserIdentifierService {
    override fun init() {
    }

    override fun dispose() {
    }

    override suspend fun getIdentifier(user: TNTWarsPlayer): Result<UserIdentifier> {
        return Result.success(UserIdentifier(user.bukkitPlayer.uniqueId, user.bukkitPlayer.uniqueId.hashCode()))
    }
}