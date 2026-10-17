package com.jeroenvdg.tntwars.services.pumpkins

import com.jeroenvdg.tntwars.services.IService
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class HikariPumpkinStatsService(private val hikari: HikariDataSource, private val logger: Logger) : IService {
    // Read cache for commands/placeholders. Only successfully committed database totals enter this cache.
    private val scores = ConcurrentHashMap<UUID, PumpkinScore>()
    private val writer = Executors.newSingleThreadExecutor { task -> Thread(task, "TNTWars-pumpkin-stats") }

    override fun init() {

        hikari.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tntwars_pumpkin_stats (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    total BIGINT NOT NULL
                ) ENGINE=InnoDB
            """.trimIndent())
            }

        }
        hikari.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT player_uuid, total FROM tntwars_pumpkin_stats").use { result ->
                    while (result.next()) {
                        val id = UUID.fromString(result.getString("player_uuid"))
                        scores[id] = PumpkinScore(id, result.getLong("total"))
                    }
                }
            }
        }
    }

    fun getTotal(playerId: UUID): Long = scores[playerId]?.total ?: 0

    fun getRank(playerId: UUID): Int {
        val total = getTotal(playerId)
        if (total == 0L) return 0
        return scores.values.count { it.total > total } + 1
    }

    fun getLeaderboard(): List<PumpkinScore> = scores.values.filter { it.total > 0 }
        .sortedWith(compareByDescending<PumpkinScore> { it.total }.thenBy { it.playerId.toString() })

    fun add(playerId: UUID, amount: Int): CompletableFuture<Long> {
        require(amount > 0)
        return CompletableFuture.supplyAsync({
            val score = increment(playerId, amount)
            scores[playerId] = score
            score.total
        }, writer)
    }

    fun increment(playerId: UUID, amount: Int): PumpkinScore {
        hikari.connection.use { connection ->
            connection.autoCommit = false
            try {
                // Increment in SQL, never overwrite a persisted total with a potentially stale cached value.
                connection.prepareStatement("""
                    INSERT INTO tntwars_pumpkin_stats (player_uuid, total)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE total = total + ?
                """.trimIndent()).use { statement ->
                    statement.setString(1, playerId.toString())
                    statement.setInt(2, amount)
                    statement.setInt(3, amount)
                    statement.executeUpdate()
                }
                val score = connection.prepareStatement(
                    "SELECT total FROM tntwars_pumpkin_stats WHERE player_uuid = ?"
                ).use { statement ->
                    statement.setString(1, playerId.toString())
                    statement.executeQuery().use { result ->
                        check(result.next()) { "Could not read saved pumpkin total for $playerId" }
                        PumpkinScore(playerId, result.getLong("total"))
                    }
                }
                connection.commit()
                return score
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            }
        }
    }

    override fun dispose() {
        writer.shutdown()
        if (!writer.awaitTermination(30, TimeUnit.SECONDS)) {
            logger.severe("Timed out waiting for pumpkin totals to finish saving to MySQL")
        }
    }
}

data class PumpkinScore(val playerId: UUID,
                        val total: Long)
