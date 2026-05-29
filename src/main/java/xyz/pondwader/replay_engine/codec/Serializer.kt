package xyz.pondwader.replay_engine.codec

import com.github.luben.zstd.ZstdOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream

class Serializer(out: OutputStream) : DataOutputStream(out) {
    companion object {
        const val ZSTD_LEVEL = 4
    }

    private val bufferedOut = ByteArrayOutputStream()

    fun writeHeader(header: ReplayHeader) {
        writeInt(header.formatVersion)
        writeString(header.sourceWorldName)
        writeNullableString(header.mapId)
        writeLong(header.startedAtMillis)

        writeInt(header.metadata.size)
        for ((key, value) in header.metadata) {
            writeString(key)
            writeString(value)
        }
    }

    fun writeFrames(frames: List<Frame>) {
        if (frames.isEmpty()) return
        val startTick = frames.first().tick
        val endTick = frames.last().tick

        val compressedBuffer = bufferedOut
        compressedBuffer.reset()

        ZstdOutputStream(compressedBuffer, ZSTD_LEVEL).use { compressedOutput ->
            val compressedSerializer = Serializer(compressedOutput)
            for (frame in frames) {
                compressedSerializer.writeFrame(frame)
            }
        }

        writeLong(startTick)
        writeLong(endTick)
        writeInt(frames.size)
        writeInt(compressedBuffer.size())
        compressedBuffer.writeTo(this)
    }

    private fun writeFrame(frame: Frame) {
        writeLong(frame.tick)
        writeInt(frame.events.size)
        for (event in frame.events) {
            writeByte(event.typeId)
            event.write(this)
        }
    }

    fun writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    fun writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeString(value)
    }

    fun writeByteArray(value: ByteArray) {
        writeInt(value.size)
        write(value)
    }

    fun writeBlockPosition(value: CaptureBlockPosition) {
        writeInt(value.x)
        writeInt(value.y)
        writeInt(value.z)
    }

    fun writeLocation(value: CaptureLocation) {
        writeDouble(value.x)
        writeDouble(value.y)
        writeDouble(value.z)
        writeFloat(value.yaw)
        writeFloat(value.pitch)
    }

    fun writeVector(value: CaptureVector) {
        writeDouble(value.x)
        writeDouble(value.y)
        writeDouble(value.z)
    }

    fun writeNullableItemStack(value: CaptureItemStack?) {
        writeBoolean(value != null)
        if (value != null) writeByteArray(value.serializedBytes)
    }

    fun writeNullableEquipment(value: CaptureEquipment?) {
        writeBoolean(value != null)
        if (value == null) return

        writeNullableItemStack(value.mainHand)
        writeNullableItemStack(value.offHand)
        writeNullableItemStack(value.helmet)
        writeNullableItemStack(value.chestplate)
        writeNullableItemStack(value.leggings)
        writeNullableItemStack(value.boots)
    }

    fun writeNullablePlayerState(value: CapturePlayerState?) {
        writeBoolean(value != null)
        if (value == null) return

        writeString(value.name)
        writeString(value.gameMode)
        writeInt(value.textureProperties.size)
        for (property in value.textureProperties) {
            writeString(property.name)
            writeString(property.value)
            writeNullableString(property.signature)
        }
    }

    fun writeVisualState(value: CaptureVisualState) {
        writeBoolean(value.sneaking)
        writeBoolean(value.sprinting)
        writeBoolean(value.swimming)
        writeBoolean(value.gliding)
        writeBoolean(value.invisible)
        writeBoolean(value.glowing)
        writeBoolean(value.onFire)
    }

    fun writeNullableVisualState(value: CaptureVisualState?) {
        writeBoolean(value != null)
        if (value != null) writeVisualState(value)
    }
}
