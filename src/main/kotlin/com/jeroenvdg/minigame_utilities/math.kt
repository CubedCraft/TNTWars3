package com.jeroenvdg.minigame_utilities

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import org.bukkit.Location
import kotlin.math.floor


fun Location.set(vec: BlockVector3) : Location {
    this.x = vec.x.toDouble()
    this.y = vec.y.toDouble()
    this.z = vec.z.toDouble()
    return this
}


fun Location.set(vec: Vector3) : Location {
    this.x = vec.x
    this.y = vec.y
    this.z = vec.z
    return this
}


fun Location.round(): Location {
    this.x = kotlin.math.round(this.x + .5) - .5
    this.y = kotlin.math.round(this.y)
    this.z = kotlin.math.round(this.z + .5) - .5
    this.pitch = kotlin.math.round(this.pitch / 90) * 90
    this.yaw = kotlin.math.round(this.yaw / 90) * 90

    return this
}


fun parseTime(time: Int): String {
    val h = floor(time/60f).toInt()
    val m = (time - h*60).toString()
    return "${h}:${if (m.length == 1) "0$m" else m}"
}