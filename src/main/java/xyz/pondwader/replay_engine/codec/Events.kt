@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package xyz.pondwader.replay_engine.codec

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

const val CAPTURE_REPLAY_FORMAT_VERSION = 2

/**
 * Represents a batch of frames. Used to write a collection of frames together.
 */
@Serializable
data class CaptureFrameBatch(
    @ProtoNumber(1) val frames: List<CaptureFrame> = emptyList(),
)

/**
 * Data structure written at the top of every replay file.
 */
@Serializable
data class CaptureReplayHeader(
    @ProtoNumber(1) val formatVersion: Int = CAPTURE_REPLAY_FORMAT_VERSION,
    @ProtoNumber(2) val sourceWorldName: String,
    @ProtoNumber(3) val mapId: String? = null,
    @ProtoNumber(4) val startedAtMillis: Long,
)

/**
 * A captured frame represents all events that occur in the same tick.
 */
@Serializable
data class CaptureFrame(
    @ProtoNumber(1) val tick: Long,
    @ProtoNumber(2) val events: MutableList<CaptureEvent> = mutableListOf(),
)

@Serializable
data class CaptureEvent(
    @ProtoNumber(1) val type: Int,
    @ProtoNumber(2) val blockChange: CaptureBlockChangeEvent? = null,
    @ProtoNumber(3) val chatMessage: CaptureChatMessageEvent? = null,
    @ProtoNumber(4) val blockDamage: CaptureBlockDamageEvent? = null,
    @ProtoNumber(5) val blockBreakAnimation: CaptureBlockBreakAnimationEvent? = null,
    @ProtoNumber(6) val explosion: CaptureExplosionEvent? = null,
    @ProtoNumber(7) val entitySpawn: CaptureEntitySpawnEvent? = null,
    @ProtoNumber(8) val entityRemove: CaptureEntityRemoveEvent? = null,
    @ProtoNumber(9) val entityMove: CaptureEntityMoveEvent? = null,
    @ProtoNumber(10) val entityVelocity: CaptureEntityVelocityEvent? = null,
    @ProtoNumber(11) val entityDamage: CaptureEntityDamageEvent? = null,
    @ProtoNumber(12) val entityState: CaptureEntityStateEvent? = null,
    @ProtoNumber(13) val entityVisualState: CaptureEntityVisualStateEvent? = null,
    @ProtoNumber(14) val playerAnimation: CapturePlayerAnimationEvent? = null,
    @ProtoNumber(15) val playerHeldItem: CapturePlayerHeldItemEvent? = null,
    @ProtoNumber(16) val playerOffhandItem: CapturePlayerOffhandItemEvent? = null,
)

sealed interface CaptureEventPayload

object CaptureEventTypes {
    const val BLOCK_CHANGE = 1
    const val CHAT_MESSAGE = 2
    const val BLOCK_DAMAGE = 3
    const val BLOCK_BREAK_ANIMATION = 4
    const val EXPLOSION = 5
    const val ENTITY_SPAWN = 6
    const val ENTITY_REMOVE = 7
    const val ENTITY_MOVE = 8
    const val ENTITY_VELOCITY = 9
    const val ENTITY_DAMAGE = 10
    const val ENTITY_STATE = 11
    const val ENTITY_VISUAL_STATE = 12
    const val PLAYER_ANIMATION = 13
    const val PLAYER_HELD_ITEM = 14
    const val PLAYER_OFFHAND_ITEM = 15
}

internal fun CaptureEventPayload.toCaptureEvent(): CaptureEvent {
    return when (this) {
        is CaptureBlockChangeEvent -> CaptureEvent(CaptureEventTypes.BLOCK_CHANGE, blockChange = this)
        is CaptureChatMessageEvent -> CaptureEvent(CaptureEventTypes.CHAT_MESSAGE, chatMessage = this)
        is CaptureBlockDamageEvent -> CaptureEvent(CaptureEventTypes.BLOCK_DAMAGE, blockDamage = this)
        is CaptureBlockBreakAnimationEvent -> CaptureEvent(
            CaptureEventTypes.BLOCK_BREAK_ANIMATION,
            blockBreakAnimation = this
        )

        is CaptureExplosionEvent -> CaptureEvent(CaptureEventTypes.EXPLOSION, explosion = this)
        is CaptureEntitySpawnEvent -> CaptureEvent(CaptureEventTypes.ENTITY_SPAWN, entitySpawn = this)
        is CaptureEntityRemoveEvent -> CaptureEvent(CaptureEventTypes.ENTITY_REMOVE, entityRemove = this)
        is CaptureEntityMoveEvent -> CaptureEvent(CaptureEventTypes.ENTITY_MOVE, entityMove = this)
        is CaptureEntityVelocityEvent -> CaptureEvent(CaptureEventTypes.ENTITY_VELOCITY, entityVelocity = this)
        is CaptureEntityDamageEvent -> CaptureEvent(CaptureEventTypes.ENTITY_DAMAGE, entityDamage = this)
        is CaptureEntityStateEvent -> CaptureEvent(CaptureEventTypes.ENTITY_STATE, entityState = this)
        is CaptureEntityVisualStateEvent -> CaptureEvent(
            CaptureEventTypes.ENTITY_VISUAL_STATE,
            entityVisualState = this
        )

        is CapturePlayerAnimationEvent -> CaptureEvent(CaptureEventTypes.PLAYER_ANIMATION, playerAnimation = this)
        is CapturePlayerHeldItemEvent -> CaptureEvent(CaptureEventTypes.PLAYER_HELD_ITEM, playerHeldItem = this)
        is CapturePlayerOffhandItemEvent -> CaptureEvent(
            CaptureEventTypes.PLAYER_OFFHAND_ITEM,
            playerOffhandItem = this
        )
    }
}

@Serializable
data class CaptureBlockChangeEvent(
    @ProtoNumber(1) val position: CaptureBlockPosition,
    @ProtoNumber(2) val newBlockData: String? = null,
) : CaptureEventPayload

@Serializable
data class CaptureChatMessageEvent(
    @ProtoNumber(1) val message: String,
) : CaptureEventPayload

@Serializable
data class CaptureBlockDamageEvent(
    @ProtoNumber(1) val position: CaptureBlockPosition,
    @ProtoNumber(2) val progress: Float,
    @ProtoNumber(3) val playerUuid: String,
) : CaptureEventPayload

@Serializable
data class CaptureBlockBreakAnimationEvent(
    @ProtoNumber(1) val animationEntityId: Int,
    @ProtoNumber(2) val position: CaptureBlockPosition,
    @ProtoNumber(3) val destroyStage: Int,
) : CaptureEventPayload

@Serializable
data class CaptureExplosionEvent(
    @ProtoNumber(1) val location: CaptureLocation,
) : CaptureEventPayload

@Serializable
data class CaptureEntitySpawnEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val entityType: String,
    @ProtoNumber(3) val location: CaptureLocation,
    @ProtoNumber(4) val velocity: CaptureVector,
    @ProtoNumber(5) val equipment: CaptureEquipment? = null,
    @ProtoNumber(6) val playerState: CapturePlayerState? = null,
    @ProtoNumber(7) val visualState: CaptureVisualState? = null,
) : CaptureEventPayload

@Serializable
data class CaptureEntityRemoveEvent(
    @ProtoNumber(1) val entityId: Int,
) : CaptureEventPayload

@Serializable
data class CaptureEntityMoveEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val to: CaptureLocation,
) : CaptureEventPayload

@Serializable
data class CaptureEntityVelocityEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val velocity: CaptureVector,
) : CaptureEventPayload

@Serializable
data class CaptureEntityDamageEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val yaw: Float,
) : CaptureEventPayload

@Serializable
data class CaptureEntityStateEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val equipment: CaptureEquipment? = null,
    @ProtoNumber(3) val visualState: CaptureVisualState? = null,
) : CaptureEventPayload

@Serializable
data class CaptureEntityVisualStateEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val visualState: CaptureVisualState,
) : CaptureEventPayload

@Serializable
data class CapturePlayerAnimationEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val hand: String? = null,
) : CaptureEventPayload

@Serializable
data class CapturePlayerHeldItemEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val newItem: CaptureItemStack? = null,
) : CaptureEventPayload

@Serializable
data class CapturePlayerOffhandItemEvent(
    @ProtoNumber(1) val entityId: Int,
    @ProtoNumber(2) val newItem: CaptureItemStack? = null,
) : CaptureEventPayload

@Serializable
data class CaptureBlockPosition(
    @ProtoNumber(1) val x: Int,
    @ProtoNumber(2) val y: Int,
    @ProtoNumber(3) val z: Int,
)

@Serializable
data class CaptureLocation(
    @ProtoNumber(1) val x: Double,
    @ProtoNumber(2) val y: Double,
    @ProtoNumber(3) val z: Double,
    @ProtoNumber(4) val yaw: Float = 0f,
    @ProtoNumber(5) val pitch: Float = 0f,
)

@Serializable
data class CaptureVector(
    @ProtoNumber(1) val x: Double,
    @ProtoNumber(2) val y: Double,
    @ProtoNumber(3) val z: Double,
)

@Serializable
data class CaptureItemStack(
    @ProtoNumber(1) val serializedBytes: ByteArray,
)

@Serializable
data class CaptureEquipment(
    @ProtoNumber(1) val mainHand: CaptureItemStack? = null,
    @ProtoNumber(2) val offHand: CaptureItemStack? = null,
    @ProtoNumber(3) val helmet: CaptureItemStack? = null,
    @ProtoNumber(4) val chestplate: CaptureItemStack? = null,
    @ProtoNumber(5) val leggings: CaptureItemStack? = null,
    @ProtoNumber(6) val boots: CaptureItemStack? = null,
)

@Serializable
data class CapturePlayerState(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val gameMode: String,
    @ProtoNumber(3) val textureProperties: List<CaptureTextureProperty> = emptyList(),
)

@Serializable
data class CaptureTextureProperty(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val value: String,
    @ProtoNumber(3) val signature: String? = null,
)

@Serializable
data class CaptureVisualState(
    @ProtoNumber(1) val sneaking: Boolean = false,
    @ProtoNumber(2) val sprinting: Boolean = false,
    @ProtoNumber(3) val swimming: Boolean = false,
    @ProtoNumber(4) val gliding: Boolean = false,
    @ProtoNumber(5) val invisible: Boolean = false,
    @ProtoNumber(6) val glowing: Boolean = false,
    @ProtoNumber(7) val onFire: Boolean = false,
)
