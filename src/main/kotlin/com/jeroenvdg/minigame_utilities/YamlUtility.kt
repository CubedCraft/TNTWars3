package com.jeroenvdg.minigame_utilities

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.util.NumberConversions


class LocationLike {
    val x: Double
    val y: Double
    val z: Double
    val pitch: Float
    val yaw: Float

    constructor(x: Double, y: Double, z: Double, pitch: Float, yaw: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.pitch = pitch
        this.yaw = yaw
    }


    constructor(location: Location) {
        x = location.x
        y = location.y
        z = location.z
        pitch = location.pitch
        yaw = location.yaw
    }


    fun toLocation(world: World): Location {
        return Location(world, x, y, z, yaw, pitch)
    }


    fun clone(): LocationLike {
        return LocationLike(x,y,z, pitch, yaw)
    }
}


fun ConfigurationSection.setLocationLike(key: String, location: LocationLike?) {
    this.set("$key.x", location?.x)
    this.set("$key.y", location?.y)
    this.set("$key.z", location?.z)
    this.set("$key.pitch", location?.pitch)
    this.set("$key.yaw", location?.yaw)
}


fun ConfigurationSection.getLocationLike(key: String): LocationLike? {
    if (!this.isSet(key)) return null
    val x = this.getDouble("$key.x")
    val y = this.getDouble("$key.y")
    val z = this.getDouble("$key.z")
    val pitch = NumberConversions.toFloat(this.getDouble("$key.pitch"))
    val yaw = NumberConversions.toFloat(this.getDouble("$key.yaw"))
    return LocationLike(x, y, z, pitch, yaw)
}


fun ConfigurationSection.setCuboidRegion(key: String, region: Region?) {
    val min = region?.boundingBox?.minimumPoint
    val max = region?.boundingBox?.maximumPoint

    this.set("$key.min.x", min?.x)
    this.set("$key.min.y", min?.y)
    this.set("$key.min.z", min?.z)

    this.set("$key.max.x", max?.x)
    this.set("$key.max.y", max?.y)
    this.set("$key.max.z", max?.z)
}


fun ConfigurationSection.getCuboidRegion(key: String): CuboidRegion? {
    if (!this.contains(key)) return null
    val minX = this.getInt("$key.min.x")
    val minY = this.getInt("$key.min.y")
    val minZ = this.getInt("$key.min.z")
    val maxX = this.getInt("$key.max.x")
    val maxY = this.getInt("$key.max.y")
    val maxZ = this.getInt("$key.max.z")
    return CuboidRegion(BlockVector3.at(minX, minY, minZ), BlockVector3.at(maxX, maxY, maxZ))
}

fun ConfigurationSection.clear() {
    for (key in getKeys(false)) {
        this.set(key, null)
    }
}


inline fun <reified T : Enum<T>> parseEnum(name: String): T? {
    return try {
        java.lang.Enum.valueOf(T::class.java, name)
    } catch (e: IllegalArgumentException) {
        null
    }
}


inline fun <reified T : Enum<T>> parseEnum(name: String, default: T): T {
    return try {
        java.lang.Enum.valueOf(T::class.java, name)
    } catch (e: IllegalArgumentException) {
        default
    }
}
