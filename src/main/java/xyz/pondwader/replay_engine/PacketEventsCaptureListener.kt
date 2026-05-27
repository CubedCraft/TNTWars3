package xyz.pondwader.replay_engine

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity

internal class PacketEventsCaptureListener(private val capture: GameCapture) :
    PacketListenerAbstract(PacketListenerPriority.MONITOR) {
    private var dedupeTick = -1L
    private val velocityKeys = HashSet<VelocityKey>()
    private val blockAnimationKeys = HashSet<BlockAnimationKey>()

    override fun onPacketSend(event: PacketSendEvent) {
        if (event.isCancelled) return

        when (event.packetType) {
            PacketType.Play.Server.ENTITY_VELOCITY -> captureEntityVelocity(event)
            PacketType.Play.Server.BLOCK_BREAK_ANIMATION -> captureBlockBreakAnimation(event)
        }
    }

    private fun captureEntityVelocity(event: PacketSendEvent) {
        val packet = WrapperPlayServerEntityVelocity(event)
        val entityId = packet.entityId
        if (!capture.isEntityVelocityTracked(entityId)) return

        val velocity = packet.velocity.toCaptureVector()
        val tick = capture.currentTick()
        if (!dedupe(tick, VelocityKey(entityId, velocity))) return

        capture.captureEvent(
            CaptureEntityVelocityEvent(
                entityId = entityId,
                velocity = velocity,
            )
        )
    }

    private fun captureBlockBreakAnimation(event: PacketSendEvent) {
        val packet = WrapperPlayServerBlockBreakAnimation(event)
        val entityId = packet.entityId
        if (!capture.isEntityTracked(entityId)) return

        val position = packet.blockPosition
        val tick = capture.currentTick()
        val key = BlockAnimationKey(entityId, position.x, position.y, position.z, packet.destroyStage.toInt())
        if (!dedupe(tick, key)) return

        capture.captureEvent(
            CaptureBlockBreakAnimationEvent(
                animationEntityId = entityId,
                position = CaptureBlockPosition(position.x, position.y, position.z),
                destroyStage = packet.destroyStage.toInt(),
            )
        )
    }

    private fun dedupe(tick: Long, key: VelocityKey): Boolean {
        resetDedupeIfNeeded(tick)
        return velocityKeys.add(key)
    }

    private fun dedupe(tick: Long, key: BlockAnimationKey): Boolean {
        resetDedupeIfNeeded(tick)
        return blockAnimationKeys.add(key)
    }

    private fun resetDedupeIfNeeded(tick: Long) {
        if (tick == dedupeTick) return
        dedupeTick = tick
        velocityKeys.clear()
        blockAnimationKeys.clear()
    }

    private fun com.github.retrooper.packetevents.util.Vector3d.toCaptureVector(): CaptureVector {
        return CaptureVector(x, y, z)
    }

    private data class VelocityKey(
        val entityId: Int,
        val velocity: CaptureVector,
    )

    private data class BlockAnimationKey(
        val entityId: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val destroyStage: Int,
    )
}
