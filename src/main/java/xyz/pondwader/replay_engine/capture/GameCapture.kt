package xyz.pondwader.replay_engine.capture

import com.github.retrooper.packetevents.PacketEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.minecart.ExplosiveMinecart
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import xyz.pondwader.replay_engine.codec.CaptureChatMessageEvent
import xyz.pondwader.replay_engine.codec.CaptureEventPayload
import xyz.pondwader.replay_engine.codec.Frame
import xyz.pondwader.replay_engine.codec.ReplayHeader
import xyz.pondwader.replay_engine.codec.Serializer

class GameCapture(
    private val plugin: Plugin,
    val world: World,
    recordingName: String,
    private val mapId: String? = null
) {
    private var pendingFrames = mutableListOf<Frame>()
    private val pendingLock = Any()
    private val fileLock = Any()
    private val replayDirectory = File(plugin.dataFolder, "replays")
    private val replayFile = File(replayDirectory, "${sanitizeRecordingName(recordingName)}.replay")
    private val partialReplayFile = File(replayDirectory, "${replayFile.name}.part")

    private var listener: CaptureListener? = null
    private var packetListener: PacketEventsCaptureListener? = null
    private var writeTask: BukkitTask? = null
    private var output: Serializer? = null
    private var startedAtTick = 0
    private var lastEmittedTick: Long = -1
    private val includedPlayerIds = ConcurrentHashMap.newKeySet<java.util.UUID>()
    private val trackedEntityIds = ConcurrentHashMap.newKeySet<Int>()
    private val trackedVelocityEntityIds = ConcurrentHashMap.newKeySet<Int>()

    fun start() {
        if (output != null) return

        // Initialise output stream
        replayDirectory.mkdirs()
        output = Serializer(BufferedOutputStream(FileOutputStream(partialReplayFile)))
        writeHeader()

        startedAtTick = Bukkit.getCurrentTick()

        // Begin capturing events
        val listener = CaptureListener(this)
        Bukkit.getPluginManager().registerEvents(listener, plugin)
        this.listener = listener

        val packetListener = PacketEventsCaptureListener(this)
        PacketEvents.getAPI().eventManager.registerListener(packetListener)
        this.packetListener = packetListener

        // Snapshot of current entities in world
        emitEntitySnapshot()

        writeTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            writePendingFrames()
        }, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS)
    }

    fun captureEvent(event: CaptureEventPayload) {
        val tick = currentTick()
        synchronized(pendingLock) {
            if (tick < lastEmittedTick) {
                throw IllegalArgumentException("Cannot emit capture event for tick $tick after tick $lastEmittedTick")
            }
            lastEmittedTick = tick

            val lastFrame = pendingFrames.lastOrNull()
            if (lastFrame != null && lastFrame.tick == tick) {
                lastFrame.events.add(event)
                return
            }

            pendingFrames.add(Frame(tick, mutableListOf(event)))
        }
    }

    fun currentTick(): Long {
        return (Bukkit.getCurrentTick() - startedAtTick).toLong()
    }

    fun captureEventNextTick(eventProvider: () -> CaptureEventPayload?) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val event = eventProvider() ?: return@Runnable
            captureEvent(event)
        })
    }

    fun captureChatMessage(message: Component) {
        captureEvent(
            CaptureChatMessageEvent(
                message = LegacyComponentSerializer.legacySection().serialize(message),
            )
        )
    }

    fun addPlayer(player: Player) {
        includedPlayerIds.add(player.uniqueId)
        showPlayer(player)
    }

    fun removePlayer(player: Player) {
        includedPlayerIds.remove(player.uniqueId)
        hidePlayer(player)
    }

    internal fun isPlayerIncluded(player: Player): Boolean {
        return includedPlayerIds.contains(player.uniqueId)
    }

    internal fun showPlayer(player: Player) {
        if (player.world != world) return
        if (player.gameMode == GameMode.SPECTATOR) return
        if (isEntityTracked(player.entityId)) return
        trackEntity(player)
        captureEvent(player.toCaptureSpawnEvent())
    }

    internal fun hidePlayer(player: Player) {
        if (!isEntityTracked(player.entityId)) return
        untrackEntity(player.entityId)
        captureEvent(player.toCaptureRemoveEvent())
    }

    fun stop() {
        listener?.let { HandlerList.unregisterAll(it) }
        packetListener?.let { PacketEvents.getAPI().eventManager.unregisterListener(it) }

        writeTask?.cancel()
        closeOutput()
    }

    private fun writePendingFrames() {
        synchronized(fileLock) {
            val frames = drainPendingFrames()
            if (frames.isEmpty()) return

            output?.writeFrames(frames)
        }
    }

    private fun drainPendingFrames(): List<Frame> {
        synchronized(pendingLock) {
            if (pendingFrames.isEmpty()) return emptyList()
            val frames = pendingFrames
            pendingFrames = mutableListOf()
            return frames
        }
    }

    private fun writeHeader() {
        synchronized(fileLock) {
            val output = output ?: return
            output.writeHeader(
                ReplayHeader(
                    sourceWorldName = world.name,
                    mapId = mapId,
                    startedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun closeOutput() {
        writePendingFrames()

        synchronized(fileLock) {
            output?.close()
            output = null
        }

        completePartialFile()
    }

    private fun completePartialFile() {
        if (!partialReplayFile.exists()) return

        try {
            Files.move(
                partialReplayFile.toPath(),
                replayFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                partialReplayFile.toPath(),
                replayFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun emitEntitySnapshot() {
        for (entity in world.entities) {
            if (entity is Player) continue
            trackEntity(entity)
            captureEvent(entity.toCaptureSpawnEvent())
        }
    }

    internal fun trackEntity(entity: Entity) {
        trackedEntityIds.add(entity.entityId)
        if (entity is TNTPrimed || entity is ExplosiveMinecart) {
            trackedVelocityEntityIds.add(entity.entityId)
        }
    }

    internal fun untrackEntity(entityId: Int) {
        trackedEntityIds.remove(entityId)
        trackedVelocityEntityIds.remove(entityId)
    }

    internal fun isEntityTracked(entityId: Int): Boolean {
        return trackedEntityIds.contains(entityId)
    }

    internal fun isEntityVelocityTracked(entityId: Int): Boolean {
        return trackedVelocityEntityIds.contains(entityId)
    }

    private companion object {
        const val FLUSH_INTERVAL_TICKS = 20L * 5L

        fun sanitizeRecordingName(recordingName: String): String {
            val sanitized = recordingName.removeSuffix(".replay")
            require(sanitized.isNotBlank()) { "Recording name cannot be blank" }
            require(!sanitized.contains(Regex("[\\\\/]"))) { "Recording name cannot contain path separators" }
            return sanitized
        }
    }
}
