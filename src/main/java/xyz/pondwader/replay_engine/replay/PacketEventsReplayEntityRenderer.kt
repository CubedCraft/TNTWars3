package xyz.pondwader.replay_engine.replay

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.entity.Player
import xyz.pondwader.replay_engine.codec.CaptureEntityDamageEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityMoveEvent
import xyz.pondwader.replay_engine.codec.CaptureEntitySpawnEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityStateEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityVelocityEvent
import xyz.pondwader.replay_engine.codec.CaptureEntityVisualStateEvent
import xyz.pondwader.replay_engine.codec.CaptureEquipment
import xyz.pondwader.replay_engine.codec.CaptureItemStack
import xyz.pondwader.replay_engine.codec.CaptureLocation
import xyz.pondwader.replay_engine.codec.CapturePlayerAnimationEvent
import xyz.pondwader.replay_engine.codec.CapturePlayerHeldItemEvent
import xyz.pondwader.replay_engine.codec.CapturePlayerOffhandItemEvent
import xyz.pondwader.replay_engine.codec.CapturePlayerState
import xyz.pondwader.replay_engine.codec.CaptureVector
import xyz.pondwader.replay_engine.codec.CaptureVisualState
import org.bukkit.inventory.ItemStack as BukkitItemStack
import java.util.EnumSet
import java.util.Optional
import java.util.UUID

internal class PacketEventsReplayEntityRenderer(private val viewer: Player) {
    private val playerManager = PacketEvents.getAPI().playerManager
    private val virtualEntities = HashMap<Int, VirtualEntity>()
    private var nextReplayEntityId = REPLAY_ENTITY_ID_START

    fun spawn(event: CaptureEntitySpawnEvent) {
        if (virtualEntities.containsKey(event.entityId)) return

        val entityType = getEntityType(event.entityType) ?: return
        val virtualEntity = VirtualEntity(
            replayId = nextReplayEntityId++,
            recordedId = event.entityId,
            uuid = replayUuid(event.entityId),
            type = entityType,
            name = event.playerState?.name,
            isPlayer = event.entityType == org.bukkit.entity.EntityType.PLAYER.key.toString(),
        )

        if (virtualEntity.isPlayer) {
            sendPlayerInfo(virtualEntity, event.playerState)
        }

        send(
            WrapperPlayServerSpawnEntity(
                virtualEntity.replayId,
                Optional.of(virtualEntity.uuid),
                entityType,
                event.location.toPacketVector(),
                event.location.pitch,
                event.location.yaw,
                event.location.yaw,
                0,
                Optional.of(event.velocity.toPacketVector()),
            )
        )

        send(WrapperPlayServerEntityHeadLook(virtualEntity.replayId, event.location.yaw))
        send(WrapperPlayServerEntityVelocity(virtualEntity.replayId, event.velocity.toPacketVector()))
        event.equipment?.let { sendEquipment(virtualEntity.replayId, it, includeEmpty = false) }
        event.visualState?.let { sendVisualState(virtualEntity.replayId, it) }

        virtualEntities[event.entityId] = virtualEntity
    }

    fun remove(entityId: Int) {
        val virtualEntity = virtualEntities.remove(entityId) ?: return
        send(WrapperPlayServerDestroyEntities(virtualEntity.replayId))

        if (virtualEntity.isPlayer) {
            send(WrapperPlayServerPlayerInfoRemove(virtualEntity.uuid))
        }
    }

    fun move(event: CaptureEntityMoveEvent) {
        val virtualEntity = virtualEntities[event.entityId] ?: return
        sendTeleport(virtualEntity.replayId, event.to)
    }

    fun state(event: CaptureEntityStateEvent) {
        val virtualEntity = virtualEntities[event.entityId] ?: return
        event.equipment?.let { sendEquipment(virtualEntity.replayId, it, includeEmpty = true) }
        event.visualState?.let { sendVisualState(virtualEntity.replayId, it) }
    }

    fun visualState(event: CaptureEntityVisualStateEvent) {
        val virtualEntity = virtualEntities[event.entityId] ?: return
        sendVisualState(virtualEntity.replayId, event.visualState)
    }

    fun velocity(event: CaptureEntityVelocityEvent) {
        val virtualEntity = virtualEntities[event.entityId] ?: return
        send(WrapperPlayServerEntityVelocity(virtualEntity.replayId, event.velocity.toPacketVector()))
    }

    fun animation(event: CapturePlayerAnimationEvent) {
        val virtualEntity = virtualEntities[event.entityId] ?: return
        send(WrapperPlayServerEntityAnimation(virtualEntity.replayId, animationType(event)))
    }

    fun heldItem(event: CapturePlayerHeldItemEvent) {
        val virtualEntity = virtualEntities[event.entityId] ?: return
        send(
            WrapperPlayServerEntityEquipment(
                virtualEntity.replayId, listOf(
                    Equipment(EquipmentSlot.MAIN_HAND, event.newItem.toPacketItemStack()),
                )
            )
        )
    }

    fun offhandItem(event: CapturePlayerOffhandItemEvent) {
        val virtualEntity = virtualEntities[event.entityId] ?: return
        send(
            WrapperPlayServerEntityEquipment(
                virtualEntity.replayId, listOf(
                    Equipment(EquipmentSlot.OFF_HAND, event.newItem.toPacketItemStack()),
                )
            )
        )
    }

    fun damage(event: CaptureEntityDamageEvent) {
        val virtualEntity = virtualEntities[event.entityId] ?: return
        send(WrapperPlayServerHurtAnimation(virtualEntity.replayId, event.yaw))
        send(WrapperPlayServerEntityStatus(virtualEntity.replayId, HURT_ENTITY_STATUS))
    }

    fun clear() {
        for (entityId in virtualEntities.keys.toList()) {
            remove(entityId)
        }
    }

    private fun sendPlayerInfo(virtualEntity: VirtualEntity, playerState: CapturePlayerState?) {
        val name = (virtualEntity.name ?: "Replay${virtualEntity.recordedId}").take(16)
        val textureProperties = playerState?.textureProperties.orEmpty().map {
            TextureProperty(it.name, it.value, it.signature)
        }
        val profile = UserProfile(virtualEntity.uuid, name, textureProperties)
        val gameMode = playerState?.gameMode
            ?.let { runCatching { GameMode.valueOf(it) }.getOrNull() }
            ?: GameMode.ADVENTURE
        val info = WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
            profile,
            true,
            0,
            gameMode,
            replayPlayerDisplayName(name),
            null,
        )

        send(
            WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(
                    WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY,
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME,
                ),
                listOf(info),
            )
        )
    }

    private fun sendTeleport(entityId: Int, location: CaptureLocation) {
        send(
            WrapperPlayServerEntityTeleport(
                entityId,
                location.toPacketVector(),
                location.yaw,
                location.pitch,
                false,
            )
        )
        send(WrapperPlayServerEntityHeadLook(entityId, location.yaw))
    }

    private fun sendEquipment(entityId: Int, equipment: CaptureEquipment, includeEmpty: Boolean) {
        val packetEquipment = mutableListOf<Equipment>()
        addEquipment(packetEquipment, EquipmentSlot.MAIN_HAND, equipment.mainHand, includeEmpty)
        addEquipment(packetEquipment, EquipmentSlot.OFF_HAND, equipment.offHand, includeEmpty)
        addEquipment(packetEquipment, EquipmentSlot.HELMET, equipment.helmet, includeEmpty)
        addEquipment(packetEquipment, EquipmentSlot.CHEST_PLATE, equipment.chestplate, includeEmpty)
        addEquipment(packetEquipment, EquipmentSlot.LEGGINGS, equipment.leggings, includeEmpty)
        addEquipment(packetEquipment, EquipmentSlot.BOOTS, equipment.boots, includeEmpty)
        if (packetEquipment.isEmpty()) return

        send(WrapperPlayServerEntityEquipment(entityId, packetEquipment))
    }

    private fun addEquipment(
        packetEquipment: MutableList<Equipment>,
        slot: EquipmentSlot,
        item: CaptureItemStack?,
        includeEmpty: Boolean,
    ) {
        if (item == null && !includeEmpty) return
        packetEquipment += Equipment(slot, item.toPacketItemStack())
    }

    private fun sendVisualState(entityId: Int, visualState: CaptureVisualState) {
        val flags = entityFlags(visualState)
        val pose = entityPose(visualState)
        send(
            WrapperPlayServerEntityMetadata(
                entityId, listOf(
                    EntityData(0, EntityDataTypes.BYTE, flags),
                    EntityData(6, EntityDataTypes.ENTITY_POSE, pose),
                )
            )
        )
    }

    private fun send(packet: PacketWrapper<*>) {
        playerManager.sendPacket(viewer, packet)
    }

    private fun getEntityType(key: String): EntityType? {
        val namespacedKey = NamespacedKey.fromString(key) ?: return null
        val bukkitType = Registry.ENTITY_TYPE.get(namespacedKey) ?: return null
        return SpigotConversionUtil.fromBukkitEntityType(bukkitType)
    }

    private fun replayUuid(recordedId: Int): UUID {
        return UUID.nameUUIDFromBytes("tntwars-replay-$recordedId".toByteArray(Charsets.UTF_8))
    }

    private fun replayPlayerDisplayName(name: String): Component {
        return Component.text(name)
            .append(
                Component.text(" (REPLAY PLAYER)")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.ITALIC)
            )
    }

    private fun CaptureLocation.toPacketVector(): Vector3d {
        return Vector3d(x, y, z)
    }

    private fun CaptureVector.toPacketVector(): Vector3d {
        return Vector3d(x, y, z)
    }

    private fun CaptureItemStack?.toPacketItemStack(): PacketItemStack {
        if (this == null) return PacketItemStack.EMPTY
        return runCatching { SpigotConversionUtil.fromBukkitItemStack(BukkitItemStack.deserializeBytes(serializedBytes)) }
            .getOrDefault(PacketItemStack.EMPTY)
    }

    private fun animationType(event: CapturePlayerAnimationEvent): WrapperPlayServerEntityAnimation.EntityAnimationType {
        if (event.hand == "OFF_HAND") return WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND
        return WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
    }

    private fun entityFlags(visualState: CaptureVisualState): Byte {
        var flags = 0
        if (visualState.onFire) flags = flags or 0x01
        if (visualState.sneaking) flags = flags or 0x02
        if (visualState.sprinting) flags = flags or 0x08
        if (visualState.swimming) flags = flags or 0x10
        if (visualState.invisible) flags = flags or 0x20
        if (visualState.glowing) flags = flags or 0x40
        if (visualState.gliding) flags = flags or 0x80
        return flags.toByte()
    }

    private fun entityPose(visualState: CaptureVisualState): EntityPose {
        return when {
            visualState.gliding -> EntityPose.FALL_FLYING
            visualState.swimming -> EntityPose.SWIMMING
            visualState.sneaking -> EntityPose.CROUCHING
            else -> EntityPose.STANDING
        }
    }

    private data class VirtualEntity(
        val replayId: Int,
        val recordedId: Int,
        val uuid: UUID,
        val type: EntityType,
        val name: String?,
        val isPlayer: Boolean,
    )

    private companion object {
        const val REPLAY_ENTITY_ID_START = 1_000_000
        const val HURT_ENTITY_STATUS = 2
    }
}
