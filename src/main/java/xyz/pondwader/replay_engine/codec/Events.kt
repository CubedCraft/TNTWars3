package xyz.pondwader.replay_engine.codec

sealed interface CaptureEventPayload {
    val typeId: Int

    fun write(serializer: Serializer)
}

object CaptureEventTypes {
    const val BLOCK_CHANGE = 1
    const val CHAT_MESSAGE = 2
    const val BLOCK_DAMAGE = 3
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

data class CaptureBlockChangeEvent(
    val position: CaptureBlockPosition,
    val newBlockData: String,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.BLOCK_CHANGE

    companion object {
        fun read(deserializer: Deserializer): CaptureBlockChangeEvent {
            return CaptureBlockChangeEvent(
                position = deserializer.readBlockPosition(),
                newBlockData = deserializer.readString(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeBlockPosition(position)
        serializer.writeString(newBlockData)
    }
}

data class CaptureChatMessageEvent(
    val message: String,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.CHAT_MESSAGE

    companion object {
        fun read(deserializer: Deserializer): CaptureChatMessageEvent {
            return CaptureChatMessageEvent(deserializer.readString())
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeString(message)
    }
}

data class CaptureBlockDamageEvent(
    val position: CaptureBlockPosition,
    val progress: Float,
    val playerUuid: String,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.BLOCK_DAMAGE

    companion object {
        fun read(deserializer: Deserializer): CaptureBlockDamageEvent {
            return CaptureBlockDamageEvent(
                position = deserializer.readBlockPosition(),
                progress = deserializer.readFloat(),
                playerUuid = deserializer.readString(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeBlockPosition(position)
        serializer.writeFloat(progress)
        serializer.writeString(playerUuid)
    }
}

data class CaptureExplosionEvent(
    val location: CaptureLocation,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.EXPLOSION

    companion object {
        fun read(deserializer: Deserializer): CaptureExplosionEvent {
            return CaptureExplosionEvent(deserializer.readLocation())
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeLocation(location)
    }
}

data class CaptureEntitySpawnEvent(
    val entityId: Int,
    val entityType: String,
    val location: CaptureLocation,
    val velocity: CaptureVector,
    val equipment: CaptureEquipment? = null,
    val playerState: CapturePlayerState? = null,
    val visualState: CaptureVisualState? = null,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.ENTITY_SPAWN

    companion object {
        fun read(deserializer: Deserializer): CaptureEntitySpawnEvent {
            return CaptureEntitySpawnEvent(
                entityId = deserializer.readInt(),
                entityType = deserializer.readString(),
                location = deserializer.readLocation(),
                velocity = deserializer.readVector(),
                equipment = deserializer.readNullableEquipment(),
                playerState = deserializer.readNullablePlayerState(),
                visualState = deserializer.readNullableVisualState(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeString(entityType)
        serializer.writeLocation(location)
        serializer.writeVector(velocity)
        serializer.writeNullableEquipment(equipment)
        serializer.writeNullablePlayerState(playerState)
        serializer.writeNullableVisualState(visualState)
    }
}

data class CaptureEntityRemoveEvent(
    val entityId: Int,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.ENTITY_REMOVE

    companion object {
        fun read(deserializer: Deserializer): CaptureEntityRemoveEvent {
            return CaptureEntityRemoveEvent(deserializer.readInt())
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
    }
}

data class CaptureEntityMoveEvent(
    val entityId: Int,
    val to: CaptureLocation,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.ENTITY_MOVE

    companion object {
        fun read(deserializer: Deserializer): CaptureEntityMoveEvent {
            return CaptureEntityMoveEvent(
                entityId = deserializer.readInt(),
                to = deserializer.readLocation(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeLocation(to)
    }
}

data class CaptureEntityVelocityEvent(
    val entityId: Int,
    val velocity: CaptureVector,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.ENTITY_VELOCITY

    companion object {
        fun read(deserializer: Deserializer): CaptureEntityVelocityEvent {
            return CaptureEntityVelocityEvent(
                entityId = deserializer.readInt(),
                velocity = deserializer.readVector(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeVector(velocity)
    }
}

data class CaptureEntityDamageEvent(
    val entityId: Int,
    val yaw: Float,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.ENTITY_DAMAGE

    companion object {
        fun read(deserializer: Deserializer): CaptureEntityDamageEvent {
            return CaptureEntityDamageEvent(
                entityId = deserializer.readInt(),
                yaw = deserializer.readFloat(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeFloat(yaw)
    }
}

data class CaptureEntityStateEvent(
    val entityId: Int,
    val equipment: CaptureEquipment? = null,
    val visualState: CaptureVisualState? = null,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.ENTITY_STATE

    companion object {
        fun read(deserializer: Deserializer): CaptureEntityStateEvent {
            return CaptureEntityStateEvent(
                entityId = deserializer.readInt(),
                equipment = deserializer.readNullableEquipment(),
                visualState = deserializer.readNullableVisualState(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeNullableEquipment(equipment)
        serializer.writeNullableVisualState(visualState)
    }
}

data class CaptureEntityVisualStateEvent(
    val entityId: Int,
    val visualState: CaptureVisualState,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.ENTITY_VISUAL_STATE

    companion object {
        fun read(deserializer: Deserializer): CaptureEntityVisualStateEvent {
            return CaptureEntityVisualStateEvent(
                entityId = deserializer.readInt(),
                visualState = deserializer.readVisualState(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeVisualState(visualState)
    }
}

data class CapturePlayerAnimationEvent(
    val entityId: Int,
    val hand: String? = null,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.PLAYER_ANIMATION

    companion object {
        fun read(deserializer: Deserializer): CapturePlayerAnimationEvent {
            return CapturePlayerAnimationEvent(
                entityId = deserializer.readInt(),
                hand = deserializer.readNullableString(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeNullableString(hand)
    }
}

data class CapturePlayerHeldItemEvent(
    val entityId: Int,
    val newItem: CaptureItemStack? = null,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.PLAYER_HELD_ITEM

    companion object {
        fun read(deserializer: Deserializer): CapturePlayerHeldItemEvent {
            return CapturePlayerHeldItemEvent(
                entityId = deserializer.readInt(),
                newItem = deserializer.readNullableItemStack(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeNullableItemStack(newItem)
    }
}

data class CapturePlayerOffhandItemEvent(
    val entityId: Int,
    val newItem: CaptureItemStack? = null,
) : CaptureEventPayload {
    override val typeId = CaptureEventTypes.PLAYER_OFFHAND_ITEM

    companion object {
        fun read(deserializer: Deserializer): CapturePlayerOffhandItemEvent {
            return CapturePlayerOffhandItemEvent(
                entityId = deserializer.readInt(),
                newItem = deserializer.readNullableItemStack(),
            )
        }
    }

    override fun write(serializer: Serializer) {
        serializer.writeInt(entityId)
        serializer.writeNullableItemStack(newItem)
    }
}

data class CaptureBlockPosition(
    val x: Int,
    val y: Int,
    val z: Int,
)

data class CaptureLocation(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
)

data class CaptureVector(
    val x: Double,
    val y: Double,
    val z: Double,
)

data class CaptureItemStack(
    val serializedBytes: ByteArray,
)

data class CaptureEquipment(
    val mainHand: CaptureItemStack? = null,
    val offHand: CaptureItemStack? = null,
    val helmet: CaptureItemStack? = null,
    val chestplate: CaptureItemStack? = null,
    val leggings: CaptureItemStack? = null,
    val boots: CaptureItemStack? = null,
)

data class CapturePlayerState(
    val name: String,
    val gameMode: String,
    val textureProperties: List<CaptureTextureProperty> = emptyList(),
)

data class CaptureTextureProperty(
    val name: String,
    val value: String,
    val signature: String? = null,
)

data class CaptureVisualState(
    val sneaking: Boolean = false,
    val sprinting: Boolean = false,
    val swimming: Boolean = false,
    val gliding: Boolean = false,
    val invisible: Boolean = false,
    val glowing: Boolean = false,
    val onFire: Boolean = false,
)
