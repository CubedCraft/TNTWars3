package com.jeroenvdg.minigame_utilities

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Location
import org.bukkit.util.BlockVector
import org.bukkit.util.Vector

fun CuboidRegion.intersects(vector: Vector): Boolean {
    val min = this.minimumPoint
    val max = this.maximumPoint

    if (vector.x < min.x) return false
    if (vector.y < min.y) return false
    if (vector.z < min.z) return false
    if (vector.x > max.x) return false
    if (vector.y > max.y) return false
    if (vector.z > max.z) return false
    return true
}

fun CuboidRegion.intersects(location: Location): Boolean {
    val min = this.minimumPoint
    val max = this.maximumPoint

    if (location.x < min.x) return false
    if (location.y < min.y) return false
    if (location.z < min.z) return false
    if (location.x > max.x) return false
    if (location.y > max.y) return false
    if (location.z > max.z) return false
    return true
}

fun CuboidRegion.intersects(vector: BlockVector): Boolean {
    val min = this.minimumPoint
    val max = this.maximumPoint

    if (vector.x < min.x) return false
    if (vector.y < min.y) return false
    if (vector.z < min.z) return false
    if (vector.x > max.x) return false
    if (vector.y > max.y) return false
    if (vector.z > max.z) return false
    return true
}

fun CuboidRegion.intersects(vector: BlockVector3): Boolean {
    val min = this.minimumPoint
    val max = this.maximumPoint

    if (vector.x < min.x) return false
    if (vector.y < min.y) return false
    if (vector.z < min.z) return false
    if (vector.x > max.x) return false
    if (vector.y > max.y) return false
    if (vector.z > max.z) return false
    return true
}

fun CuboidRegion.intersects(region: CuboidRegion): Boolean {
    return (this.minimumX <= region.maximumX && this.maximumX >= region.minimumX) &&
           (this.minimumY <= region.maximumY && this.maximumY >= region.minimumY) &&
           (this.minimumZ <= region.maximumZ && this.maximumZ >= region.minimumZ)
}

fun CuboidRegion.isInside(other: CuboidRegion): Boolean {
    return (this.minimumX >= other.minimumX && this.maximumX <= other.maximumX) &&
            (this.minimumY >= other.minimumY && this.maximumY <= other.maximumY) &&
            (this.minimumZ >= other.minimumZ && this.maximumZ <= other.maximumZ)
}

fun CuboidRegion.isInsideIgnoreBottom(other: CuboidRegion): Boolean {
    return (this.minimumX >= other.minimumX && this.maximumX <= other.maximumX) &&
            (this.maximumY <= other.maximumY) &&
            (this.minimumZ >= other.minimumZ && this.maximumZ <= other.maximumZ)
}
