package com.jeroenvdg.tntwars.listeners

import com.jeroenvdg.tntwars.game.Team
import com.destroystokyo.paper.event.block.BlockDestroyEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.persistence.PersistentDataType
import java.util.*

class WorldOwnershipManager(private val activeMap: ActiveMap) : Listener {
    companion object {
        val staffToolTag get() = NamespacedKey(TNTWars.instance, "blockownershiphelper")

        val tool
            get() = makeItem(Material.WOODEN_HOE) {
                named("&aBlock Tool &7(lore)")
                setLore {
                    line("&fCheck Block &7(Right Click)")
                    line("&fSet Block &7(Left Click)")
                }
                withPersistentData(staffToolTag, PersistentDataType.BOOLEAN, true)
            }


        fun Block.setOwner(string: String) = managerFor(world)?.setBlockOwner(this, string)
        fun Block.setOwner(player: Player) = setOwner(player.uniqueId.toString())
        fun Block.getOwner() = getOwnership()?.owner
        fun Block.getOwnership() = managerFor(world)?.blockOwnership?.get(position)
        fun Block.setOwnership(ownership: OwnershipData) = managerFor(world)?.setBlockOwnership(this, ownership)

        fun Block.getTeam() = getOwnership()?.team
        fun Block.setTeam(team: Team) = managerFor(world)?.setBlockTeam(this, team)
        fun Block.removeOwnership() = managerFor(world)?.blockOwnership?.remove(position)

        fun Entity.setOwner(string: String) {
            managerFor(world)?.setEntityOwner(this, string)
        }

        fun Entity.setOwner(player: Player) = setOwner(player.uniqueId.toString())
        fun Entity.getOwner() = getOwnership()?.owner
        fun Entity.getOwnership() = managerFor(world)?.entityOwnership?.get(uniqueId)
        fun Entity.setOwnership(ownership: OwnershipData) = managerFor(world)?.setEntityOwnership(this, ownership)

        fun Entity.getTeam() = getOwnership()?.team
        fun Entity.setTeam(team: Team) {
            managerFor(world)?.setEntityTeam(this, team)
        }

        private fun managerFor(world: World) = GameManager.instance.currentMap?.worldOwnershipManager
            ?.takeIf { it.activeMap.managedWorld.world == world }

        private val Block.position: BlockKey
            get() = BlockKey(x, y, z)
    }

    private val blockOwnership = HashMap<BlockKey, OwnershipData>()
    private val entityOwnership = HashMap<UUID, OwnershipData>()

    init {
        TNTWars.instance.addEventListener(this)
    }

    fun dispose() {
        HandlerList.unregisterAll(this)
        blockOwnership.clear()
        entityOwnership.clear()
    }

    private fun removeEntityData(entityId: UUID) {
        entityOwnership.remove(entityId)
    }

    private fun setBlockOwner(block: Block, owner: String) {
        val position = BlockKey(block.x, block.y, block.z)
        blockOwnership[position] = (blockOwnership[position] ?: OwnershipData()).copy(owner = owner)
    }

    private fun setBlockOwnership(block: Block, ownership: OwnershipData) {
        val position = BlockKey(block.x, block.y, block.z)
        if (ownership.owner == null && ownership.team == null) blockOwnership.remove(position)
        else blockOwnership[position] = ownership
    }

    private fun setBlockTeam(block: Block, team: Team) {
        val position = BlockKey(block.x, block.y, block.z)
        blockOwnership[position] = (blockOwnership[position] ?: OwnershipData()).copy(team = team)
    }

    private fun setEntityOwner(entity: Entity, owner: String) {
        entityOwnership[entity.uniqueId] = (entityOwnership[entity.uniqueId] ?: OwnershipData()).copy(owner = owner)
    }

    private fun setEntityOwnership(entity: Entity, ownership: OwnershipData) {
        if (ownership.owner == null && ownership.team == null) entityOwnership.remove(entity.uniqueId)
        else entityOwnership[entity.uniqueId] = ownership
    }

    private fun setEntityTeam(entity: Entity, team: Team) {
        entityOwnership[entity.uniqueId] = (entityOwnership[entity.uniqueId] ?: OwnershipData()).copy(team = team)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onPistonExtend(event: BlockPistonExtendEvent) {
        handlePistonEvent(event.blocks, event.direction)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onPistonRetract(event: BlockPistonRetractEvent) {
        handlePistonEvent(event.blocks, event.direction)
    }

    private fun handlePistonEvent(blockList: List<Block>, moveDirection: BlockFace) {
        // perf: update block ownership furthest first which avoids the need to allocate an intermediary list
        var index = blockList.lastIndex
        while (index >= 0) {
            val block = blockList[index--]
            if (block.type != Material.TNT) continue
            val ownership = block.getOwnership() ?: continue

            tryRemoveBlockData(block)
            block.getRelative(moveDirection).setOwnership(ownership)
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onBlockBroken(event: BlockDestroyEvent) {
        tryRemoveBlockData(event.block)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun onEntityExplode(event: EntityExplodeEvent) {
        for (block in event.blockList()) {
            tryRemoveBlockData(block)
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun onBlockExplode(event: BlockExplodeEvent) {
        for (block in event.blockList()) {
            tryRemoveBlockData(block)
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onBlockChange(event: EntityChangeBlockEvent) {
        if (event.to == event.block.type) return
        tryRemoveBlockData(event.block)
    }

    @EventHandler
    private fun onPlayerInteraction(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        val block = event.clickedBlock ?: return
        if (block.world != activeMap.managedWorld.world) return

        if (item.itemMeta.persistentDataContainer.get(staffToolTag, PersistentDataType.BOOLEAN) != true) return
        event.isCancelled = true

        val ownership = block.getOwnership()
        var ownerText = "&cNone"
        if (ownership?.owner != null) {
            try {
                val owner = Bukkit.getOfflinePlayer(UUID.fromString(ownership.owner))
                if (owner.name != null) {
                    ownerText = "&p${owner.name}"
                } else {
                    ownerText = "&cUnkown"
                }
            } catch (e: Exception) {
                ownerText = "&cError"
            }
        }

        var teamText = "&cNone"
        if (ownership?.team != null) {
            try {
                val team = ownership.team
                teamText = "&${team.primaryColor.char}${team.name}"
            } catch (e: Exception) {
                teamText = "&cError"
            }
        }

        event.player.sendMessage(Textial.cmd.format("Block owner is ${ownerText}&r. Team is $teamText"))
    }

    @EventHandler(ignoreCancelled = true)
    private fun onBlockBreak(event: BlockBreakEvent) {
        val item = event.player.inventory.itemInMainHand
        val block = event.block
        if (block.world != activeMap.managedWorld.world) return

        if (!item.hasItemMeta() || item.itemMeta.persistentDataContainer.get(
                staffToolTag,
                PersistentDataType.BOOLEAN
            ) != true
        )
            return tryRemoveBlockData(block)

        event.isCancelled = true
        block.setOwner(event.player)
    }

    @EventHandler
    private fun onEntityRemoved(event: EntityRemoveFromWorldEvent) {
        removeEntityData(event.entity.uniqueId)
    }

    @EventHandler
    private fun onWorldUnload(event: WorldUnloadEvent) {
        if (event.world != activeMap.managedWorld.world) return
        blockOwnership.clear()
        entityOwnership.clear()
    }


    private fun tryRemoveBlockData(block: Block) {
        if (block.world != activeMap.managedWorld.world) return
        blockOwnership.remove(BlockKey(block.x, block.y, block.z))
    }
}

data class OwnershipData(
    val owner: String? = null,
    val team: Team? = null,
)

private data class BlockKey(
    val x: Int,
    val y: Int,
    val z: Int,
)
