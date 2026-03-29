package com.jeroenvdg.tntwars.listeners

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getTeam
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setTeam
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.minigame_utilities.intersects
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.util.Vector

class BlockListener : Listener {
    @EventHandler
    private fun onBlockPlaced(event: BlockPlaceEvent) {
        val user = TNTWars.instance.playerManager.get(event.player) ?: return
        if (user.team.isSpectatorTeam) {
            event.isCancelled = true
            return
        }

        val block = event.blockPlaced
        if (isBlockInRegion(block.location, TNTWars.instance.gameManager.activeMap)) {
            event.isCancelled = true
            return
        }

        user.onBlockPlaced.invoke(event)
        if (event.isCancelled) return

        val material = block.type
        if (material != Material.TNT && material != Material.DISPENSER) return
        event.block.setOwner(event.player)
        event.block.setTeam(user.team)
    }

    @EventHandler
    private fun onBlockBreak(event: BlockBreakEvent) {
        val map = TNTWars.instance.gameManager.activeMap
        if (!isBlockInRegion(event.block.location, map)) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onLiquidFlowEvent(event: BlockFromToEvent) {
        val block = event.toBlock
        if (block.type != Material.AIR) return
        val map = GameManager.instance.activeMap
        if (map.managedWorld.world != block.world) return
        if (block.location.y > GameManager.instance.activeMap.voidHeight) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockPush(event: BlockPistonExtendEvent) {
        val map = TNTWars.instance.gameManager.activeMap
        val directionVector = event.direction.direction
        for (block in event.blocks.plus(event.block)) {
            if (!isVectorInRegion(block.location.toVector().add(directionVector), map) && !isBlockInRegion(block.location, map)) continue
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    private fun onBlockPull(event: BlockPistonRetractEvent) {
        val directionVector = event.direction.direction
        val map = TNTWars.instance.gameManager.activeMap

        for (block in event.blocks) {
            if (!isVectorInRegion(block.location.toVector().add(directionVector), map) && !isBlockInRegion(block.location, map)) continue
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    private fun onEntityChaneToBlock(event: EntityChangeBlockEvent) {
        val map = TNTWars.instance.gameManager.activeMap
        if (!isBlockInRegion(event.block.location, map)) return
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onTNTKaboom(event: BlockExplodeEvent) {
        event.yield = 0f
        val block = event.block
        if (handleExplosion(block.getTeam(), block.getOwner(), block.location, event.blockList())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onTNTExploded(event: EntityExplodeEvent) {
        val map = GameManager.instance.activeMap
        val entity = event.entity
        val team = entity.getTeam()
        val owner = entity.getOwner()

        if (map.tntStrength < 0 || (team != null && map.teamRegions[team]?.intersects(entity.location) == true)) {
            event.yield = 0f
            if (handleExplosion(team, owner, entity.location, event.blockList())) {
                event.isCancelled = true
            }
            return
        }

        val block = entity.location.block
        if (team != null) block.setTeam(team)
        if (owner != null) block.setOwner(owner)

        event.isCancelled = true
        event.location.world.createExplosion(event.location, GameManager.instance.activeMap.tntStrength)
    }

    private fun isBlockInRegion(location: Location, map: ActiveMap): Boolean {
        val point = BukkitAdapter.adapt(location).toBlockPoint()
        return isBlockPointInRegion(point, map)
    }

    private fun isVectorInRegion(vector: Vector, map: ActiveMap): Boolean {
        val point = BlockVector3.at(vector.x, vector.y, vector.z)
        return isBlockPointInRegion(point, map)
    }

    private fun isBlockPointInRegion(point: BlockVector3, map: ActiveMap): Boolean {
        for (mapRegion in map.protectedRegions) {
            val region = mapRegion.region ?: continue
            if (!region.intersects(point)) continue
            return true
        }
        return false
    }

    private fun handleExplosion(team: Team?, owner: String?, center: Location, blocklist: MutableList<Block>): Boolean {
        if (team != null) {
            if (team.isSpectatorTeam) {
                return true
            }

            val bounds = GameManager.instance.activeMap.teamRegions[team]
            if (bounds != null && bounds.intersects(center)) {
                if (owner == null) {
                    blocklist.clear()
                } else {
                    blocklist.removeIf { it.getOwner() != owner }
                }
                return false
            }
        }

        val tntBounds = CuboidRegion.fromCenter(BukkitAdapter.adapt(center).toBlockPoint(), 7)
        val mapRegions = GameManager.instance.activeMap.protectedRegions

        for (mapRegion in mapRegions) {
            val region = mapRegion.region ?: continue
            if (!region.intersects(tntBounds)) continue

            blocklist.removeIf { region.intersects(it.location) }
            if (blocklist.size == 0) {
                break
            }
        }
        return false
    }
}
