package com.jeroenvdg.tntwars.services.userIdentifier

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.IService
import com.jeroenvdg.tntwars.services.ServiceSingleton
import java.util.*

interface IUserIdentifierService : IService {

    companion object : ServiceSingleton<IUserIdentifierService>(IUserIdentifierService::class.java)

    suspend fun getIdentifier(user: TNTWarsPlayer): Result<UserIdentifier>

}

data class UserIdentifier(val uuid: UUID, val intId: Int) {
    override fun hashCode(): Int {
        return intId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserIdentifier

        if (uuid != other.uuid) return false
        if (intId != other.intId) return false

        return true
    }
}