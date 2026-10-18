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
            // 1. Skapa tabellen om den inte finns (med player_name inkluderad)
            connection.createStatement().use { statement ->
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tntwars_pumpkin_stats (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        player_name VARCHAR(36) NOT NULL,
                        total BIGINT NOT NULL
                    ) ENGINE=InnoDB
                """.trimIndent())
            }

            // 2. Lägg till kolumnen om tabellen fanns sedan tidigare men saknade den
            connection.createStatement().use { statement ->
                statement.executeUpdate("""
                    ALTER TABLE tntwars_pumpkin_stats ADD COLUMN IF NOT EXISTS player_name VARCHAR(36);
                """.trimIndent())
            }
        }

        // 3. Hämta all data inklusive player_name
        hikari.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT player_uuid, player_name, total FROM tntwars_pumpkin_stats").use { result ->
                    while (result.next()) {
                        val id = UUID.fromString(result.getString("player_uuid"))
                        val nameFromDb = result.getString("player_name")
                        // Old rows can have a NULL name, and players are usually offline during startup.
                        // Resolve a known name first and always provide a non-null final fallback.
                        val fallbackName = resolvePlayerName(id, nameFromDb)

                        scores[id] = PumpkinScore(
                            id,
                            fallbackName,
                            result.getLong("total")
                        )
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

    // Called by the command on the server thread so names can recover after startup or a player rename.
    // Do not replace a score here: a database write may be updating its committed total concurrently.
    fun getPlayerName(playerId: UUID): String = resolvePlayerName(playerId, scores[playerId]?.playerName)

    private fun knownName(playerId: UUID, playerName: String?): String? = playerName?.takeIf {
        it.isNotBlank() && !it.equals(playerId.toString(), ignoreCase = true)
    }

    private fun resolvePlayerName(playerId: UUID, storedName: String?): String {
        // Use Bukkit lookups only on the server thread (init, commands and the caller of add).
        // OfflinePlayer.name can also be null; a UUID is only the last resort until a name is known.
        return knownName(playerId, Bukkit.getPlayer(playerId)?.name)
            ?: knownName(playerId, Bukkit.getOfflinePlayer(playerId).name)
            ?: knownName(playerId, storedName)
            ?: playerId.toString()
    }

    fun add(playerId: UUID, playerName: String?, amount: Int): CompletableFuture<Long> {
        require(amount > 0)
        // Capture the name before entering the database worker; do not call Bukkit from that worker.
        // Treat legacy UUID placeholders and blank names as unknown rather than real player names.
        val resolvedName = knownName(playerId, playerName)
            ?: knownName(playerId, resolvePlayerName(playerId, scores[playerId]?.playerName))
        return CompletableFuture.supplyAsync({
            val score = increment(playerId, resolvedName, amount)
            scores[playerId] = score
            score.total
        }, writer)
    }

    private fun increment(playerId: UUID, playerName: String?, amount: Int): PumpkinScore {
        hikari.connection.use { connection ->
            connection.autoCommit = false
            try {
                // Increment in SQL, never overwrite a persisted total with a potentially stale cached value.
                // New tables require a non-null name. Store an empty value only when no name is known;
                // NULLIF/COALESCE keeps an existing name when this increment supplies no usable name.
                connection.prepareStatement("""
                    INSERT INTO tntwars_pumpkin_stats (player_uuid, player_name, total) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE player_name = COALESCE(NULLIF(VALUES(player_name), ''), player_name),
                        total = total + VALUES(total)
                """.trimIndent()).use { statement ->
                    statement.setString(1, playerId.toString())
                    statement.setString(2, playerName ?: "")
                    statement.setInt(3, amount)
                    statement.executeUpdate()
                }
                val score = connection.prepareStatement(
                    "SELECT player_name, total FROM tntwars_pumpkin_stats WHERE player_uuid = ?"
                ).use { statement ->
                    statement.setString(1, playerId.toString())
                    statement.executeQuery().use { result ->
                        check(result.next()) { "Could not read saved pumpkin total for $playerId" }
                        // Legacy rows may contain NULL, blank or UUID names. Keep the cache non-null
                        // without performing any Bukkit lookups on this database worker.
                        val savedName = knownName(playerId, result.getString("player_name"))
                            ?: playerName
                            ?: playerId.toString()
                        PumpkinScore(playerId, savedName, result.getLong("total"))
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

data class PumpkinScore(val playerId: UUID, val playerName: String, val total: Long)
