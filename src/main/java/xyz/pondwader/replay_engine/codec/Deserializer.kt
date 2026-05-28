package xyz.pondwader.replay_engine.codec

import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

class Deserializer(input: InputStream) : DataInputStream(input) {
    fun readHeader(): ReplayHeader {
        val formatVersion = readInt()
        val sourceWorldName = readString()
        val mapId = readNullableString()
        val startedAtMillis = readLong()

        val metadataSize = readInt()
        val metadata = LinkedHashMap<String, String>(metadataSize)
        repeat(metadataSize) {
            metadata[readString()] = readString()
        }

        return ReplayHeader(
            formatVersion = formatVersion,
            sourceWorldName = sourceWorldName,
            mapId = mapId,
            startedAtMillis = startedAtMillis,
            metadata = metadata,
        )
    }

    fun readFrameBatch(): FrameBatch? {
        val startTick = try {
            readLong()
        } catch (_: EOFException) {
            return null
        }
        val endTick = readLong()
        val frameCount = readInt()
        val compressedBytes = readByteArray()
        val frames = ArrayList<Frame>(frameCount)

        Deserializer(ZstdInputStream(ByteArrayInputStream(compressedBytes))).use { compressedDeserializer ->
            repeat(frameCount) {
                frames.add(compressedDeserializer.readFrame())
            }
        }

        return FrameBatch(startTick, endTick, frames)
    }

    private fun readFrame(): Frame {
        val tick = readLong()
        val events = mutableListOf<CaptureEventPayload>()
        repeat(readInt()) {
            events.add(readEventPayload(readByte().toInt()))
        }
        return Frame(tick, events)
    }

    private fun readEventPayload(typeId: Int): CaptureEventPayload {
        return when (typeId) {
            CaptureEventTypes.BLOCK_CHANGE -> CaptureBlockChangeEvent.read(this)
            CaptureEventTypes.CHAT_MESSAGE -> CaptureChatMessageEvent.read(this)
            CaptureEventTypes.BLOCK_DAMAGE -> CaptureBlockDamageEvent.read(this)
            CaptureEventTypes.EXPLOSION -> CaptureExplosionEvent.read(this)
            CaptureEventTypes.ENTITY_SPAWN -> CaptureEntitySpawnEvent.read(this)
            CaptureEventTypes.ENTITY_REMOVE -> CaptureEntityRemoveEvent.read(this)
            CaptureEventTypes.ENTITY_MOVE -> CaptureEntityMoveEvent.read(this)
            CaptureEventTypes.ENTITY_VELOCITY -> CaptureEntityVelocityEvent.read(this)
            CaptureEventTypes.ENTITY_DAMAGE -> CaptureEntityDamageEvent.read(this)
            CaptureEventTypes.ENTITY_STATE -> CaptureEntityStateEvent.read(this)
            CaptureEventTypes.ENTITY_VISUAL_STATE -> CaptureEntityVisualStateEvent.read(this)
            CaptureEventTypes.PLAYER_ANIMATION -> CapturePlayerAnimationEvent.read(this)
            CaptureEventTypes.PLAYER_HELD_ITEM -> CapturePlayerHeldItemEvent.read(this)
            CaptureEventTypes.PLAYER_OFFHAND_ITEM -> CapturePlayerOffhandItemEvent.read(this)
            else -> throw IllegalArgumentException("Unknown capture event type $typeId")
        }
    }

    fun readString(): String {
        return readByteArray().toString(Charsets.UTF_8)
    }

    fun readNullableString(): String? {
        if (!readBoolean()) return null
        return readString()
    }

    fun readByteArray(): ByteArray {
        val bytes = ByteArray(readInt())
        readFully(bytes)
        return bytes
    }

    fun readBlockPosition(): CaptureBlockPosition {
        return CaptureBlockPosition(readInt(), readInt(), readInt())
    }

    fun readLocation(): CaptureLocation {
        return CaptureLocation(
            x = readDouble(),
            y = readDouble(),
            z = readDouble(),
            yaw = readFloat(),
            pitch = readFloat(),
        )
    }

    fun readVector(): CaptureVector {
        return CaptureVector(readDouble(), readDouble(), readDouble())
    }

    fun readNullableItemStack(): CaptureItemStack? {
        if (!readBoolean()) return null
        return CaptureItemStack(readByteArray())
    }

    fun readNullableEquipment(): CaptureEquipment? {
        if (!readBoolean()) return null
        return CaptureEquipment(
            mainHand = readNullableItemStack(),
            offHand = readNullableItemStack(),
            helmet = readNullableItemStack(),
            chestplate = readNullableItemStack(),
            leggings = readNullableItemStack(),
            boots = readNullableItemStack(),
        )
    }

    fun readNullablePlayerState(): CapturePlayerState? {
        if (!readBoolean()) return null

        val name = readString()
        val gameMode = readString()
        val textureProperties = ArrayList<CaptureTextureProperty>()
        repeat(readInt()) {
            textureProperties.add(
                CaptureTextureProperty(
                    name = readString(),
                    value = readString(),
                    signature = readNullableString(),
                )
            )
        }

        return CapturePlayerState(name, gameMode, textureProperties)
    }

    fun readVisualState(): CaptureVisualState {
        return CaptureVisualState(
            sneaking = readBoolean(),
            sprinting = readBoolean(),
            swimming = readBoolean(),
            gliding = readBoolean(),
            invisible = readBoolean(),
            glowing = readBoolean(),
            onFire = readBoolean(),
        )
    }

    fun readNullableVisualState(): CaptureVisualState? {
        if (!readBoolean()) return null
        return readVisualState()
    }
}
