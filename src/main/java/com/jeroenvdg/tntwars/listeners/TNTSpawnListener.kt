package com.jeroenvdg.tntwars.listeners

import com.jeroenvdg.minigame_utilities.intersects
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.TNTSpawnEvent
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.MatchEndReason
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.WorldOwnershipManager.Companion.getOwnership
import com.jeroenvdg.tntwars.listeners.WorldOwnershipManager.Companion.removeOwnership
import com.jeroenvdg.tntwars.listeners.WorldOwnershipManager.Companion.setOwnership
import com.jeroenvdg.tntwars.listeners.WorldOwnershipManager.Companion.setTeam
import com.jeroenvdg.tntwars.listeners.OwnershipData
import com.jeroenvdg.tntwars.player.PlayerManager
import io.papermc.paper.math.BlockPosition
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.Rail
import org.bukkit.block.data.type.Dispenser
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.minecart.ExplosiveMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.TNTPrimeEvent
import org.bukkit.event.entity.EntityPlaceEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector


@Suppress("UnstableApiUsage")
class TNTSpawnListener(val plugin: Plugin) : Listener {
    private val primedTntOwnership = HashMap<BlockPosition, OwnershipData>()

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
        if (dispensedItem.type == Material.TNT_MINECART) {
            handleTNTMinecartDispense(event)
        }
    }

    @EventHandler
    private fun handleUnknownItems(event: ItemSpawnEvent) {
        event.entity.remove()
    }

    @EventHandler
    private fun onEntityIgnite(event: TNTPrimeEvent) {
        val block = event.block
        val primingOwnership = event.primingEntity?.getOwnership()
        val blockOwnership = block.getOwnership()

        val owner = primingOwnership?.owner ?: blockOwnership?.owner
        val team = primingOwnership?.team ?: blockOwnership?.team ?: tryGetTeam(block.location)

        val e = TNTSpawnEvent(team, owner)
        EventBus.onTNTSpawnEvent.invoke(e)
        if (e.isCancelled) {
            event.isCancelled = true
            return
        }

        block.removeOwnership()

        val point = event.block.location.toCenterLocation().toBlock()
        if (owner != null || team != null) primedTntOwnership[point] = OwnershipData(owner, team)
    }

    @EventHandler
    private fun onEntitySpawn(event: EntitySpawnEvent) {
        val entity = event.entity
        if (entity !is TNTPrimed) return
        val location = entity.location.toCenterLocation().toBlock()

        val map = GameManager.instance.activeMap
        if (map.fuseTicks >= 0) {
            entity.fuseTicks = map.fuseTicks
        }

        val ownership = primedTntOwnership.remove(location)

        if (ownership != null) entity.setOwnership(ownership)
    }

    @EventHandler
    fun onMinecartPlace(event: EntityPlaceEvent) {
        if (event.entityType == EntityType.TNT_MINECART) {
            val player = event.player?.let { PlayerManager.instance.get(it) }
            val entity = event.entity
            if (player != null) {
                entity.setOwnership(OwnershipData(player.bukkitPlayer.uniqueId.toString(), player.team))
            }
        }
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
        primedTntOwnership.clear()
    }

    private fun handleTNTDispense(event: BlockDispenseEvent) {
        val blockState = event.block.blockData
        val dispenser = event.block.state as? org.bukkit.block.Dispenser ?: return
        if (blockState !is Dispenser) return

        event.isCancelled = true

        if (!dispenser.inventory.containsAtLeast(event.item, 2)) {
            return
        }

        val ownership = event.block.getOwnership()
        val owner = ownership?.owner
        var team = ownership?.team

        if (team == null) {
            team = tryGetTeam(event.block.location)
            if (team != null) event.block.setTeam(team)
        }

        val e = TNTSpawnEvent(team, owner)
        EventBus.onTNTSpawnEvent.invoke(e)
        if (e.isCancelled) {
            return
        }

        dispenser.inventory.removeItem(event.item)

        val newLoc = event.block.location.clone().add(0.5, 0.0, 0.5).add(blockState.facing.direction)
        val entity = newLoc.block.world.spawnEntity(newLoc, EntityType.TNT)

        if (owner != null || team != null) entity.setOwnership(OwnershipData(owner, team))
    }

    private fun handleSandDispense(event: BlockDispenseEvent) {
        val blockState = event.block.blockData
        val dispenser = event.block.state as? org.bukkit.block.Dispenser ?: return
        if (blockState !is Dispenser) return

        event.isCancelled = true
        if (!dispenser.inventory.containsAtLeast(event.item, 2)) {
            return
        }

        dispenser.inventory.removeItem(event.item)
        val loc = event.block.location.add(blockState.facing.direction)
        loc.add(Vector(.5, 0.0, .5))
        val entity = loc.world.spawn(loc, FallingBlock::class.java)
        entity.blockData = Material.SAND.createBlockData()
    }

    private fun handleTNTMinecartDispense(event: BlockDispenseEvent) {
        val blockState = event.block.blockData
        val dispenser = event.block.state as? org.bukkit.block.Dispenser ?: return
        if (blockState !is Dispenser) return

        event.isCancelled = true
        if (!dispenser.inventory.containsAtLeast(event.item, 2)) {
            return
        }

        val ownership = event.block.getOwnership()
        val owner = ownership?.owner
        var team = ownership?.team

        if (team == null) {
            team = tryGetTeam(event.block.location)
            if (team != null) event.block.setTeam(team)
        }

        val loc = event.block.location.add(blockState.facing.direction)
        if (loc.block.blockData !is Rail) return
        dispenser.inventory.removeItem(event.item)
        loc.add(Vector(.5, 0.0, .5))
        val entity = loc.world.spawn(loc, ExplosiveMinecart::class.java)
        if (owner != null || team != null) entity.setOwnership(OwnershipData(owner, team))
    }
}
