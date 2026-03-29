package com.jeroenvdg.tntwars.listeners

import com.jeroenvdg.tntwars.game.Team
import com.destroystokyo.paper.event.block.BlockDestroyEvent
import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.makeItem
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.metadata.Metadatable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.util.*

class BlockOwnershipManager(val plugin: Plugin) : Listener {

    companion object {
        var instance: BlockOwnershipManager? = null
        val staffToolTag get() = instance!!.staffTag
        val ownerTag = "tntOwner"
        val teamTag = "tntTeam"

        val tool get() = com.jeroenvdg.minigame_utilities.makeItem(Material.WOODEN_HOE) {
            named("&aBlock Tool &7(lore)")
            setLore {
                line("&fCheck Block &7(Right Click)")
                line("&fSet Block &7(Left Click)")
            }
            withPersistentData(staffToolTag, PersistentDataType.BOOLEAN, true)
        }


        fun Metadatable.setOwner(string: String) = this.setMetadata(ownerTag, FixedMetadataValue(instance!!.plugin, string))
        fun Metadatable.setOwner(player: Player) = this.setOwner(player.uniqueId.toString())
        fun Metadatable.hasOwner() = this.hasMetadata(ownerTag)

        fun Metadatable.getOwner() = this.getMetadata(ownerTag).firstOrNull()?.asString()
        fun Metadatable.getOwnerAsPlayer(): Player? {
            return try {
                Bukkit.getPlayer(UUID.fromString(this.getOwner() ?: return null))
            } catch (e: Exception) {
                Debug.error(e)
                null
            }
        }
        fun Metadatable.getOwnerAsOfflinePlayer(): OfflinePlayer? {
            return try {
                Bukkit.getOfflinePlayer(UUID.fromString(this.getOwner() ?: return null))
            } catch (e: Exception) {
                Debug.error(e)
                null
            }
        }
        fun Metadatable.removeOwner() = this.removeMetadata(ownerTag, instance!!.plugin)

        fun Metadatable.hasTeam() = this.hasMetadata(teamTag)
        fun Metadatable.getTeam() = this.getMetadata(teamTag).firstOrNull()?.asString()?.let { Team.valueOf(it) }
        fun Metadatable.setTeam(team: Team) = this.setMetadata(teamTag, FixedMetadataValue(instance!!.plugin, team.name))
        fun Metadatable.removeTeam() = this.removeMetadata(teamTag, instance!!.plugin)
    }

    val staffTag = NamespacedKey(plugin, "blockownershiphelper")

    init {
        instance = this
    }

    @EventHandler
    private fun onItemPushed(event: BlockPistonExtendEvent) {
        handlePistonEvent(event.blocks, event.direction.direction)
    }

    @EventHandler
    private fun onItemPushed(event: BlockPistonRetractEvent) {
        handlePistonEvent(event.blocks, event.direction.direction)
    }

    @EventHandler
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

    // Go down for the player BlockBreakEvent
    @EventHandler
    private fun onBlockChange(event: EntityChangeBlockEvent) {
        if (event.to != event.block.type) return
        tryRemoveBlockData(event.block)
    }

    private fun tryRemoveBlockData(block: Block) {
        if (block.hasMetadata(ownerTag)) {
            block.removeMetadata(ownerTag, plugin)
        }

        if (block.hasMetadata(teamTag)) {
            block.removeMetadata(teamTag, plugin)
        }
    }

    private fun handlePistonEvent(blockList: List<Block>, moveDirection: Vector) {
        val moveMap = ArrayList<BlockChangeElement>(blockList.size)

        fun checkTag(tag: String, block: Block) {
            if (!block.hasMetadata(tag)) return
            moveMap.add(BlockChangeElement(tag, block.getMetadata(tag).first().asString(), block.location.add(moveDirection)))
            block.removeMetadata(tag, plugin)
        }

        for (i in blockList.indices) {
            val block = blockList[i]
            if (block.type != Material.TNT) continue
            checkTag(ownerTag, block)
            checkTag(teamTag, block)
        }

        for (blockChangeElement in moveMap) {
            blockChangeElement.location.block.setMetadata(blockChangeElement.tag, FixedMetadataValue(plugin, blockChangeElement.value))
        }
    }

    @EventHandler
    private fun onRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        val block = event.clickedBlock ?: return

        if (item.itemMeta.persistentDataContainer.get(staffToolTag, PersistentDataType.BOOLEAN) != true) return
        event.isCancelled = true

        var ownerText = "&cNone"
        if (block.hasOwner()) {
            try {
                val owner = block.getOwnerAsOfflinePlayer()!!
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
        if (block.hasTeam()) {
            try {
                val team = block.getTeam()!!
                teamText = "&${team.primaryColor}${team.name}"
            } catch (e: Exception) {
                teamText = "&cError"
            }
        }

        event.player.sendMessage(Textial.cmd.format("Block owner is ${ownerText}&r. Team is $teamText"))
    }

    @EventHandler
    private fun onLeftClick(event: BlockBreakEvent) {
        val item = event.player.inventory.itemInMainHand
        val block = event.block

        if (!item.hasItemMeta() || item.itemMeta.persistentDataContainer.get(staffToolTag, PersistentDataType.BOOLEAN) != true)
            return tryRemoveBlockData(block)

        event.isCancelled = true
        block.setOwner(event.player)
    }
}


data class BlockChangeElement(val tag: String, val value: String, val location: Location)