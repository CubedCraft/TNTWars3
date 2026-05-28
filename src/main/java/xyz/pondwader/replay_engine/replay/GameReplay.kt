package xyz.pondwader.replay_engine.replay

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
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import xyz.pondwader.replay_engine.codec.CaptureBlockChangeEvent
import xyz.pondwader.replay_engine.codec.CaptureBlockDamageEvent
import xyz.pondwader.replay_engine.codec.CaptureChatMessageEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityDamageEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityMoveEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityRemoveEvent
import xyz.pondwader.replay_engine.codec.CaptureEntitySpawnEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityStateEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityVelocityEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityVisualStateEvent
import xyz.pondwader.replay_engine.codec.CaptureEventPayload
import xyz.pondwader.replay_engine.codec.CaptureExplosionEvent
import xyz.pondwader.replay_engine.codec.CaptureLocation
import xyz.pondwader.replay_engine.codec.CapturePlayerAnimationEvent
import xyz.pondwader.replay_engine.codec.CapturePlayerHeldItemEvent
import xyz.pondwader.replay_engine.codec.CapturePlayerOffhandItemEvent
import xyz.pondwader.replay_engine.codec.CaptureVector
import xyz.pondwader.replay_engine.codec.Deserializer
import xyz.pondwader.replay_engine.codec.Frame
import xyz.pondwader.replay_engine.codec.ReplayHeader
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.Closeable

class GameReplay(
    private val plugin: Plugin,
    replayFile: File,
    private val world: World,
    private val viewer: Player,
    private val onExit: () -> Unit,
) {
    private val entityRenderer = PacketEventsReplayEntityRenderer(viewer)
    private val frameReader = ReplayFrameReader(replayFile)
    private val queuedFrames = ArrayDeque<Frame>()
    val header: ReplayHeader

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
        val blockChanges = mutableListOf<PendingBlockChange>()

        while (true) {
            val frame = queuedFrames.firstOrNull() ?: break
            if (frame.tick > currentTick) break

            queuedFrames.removeFirst()
            applyFrame(frame, blockChanges)
            loadFramesIfNeeded()
        }

        scheduleBlockChanges(blockChanges)

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

        Bukkit.getScheduler().runTask(plugin, Runnable {
            onExit()
        })
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

    private fun applyFrame(frame: Frame, blockChanges: MutableList<PendingBlockChange>) {
        for (event in frame.events) {
            applyEvent(event, blockChanges)
        }
    }

    private fun applyEvent(event: CaptureEventPayload, blockChanges: MutableList<PendingBlockChange>) {
        when (event) {
            is CaptureBlockChangeEvent -> blockChanges.add(parseBlockChange(event))
            is CaptureChatMessageEvent -> applyChatMessage(event)
            is CaptureEntitySpawnEvent -> entityRenderer.spawn(event)
            is CaptureEntityRemoveEvent -> entityRenderer.remove(event.entityId)
            is CaptureEntityMoveEvent -> entityRenderer.move(event)
            is CaptureEntityVelocityEvent -> entityRenderer.velocity(event)
            is CaptureEntityStateEvent -> entityRenderer.state(event)
            is CaptureEntityVisualStateEvent -> entityRenderer.visualState(event)
            is CapturePlayerAnimationEvent -> entityRenderer.animation(event)
            is CapturePlayerHeldItemEvent -> entityRenderer.heldItem(event)
            is CapturePlayerOffhandItemEvent -> entityRenderer.offhandItem(event)
            is CaptureBlockDamageEvent -> applyBlockDamage(event)
            is CaptureEntityDamageEvent -> entityRenderer.damage(event)
            is CaptureExplosionEvent -> applyExplosion(event)
        }
    }

    private fun parseBlockChange(event: CaptureBlockChangeEvent): PendingBlockChange {
        return PendingBlockChange(
            x = event.position.x,
            y = event.position.y,
            z = event.position.z,
            blockData = Bukkit.createBlockData(event.newBlockData),
        )
    }

    private fun scheduleBlockChanges(blockChanges: List<PendingBlockChange>) {
        if (blockChanges.isEmpty()) return

        Bukkit.getScheduler().runTask(plugin, Runnable {
            for (event in blockChanges) {
                applyBlockChange(event)
            }
        })
    }

    private fun applyChatMessage(event: CaptureChatMessageEvent) {
        val message = LegacyComponentSerializer.legacySection().deserialize(event.message)

        viewer.sendMessage(
            Component.text()
                .append(
                    Component.text("(REPLAY) ")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.ITALIC)
                )
                .append(message)
                .build()
        )
    }

    private fun applyExplosion(event: CaptureExplosionEvent) {
        val location = event.location.toBukkitLocation(world)
        viewer.spawnParticle(Particle.EXPLOSION_EMITTER, location, 1, 0.0, 0.0, 0.0, 0.0)
        viewer.spawnParticle(Particle.EXPLOSION, location, 16, 1.5, 1.5, 1.5, 0.0)
        viewer.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 4.0f, 1.0f)
    }

    private fun applyBlockChange(event: PendingBlockChange) {
        val block = world.getBlockAt(event.x, event.y, event.z)
        block.blockData = event.blockData
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

        fun readHeader(file: File): ReplayHeader {
            ReplayFrameReader(file).use { reader ->
                return reader.header
            }
        }
    }

    private data class PendingBlockChange(
        val x: Int,
        val y: Int,
        val z: Int,
        val blockData: BlockData,
    )
}

private class ReplayFrameReader(file: File) : Closeable {
    private val input = Deserializer(BufferedInputStream(FileInputStream(file)))
    val header: ReplayHeader = input.readHeader()

    fun readNextBatch(): List<Frame>? {
        return input.readFrameBatch()?.frames
    }

    override fun close() {
        input.close()
    }
}

internal fun CaptureLocation.toBukkitLocation(world: World): org.bukkit.Location {
    return org.bukkit.Location(world, x, y, z, yaw, pitch)
}

internal fun CaptureVector.toBukkitVector(): org.bukkit.util.Vector {
    return org.bukkit.util.Vector(x, y, z)
}
