package xyz.pondwader.replay_engine.capture

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity
import xyz.pondwader.replay_engine.codec.CaptureEntityVelocityEvent
import xyz.pondwader.replay_engine.codec.CaptureVector

internal class PacketEventsCaptureListener(private val capture: GameCapture) :
    PacketListenerAbstract(PacketListenerPriority.MONITOR) {
    private var dedupeTick = -1L
    private val velocityKeys = HashSet<VelocityKey>()

    override fun onPacketSend(event: PacketSendEvent) {
        if (event.isCancelled) return

        when (event.packetType) {
            PacketType.Play.Server.ENTITY_VELOCITY -> captureEntityVelocity(event)
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

    private fun dedupe(tick: Long, key: VelocityKey): Boolean {
        resetDedupeIfNeeded(tick)
        return velocityKeys.add(key)
    }

    private fun resetDedupeIfNeeded(tick: Long) {
        if (tick == dedupeTick) return
        dedupeTick = tick
        velocityKeys.clear()
    }

    private fun com.github.retrooper.packetevents.util.Vector3d.toCaptureVector(): CaptureVector {
        return CaptureVector(x, y, z)
    }

    private data class VelocityKey(
        val entityId: Int,
        val velocity: CaptureVector,
    )
}
