package com.jeroenvdg.tntwars.services.boosterService

import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoosterConsumptionTest {
    @Test
    fun consumptionSurvivesReconnectWithEitherPoolDefault() {
        for (autoCommit in listOf(true, false)) {
            val db = BoosterDatabase(autoCommit)
            consumeBooster(db.connection, 42, 7, "tntwars") { Unit }
            assertFalse(db.persistedBooster, "Consumed booster must stay removed on reconnect")
            assertEquals(1, db.commits)
            assertEquals(listOf(42, 7, "tntwars"), db.parameters.values.toList())
            assertTrue(db.statementClosed)
        }
    }

    @Test
    fun alreadyConsumedBoosterCannotBeActivatedAgain() {
        val db = BoosterDatabase(false)
        consumeBooster(db.connection, 42, 7, "tntwars") { Unit }
        var activatedAgain = false
        assertFailsWith<IllegalStateException> {
            consumeBooster(db.connection, 42, 7, "tntwars") { activatedAgain = true }
        }
        assertFalse(activatedAgain)
        assertEquals(1, db.commits)
    }

    @Test
    fun wrongOwnerOrServerCannotConsumeBooster() {
        for ((owner, server) in listOf(8 to "tntwars", 7 to "other")) {
            val db = BoosterDatabase(true)
            assertFailsWith<IllegalStateException> {
                consumeBooster(db.connection, 42, owner, server) { error("Must not activate") }
            }
            assertTrue(db.persistedBooster)
            assertEquals(0, db.commits)
        }
    }

    @Test
    fun failedActivationRollsBackConsumption() {
        val db = BoosterDatabase(true)
        assertFailsWith<SQLException> {
            consumeBooster(db.connection, 42, 7, "tntwars") {
                throw SQLException("Active booster insert failed")
            }
        }
        assertTrue(db.persistedBooster)
        assertTrue(db.pendingBooster)
        assertEquals(1, db.rollbacks)
        assertEquals(0, db.commits)
    }

    @Test
    fun failedCommitDoesNotReportSuccess() {
        val db = BoosterDatabase(false)
        db.failCommit = true
        assertFailsWith<SQLException> {
            consumeBooster(db.connection, 42, 7, "tntwars") { Unit }
        }
        assertTrue(db.persistedBooster)
        assertEquals(1, db.rollbacks)
    }

    /** JDBC test double: reconnects observe committed rows, never pending changes. */
    private class BoosterDatabase(var autoCommit: Boolean) {
        var persistedBooster = true
        var pendingBooster = true
        var commits = 0
        var rollbacks = 0
        var failCommit = false
        var statementClosed = false
        val parameters = linkedMapOf<Int, Any>()

        val statement = proxy<PreparedStatement> { method, args ->
            when (method) {
                "setInt", "setString" -> { parameters[args[0] as Int] = args[1]!!; null }
                "executeUpdate" -> {
                    if (pendingBooster && parameters.values.toList() == listOf(42, 7, "tntwars")) {
                        pendingBooster = false
                        if (autoCommit) persistedBooster = false
                        1
                    } else 0
                }
                "close" -> { statementClosed = true; null }
                else -> error("Unexpected statement call: $method")
            }
        }

        val connection = proxy<Connection> { method, args ->
            when (method) {
                "setAutoCommit" -> { autoCommit = args[0] as Boolean; null }
                "getAutoCommit" -> autoCommit
                "prepareStatement" -> {
                    assertEquals(
                        "DELETE FROM player_boosters WHERE id = ? AND user_id = ? AND server = ?",
                        args[0]
                    )
                    statement
                }
                "commit" -> {
                    if (failCommit) throw SQLException("Commit failed")
                    persistedBooster = pendingBooster
                    commits++
                    null
                }
                "rollback" -> { pendingBooster = persistedBooster; rollbacks++; null }
                else -> error("Unexpected connection call: $method")
            }
        }
    }
}

private inline fun <reified T> proxy(crossinline call: (String, Array<out Any?>) -> Any?): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
        call(method.name, args ?: emptyArray())
    } as T
