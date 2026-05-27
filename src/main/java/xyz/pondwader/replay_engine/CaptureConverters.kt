package xyz.pondwader.replay_engine

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

internal fun Location.toCaptureLocation(): CaptureLocation {
    return CaptureLocation(x, y, z, yaw, pitch)
}

internal fun Vector.toCaptureVector(): CaptureVector {
    return CaptureVector(x, y, z)
}

internal fun Block.toCaptureBlockPosition(): CaptureBlockPosition {
    return CaptureBlockPosition(x, y, z)
}

internal fun ItemStack?.toCaptureItemStack(): CaptureItemStack? {
    if (this == null || isEmpty) return null
    return CaptureItemStack(serializeAsBytes())
}

internal fun LivingEntity.toCaptureEquipment(): CaptureEquipment? {
    val equipment = equipment ?: return null
    return CaptureEquipment(
        mainHand = equipment.itemInMainHand.toCaptureItemStack(),
        offHand = equipment.itemInOffHand.toCaptureItemStack(),
        helmet = equipment.helmet.toCaptureItemStack(),
        chestplate = equipment.chestplate.toCaptureItemStack(),
        leggings = equipment.leggings.toCaptureItemStack(),
        boots = equipment.boots.toCaptureItemStack(),
    )
}

internal fun Player.toCapturePlayerState(): CapturePlayerState {
    val textureProperties = playerProfile.properties
        .filter { it.name == "textures" }
        .map { CaptureTextureProperty(it.name, it.value, it.signature) }

    return CapturePlayerState(
        name = name,
        gameMode = gameMode.name,
        textureProperties = textureProperties,
    )
}

internal fun Entity.toCaptureVisualState(): CaptureVisualState {
    val living = this as? LivingEntity
    val player = this as? Player

    return CaptureVisualState(
        sneaking = player?.isSneaking ?: false,
        sprinting = player?.isSprinting ?: false,
        swimming = living?.isSwimming ?: false,
        gliding = living?.isGliding ?: false,
        invisible = isInvisible,
        glowing = isGlowing,
        onFire = fireTicks > 0,
    )
}

internal fun Entity.toCaptureVisualStateEvent(): CaptureEntityVisualStateEvent {
    return CaptureEntityVisualStateEvent(
        entityId = entityId,
        visualState = toCaptureVisualState(),
    )
}

internal fun Entity.toCaptureSpawnEvent(): CaptureEntitySpawnEvent {
    val living = this as? LivingEntity
    val player = this as? Player

    return CaptureEntitySpawnEvent(
        entityId = entityId,
        entityType = type.key.toString(),
        location = location.toCaptureLocation(),
        velocity = velocity.toCaptureVector(),
        equipment = living?.toCaptureEquipment(),
        playerState = player?.toCapturePlayerState(),
        visualState = toCaptureVisualState(),
    )
}

internal fun Entity.toCaptureRemoveEvent(): CaptureEntityRemoveEvent {
    return CaptureEntityRemoveEvent(
        entityId = entityId,
    )
}

internal fun Entity.toCaptureStateEvent(): CaptureEntityStateEvent {
    val living = this as? LivingEntity
    val player = this as? Player

    return CaptureEntityStateEvent(
        entityId = entityId,
        equipment = living?.toCaptureEquipment(),
        visualState = toCaptureVisualState(),
    )
}
