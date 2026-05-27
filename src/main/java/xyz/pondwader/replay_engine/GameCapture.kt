package xyz.pondwader.replay_engine

import com.github.retrooper.packetevents.PacketEvents
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import com.github.luben.zstd.ZstdOutputStream
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
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalSerializationApi::class)
class GameCapture(
    private val plugin: Plugin,
    val world: World,
    recordingName: String,
    private val mapId: String? = null
) {
    private var pendingFrames = mutableListOf<CaptureFrame>()
    private val pendingLock = Any()
    private val fileLock = Any()
    private val replayDirectory = File(plugin.dataFolder, "replays")
    private val replayFile = File(replayDirectory, "${sanitizeRecordingName(recordingName)}.replay")

    private var listener: CaptureListener? = null
    private var packetListener: PacketEventsCaptureListener? = null
    private var writeTask: BukkitTask? = null
    private var output: DataOutputStream? = null
    private var startedAtTick = 0
    private var lastEmittedTick: Long = -1
    private val trackedEntityIds = ConcurrentHashMap.newKeySet<Int>()
    private val trackedVelocityEntityIds = ConcurrentHashMap.newKeySet<Int>()

    fun start() {
        if (output != null) return

        // Initialise output stream
        replayDirectory.mkdirs()
        val compressedOutput = ZstdOutputStream(BufferedOutputStream(FileOutputStream(replayFile, true)), ZSTD_LEVEL)
        output = DataOutputStream(compressedOutput)
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
        val captureEvent = event.toCaptureEvent()
        synchronized(pendingLock) {
            if (tick < lastEmittedTick) {
                throw IllegalArgumentException("Cannot emit capture event for tick $tick after tick $lastEmittedTick")
            }
            lastEmittedTick = tick

            val lastFrame = pendingFrames.lastOrNull()
            if (lastFrame != null && lastFrame.tick == tick) {
                lastFrame.events.add(captureEvent)
                return
            }

            pendingFrames.add(CaptureFrame(tick, mutableListOf(captureEvent)))
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

            val output = output ?: return
            val bytes = ProtoBuf.encodeToByteArray(CaptureFrameBatch(frames))
            output.writeInt(bytes.size)
            output.write(bytes)
        }
    }

    private fun drainPendingFrames(): List<CaptureFrame> {
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
            val bytes = ProtoBuf.encodeToByteArray(
                CaptureReplayHeader(
                    sourceWorldName = world.name,
                    mapId = mapId,
                    startedAtMillis = System.currentTimeMillis(),
                )
            )
            output.writeInt(bytes.size)
            output.write(bytes)
        }
    }

    private fun closeOutput() {
        writePendingFrames()

        synchronized(fileLock) {
            output?.close()
            output = null
        }
    }

    private fun emitEntitySnapshot() {
        val tick = currentTick()
        for (entity in world.entities) {
            if (entity is Player && entity.gameMode == GameMode.SPECTATOR) continue
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
        const val ZSTD_LEVEL = 3

        fun sanitizeRecordingName(recordingName: String): String {
            val sanitized = recordingName.removeSuffix(".replay")
            require(sanitized.isNotBlank()) { "Recording name cannot be blank" }
            require(!sanitized.contains(Regex("[\\\\/]"))) { "Recording name cannot contain path separators" }
            return sanitized
        }
    }
}
