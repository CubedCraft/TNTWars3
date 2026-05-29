package com.jeroenvdg.tntwars.managers

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.block.Block
import org.bukkit.World
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import xyz.pondwader.replay_engine.codec.CaptureBlockPosition
import xyz.pondwader.replay_engine.codec.CaptureBlockChangeEvent
import xyz.pondwader.replay_engine.capture.GameCapture
import xyz.pondwader.replay_engine.replay.GameReplay
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ReplayManager(private val plugin: TNTWars) {
    private val replaysDirectory = File(plugin.dataFolder, "replays")
    private val activeReplays = HashMap<UUID, ActiveReplaySession>()
    private var activeCapture: GameCapture? = null

    fun startCapture(map: ActiveMap) {
        stopCapture()

        val mapData = map.getMapData()
        val recordingName =
            "${DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())}_${mapData.id}"
        val capture = GameCapture(plugin, map.managedWorld.world!!, recordingName, mapData.id).also { it.start() }
        activeCapture = capture

        for (user in PlayerManager.instance.players) {
            if (user.team.isGameTeam) {
                capture.addPlayer(user.bukkitPlayer)
            }
        }
    }

    fun stopCapture() {
        activeCapture?.stop()
        activeCapture = null
    }

    fun recordBlockChange(block: Block) {
        val capture = activeCapture ?: return
        if (!GameManager.instance.isGameWorld(block.world)) return
        val newBlockData = block.blockData.asString

        capture.captureEvent(
            CaptureBlockChangeEvent(
                position = CaptureBlockPosition(block.x, block.y, block.z),
                newBlockData = newBlockData,
            )
        )
    }

    fun recordChatMessage(message: Component) {
        activeCapture?.captureChatMessage(message)
    }

    fun addPlayerToCapture(player: Player) {
        activeCapture?.addPlayer(player)
    }

    fun removePlayerFromCapture(player: Player) {
        activeCapture?.removePlayer(player)
    }

    fun listReplays(): List<File> {
        if (!replaysDirectory.exists()) return emptyList()
        val replays = replaysDirectory.listFiles { file -> file.isFile && file.extension == "replay" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        pruneOldReplays(replays)
        return replays.take(MAX_REPLAYS)
    }

    fun startReplay(player: Player, replayFile: File) {
        stopReplay(player)

        val header = GameReplay.readHeader(replayFile)
        val mapId = header.mapId ?: throw IllegalStateException("Replay ${replayFile.name} does not include a map id")
        val map = plugin.mapManager.find(mapId) ?: throw IllegalStateException("Map $mapId no longer exists")
        val replayWorld =
            map.managedWorld.clone("active${File.separatorChar}replay_${map.id}__${UUID.randomUUID()}", false)
        replayWorld.load()

        val world = replayWorld.world!!
        world.isAutoSave = false
        clearReplayWorldEntities(world)
        val user = PlayerManager.instance.get(player)
        user?.team = Team.Detached

        val replay = GameReplay(plugin, replayFile, world, player) {
            if (player.isOnline) user?.team = Team.Spectator
            replayWorld.delete()
            activeReplays.remove(player.uniqueId)
        }

        activeReplays[player.uniqueId] = ActiveReplaySession(replay, replayWorld)
        replay.start()
    }

    fun pauseReplay(player: Player) {
        activeReplays[player.uniqueId]?.replay?.pause()
    }

    fun resumeReplay(player: Player) {
        activeReplays[player.uniqueId]?.replay?.resume()
    }

    fun stopReplay(player: Player) {
        val session = activeReplays.remove(player.uniqueId) ?: return
        session.replay.stop()
    }

    private fun clearReplayWorldEntities(world: World) {
        for (entity in world.entities) {
            if (entity is Player) continue
            entity.remove()
        }
    }

    private fun pruneOldReplays(replays: List<File>? = null) {
        if (!replaysDirectory.exists()) return
        val sortedReplays = replays ?: replaysDirectory.listFiles { file -> file.isFile && file.extension == "replay" }
            ?.sortedByDescending { it.lastModified() }
        ?: return

        for (replay in sortedReplays.drop(MAX_REPLAYS)) {
            replay.delete()
        }
    }

    private companion object {
        const val MAX_REPLAYS = 5
    }
}

private data class ActiveReplaySession(
    val replay: GameReplay,
    val world: ManagedWorld,
)
