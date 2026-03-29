package com.jeroenvdg.minigame_utilities

import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.util.BlockVector

class Bounds private constructor() {
    public companion object {
        fun intersects(aMin: BlockVector3, aMax: BlockVector3, bMin: BlockVector3, bMax: BlockVector3): Boolean {
            return intersects(aMin.x, aMax.x, bMin.x, bMax.x) &&
                    intersects(aMin.y, aMax.y, bMin.y, bMax.y) &&
                    intersects(aMin.z, aMax.z, bMin.z, bMax.z)
        }


        fun intersects(aMin: BlockVector, aMax: BlockVector, bMin: BlockVector, bMax: BlockVector): Boolean {
            return intersects(aMin.x, aMax.x, bMin.x, bMax.x) &&
                    intersects(aMin.y, aMax.y, bMin.y, bMax.y) &&
                    intersects(aMin.z, aMax.z, bMin.z, bMax.z)
        }


        fun intersects(aMin: Int, aMax: Int, bMin: Int, bMax: Int): Boolean {
            return if (aMin > bMin) {
                bMax >= aMin
            } else {
                aMax >= bMin
            }
        }


        fun intersects(aMin: Double, aMax: Double, bMin: Double, bMax: Double): Boolean {
            return if (aMin > bMin) {
                bMax >= aMin
            } else {
                aMax >= bMin
            }
        }
    }
}