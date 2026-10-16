package com.jeroenvdg.tntwars.misc

import com.jeroenvdg.minigame_utilities.intersects
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.tntwars.managers.mapManager.RegionType
import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.Material
import org.bukkit.block.BlockFace
import kotlin.random.Random

class PumpkinSpawner {
    fun spawn(map: ActiveMap, perTeam: Int, pumpkins: EventPumpkins) {
        val world = map.managedWorld.world ?: return
        val protected = map.getMapData().regions.filter { it.type == RegionType.Protected || it.type == RegionType.Wall }
        val spawns = map.spawns.values.flatten()

        for (team in listOf(Team.Red, Team.Blue)) {
            val region = map.teamRegions[team] ?: continue
            val min = region.minimumPoint
            val max = region.maximumPoint
            val bottom = maxOf(min.blockY, world.minHeight + 1, map.voidHeight + 1)
            val top = minOf(max.blockY, world.maxHeight - 1)
            if (bottom > top) continue

            var placed = 0
            val columns = HashSet<Pair<Int, Int>>()
            // Bound the work for sparse/empty maps; never scan the entire region volume.
            for (attempt in 0 until perTeam * 30) {
                if (placed >= perTeam) break
                val x = Random.nextInt(min.blockX, max.blockX + 1)
                val z = Random.nextInt(min.blockZ, max.blockZ + 1)
                if (!columns.add(x to z)) continue
                val surface = minOf(top, world.getHighestBlockYAt(x, z) + 1)
                for (y in surface downTo bottom) {
                    val block = world.getBlockAt(x, y, z)
                    if (!block.type.isAir || !block.getRelative(BlockFace.DOWN).type.isSolid) continue
                    if (protected.any { it.region?.intersects(block.location) == true }) break
                    if (spawns.any { it.distanceSquared(block.location) < 9 }) break
                    if (map.teamRegions.any { it.key != team && it.value.intersects(block.location) }) break

                    block.setType(Material.PUMPKIN, false)
                    pumpkins.add(BlockVector3.at(x, y, z), team)
                    placed++
                    break
                }
            }
        }
    }
}
