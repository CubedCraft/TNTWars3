package com.jeroenvdg.tntwars.listeners

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.TNTSpawnEvent
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.MatchEndReason
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getTeam
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.hasOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.hasTeam
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.removeOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.removeTeam
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setTeam
import com.jeroenvdg.minigame_utilities.intersects
import io.papermc.paper.math.BlockPosition
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.type.Dispenser
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.TNTPrimeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector

@Suppress("UnstableApiUsage")
class TNTSpawnListener(val plugin: Plugin) : Listener {

    private val tntPrimeOwnerMap: HashMap<BlockPosition, String> = HashMap()
    private val tntPrimeTeamMap: HashMap<BlockPosition, Team> = HashMap()

    init {
        EventBus.onMatchEnded += ::handleMatchEnded
    }

    @EventHandler
    private fun onBlockDispense(event: BlockDispenseEvent) {
        val dispensedItem = event.item
        if (dispensedItem.type == Material.TNT) {
            handleTNTDispense(event)
        }
        if (dispensedItem.type == Material.SAND) {
            handleSandDispense(event)
        }
    }

    @EventHandler
    private fun onEntityIgnite(event: TNTPrimeEvent) {
        val block = event.block

        val owner = if (event.primingEntity != null && event.primingEntity!!.hasOwner()) {
            event.primingEntity!!.getOwner()!!
        } else {
            block.getOwner()
        }

        val team = if (event.primingEntity != null && event.primingEntity!!.hasTeam()) {
            event.primingEntity!!.getTeam()!!
        } else {
            block.getTeam() ?: tryGetTeam(block.location)
        }

        val e = TNTSpawnEvent(team, owner)
        EventBus.onTNTSpawnEvent.invoke(e)
        if (e.isCancelled) {
            event.isCancelled = true
            return
        }

        block.removeOwner()
        block.removeTeam()

        val point = event.block.location.toCenterLocation().toBlock()
        if (owner != null) tntPrimeOwnerMap[point] = owner
        if (team != null) tntPrimeTeamMap[point] = team
    }

    @EventHandler
    private fun onEntitySpawn(event: EntitySpawnEvent) {
        val entity =event.entity
        if (entity !is TNTPrimed) return
        val location = entity.location.toCenterLocation().toBlock()

        val map = GameManager.instance.activeMap
        if (map.fuseTicks >= 0) {
            entity.fuseTicks = map.fuseTicks
        }

        val owner = tntPrimeOwnerMap.remove(location)
        val team = tntPrimeTeamMap.remove(location)

        if (owner != null) entity.setOwner(owner)
        if (team != null) entity.setTeam(team)
    }

    private fun tryGetTeam(location: Location): Team? {
        for (teamData in GameManager.instance.activeMap.teamRegions) {
            val region = teamData.value
            if (!region.intersects(location)) continue
            return teamData.key
        }
        return null
    }

    private fun handleMatchEnded(reason: MatchEndReason) {
        tntPrimeTeamMap.clear()
        tntPrimeOwnerMap.clear()
    }

    private fun handleTNTDispense(event: BlockDispenseEvent) {
        val blockState = event.block.state.blockData
        val block = event.block.state
        if (block !is org.bukkit.block.Dispenser) return
        if (blockState !is Dispenser) return

        event.isCancelled = true

        if (!block.inventory.containsAtLeast(event.item, 2)) {
            return
        }

        val owner = block.getOwner()
        var team = block.getTeam()

        if (team == null) {
            team = tryGetTeam(block.location)
            if (team != null) block.setTeam(team)
        }

        val e = TNTSpawnEvent(team, owner)
        EventBus.onTNTSpawnEvent.invoke(e)
        if (e.isCancelled) {
            return
        }

        block.inventory.removeItem(event.item)

        val newLoc = event.block.location.clone().add(0.5, 0.0, 0.5).add(blockState.facing.direction)
        val entity = newLoc.block.world.spawnEntity(newLoc, EntityType.TNT)

        if (owner != null) entity.setOwner(owner)
        if (team != null) entity.setTeam(team)
    }

    private fun handleSandDispense(event: BlockDispenseEvent) {
        val blockState = event.block.state.blockData
        val block = event.block.state
        if (block !is org.bukkit.block.Dispenser) return
        if (blockState !is Dispenser) return

        event.isCancelled = true
        if (!block.inventory.containsAtLeast(event.item, 2)) {
            return
        }

        block.inventory.removeItem(event.item)
        val loc = block.location.add(blockState.facing.direction)
        loc.add(Vector(.5, 0.0, .5))
        val entity = loc.world.spawn(loc, FallingBlock::class.java)
        entity.blockData = Material.SAND.createBlockData()
    }
}