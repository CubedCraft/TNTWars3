package com.jeroenvdg.tntwars.services.boosterService

import java.sql.Connection

/**
 * Commits consumption before reporting success, including on pools with auto-commit disabled.
 * The optional activation runs in the same transaction when both tables share a database.
 */
internal fun <T> consumeBooster(
    connection: Connection,
    boosterId: Int,
    userId: Int,
    serverName: String,
    activate: () -> T
): T {
    connection.autoCommit = false
    try {
        connection.prepareStatement(
            "DELETE FROM player_boosters WHERE id = ? AND user_id = ? AND server = ?"
        ).use { statement ->
            statement.setInt(1, boosterId)
            statement.setInt(2, userId)
            statement.setString(3, serverName)
            check(statement.executeUpdate() == 1) {
                "Booster is no longer available for this player and server"
            }
        }
        val result = activate()
        connection.commit()
        return result
    } catch (failure: Throwable) {
        try {
            connection.rollback()
        } catch (rollbackFailure: Throwable) {
            failure.addSuppressed(rollbackFailure)
        }
        throw failure
    }
}
