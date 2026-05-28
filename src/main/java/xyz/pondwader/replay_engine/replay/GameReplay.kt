package xyz.pondwader.replay_engine.replay

import com.github.luben.zstd.ZstdInputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import xyz.pondwader.replay_engine.codec.CaptureBlockChangeEvent
import xyz.pondwader.replay_engine.codec.CaptureBlockDamageEvent
import xyz.pondwader.replay_engine.codec.CaptureChatMessageEvent
import xyz.pondwader.replay_engine.codec.CaptureReplayHeader
import xyz.pondwader.replay_engine.codec.CaptureFrame
import xyz.pondwader.replay_engine.codec.CaptureEvent
import xyz.pondwader.replay_engine.codec.CaptureEventTypes
import xyz.pondwader.replay_engine.codec.CaptureExplosionEvent
import xyz.pondwader.replay_engine.codec.CaptureFrameBatch
import xyz.pondwader.replay_engine.codec.CaptureLocation
import xyz.pondwader.replay_engine.codec.CaptureVector
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.Closeable

@OptIn(ExperimentalSerializationApi::class)
class GameReplay(
    private val plugin: Plugin,
    replayFile: File,
    private val world: World,
    private val viewer: Player,
    private val onExit: () -> Unit,
) {
    private val entityRenderer = PacketEventsReplayEntityRenderer(viewer)
    private val frameReader = ReplayFrameReader(replayFile)
    private val queuedFrames = ArrayDeque<CaptureFrame>()
    val header: CaptureReplayHeader

    private var task: BukkitTask? = null
    private var currentTick = 0L
    private var paused = false
    private var ended = false
    private var endOfReplay = false

    init {
        ensureReplayWorldListenerRegistered(plugin)
        activeReplayWorlds += world
        header = frameReader.header
    }

    fun start() {
        if (task != null) return

        viewer.gameMode = GameMode.SPECTATOR
        viewer.teleport(world.spawnLocation)

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            tick()
        }, 0L, 1L)
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun stop() {
        finish()
    }

    private fun tick() {
        if (paused || ended) return

        loadFramesIfNeeded()

        while (true) {
            val frame = queuedFrames.firstOrNull() ?: break
            if (frame.tick > currentTick) break

            queuedFrames.removeFirst()
            applyFrame(frame)
            loadFramesIfNeeded()
        }

        if (endOfReplay && queuedFrames.isEmpty()) {
            finish()
            return
        }

        currentTick++
    }

    private fun finish() {
        if (ended) return
        ended = true

        task?.cancel()
        entityRenderer.clear()
        frameReader.close()
        activeReplayWorlds -= world

        onExit()
    }

    private fun loadFramesIfNeeded() {
        while (queuedFrames.isEmpty() && !endOfReplay) {
            val frames = frameReader.readNextBatch()
            if (frames == null) {
                endOfReplay = true
                return
            }
            queuedFrames.addAll(frames)
        }
    }

    private fun applyFrame(frame: CaptureFrame) {
        for (event in frame.events) {
            applyEvent(event)
        }
    }

    private fun applyEvent(event: CaptureEvent) {
        when (event.type) {
            CaptureEventTypes.BLOCK_CHANGE -> event.blockChange?.let { applyBlockChange(it) }
            CaptureEventTypes.CHAT_MESSAGE -> event.chatMessage?.let { applyChatMessage(it) }
            CaptureEventTypes.ENTITY_SPAWN -> event.entitySpawn?.let { entityRenderer.spawn(it) }
            CaptureEventTypes.ENTITY_REMOVE -> event.entityRemove?.let { entityRenderer.remove(it.entityId) }
            CaptureEventTypes.ENTITY_MOVE -> event.entityMove?.let { entityRenderer.move(it) }
            CaptureEventTypes.ENTITY_VELOCITY -> event.entityVelocity?.let { entityRenderer.velocity(it) }
            CaptureEventTypes.ENTITY_STATE -> event.entityState?.let { entityRenderer.state(it) }
            CaptureEventTypes.ENTITY_VISUAL_STATE -> event.entityVisualState?.let { entityRenderer.visualState(it) }
            CaptureEventTypes.PLAYER_ANIMATION -> event.playerAnimation?.let { entityRenderer.animation(it) }
            CaptureEventTypes.PLAYER_HELD_ITEM -> event.playerHeldItem?.let { entityRenderer.heldItem(it) }
            CaptureEventTypes.PLAYER_OFFHAND_ITEM -> event.playerOffhandItem?.let { entityRenderer.offhandItem(it) }
            CaptureEventTypes.BLOCK_DAMAGE -> event.blockDamage?.let { applyBlockDamage(it) }
            CaptureEventTypes.BLOCK_BREAK_ANIMATION -> event.blockBreakAnimation?.let {
                entityRenderer.blockBreakAnimation(
                    it
                )
            }

            CaptureEventTypes.ENTITY_DAMAGE -> event.entityDamage?.let { entityRenderer.damage(it) }
            CaptureEventTypes.EXPLOSION -> event.explosion?.let { applyExplosion(it) }
        }
    }

    private fun applyChatMessage(event: CaptureChatMessageEvent) {
        val message = LegacyComponentSerializer.legacySection().deserialize(event.message)

        viewer.sendMessage(
            Component.text("(REPLAY) ")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.ITALIC)
                .append(message)
        )
    }

    private fun applyExplosion(event: CaptureExplosionEvent) {
        val location = event.location.toBukkitLocation(world)
        viewer.spawnParticle(Particle.EXPLOSION_EMITTER, location, 1, 0.0, 0.0, 0.0, 0.0)
        viewer.spawnParticle(Particle.EXPLOSION, location, 16, 1.5, 1.5, 1.5, 0.0)
        viewer.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 4.0f, 1.0f)
    }

    private fun applyBlockChange(event: CaptureBlockChangeEvent) {
        val blockData = event.newBlockData ?: return
        val block = world.getBlockAt(event.position.x, event.position.y, event.position.z)
        block.blockData = Bukkit.createBlockData(blockData)
    }

    private fun applyBlockDamage(event: CaptureBlockDamageEvent) {
        val location = org.bukkit.Location(
            world,
            event.position.x.toDouble(),
            event.position.y.toDouble(),
            event.position.z.toDouble()
        )
        viewer.sendBlockDamage(location, event.progress, event.playerUuid.hashCode())
    }

    companion object {
        private val activeReplayWorlds = HashSet<World>()
        private var replayWorldListenerRegistered = false

        fun isReplayWorld(world: World): Boolean {
            return activeReplayWorlds.contains(world)
        }

        private fun ensureReplayWorldListenerRegistered(plugin: Plugin) {
            if (replayWorldListenerRegistered) return

            Bukkit.getPluginManager().registerEvents(ReplayWorldListener(), plugin)
            replayWorldListenerRegistered = true
        }

        fun readHeader(file: File): CaptureReplayHeader {
            ReplayFrameReader(file).use { reader ->
                return reader.header
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class ReplayFrameReader(file: File) : Closeable {
    private val input = DataInputStream(ZstdInputStream(BufferedInputStream(FileInputStream(file))))
    val header: CaptureReplayHeader = ProtoBuf.decodeFromByteArray(input.readRecord())

    fun readNextBatch(): List<CaptureFrame>? {
        val bytes = try {
            input.readRecord()
        } catch (_: EOFException) {
            return null
        }

        return ProtoBuf.decodeFromByteArray<CaptureFrameBatch>(bytes).frames
    }

    override fun close() {
        input.close()
    }

    private fun DataInputStream.readRecord(): ByteArray {
        val length = readInt()
        val bytes = ByteArray(length)
        readFully(bytes)
        return bytes
    }
}

internal fun CaptureLocation.toBukkitLocation(world: World): org.bukkit.Location {
    return org.bukkit.Location(world, x, y, z, yaw, pitch)
}

internal fun CaptureVector.toBukkitVector(): org.bukkit.util.Vector {
    return org.bukkit.util.Vector(x, y, z)
}
