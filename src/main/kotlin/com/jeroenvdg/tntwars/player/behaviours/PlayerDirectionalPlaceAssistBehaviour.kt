package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.player.PlayerBehaviour
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.services.playerSettings.DispenserPlaceAssistLevel
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.Dispenser
import org.bukkit.block.data.Directional
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.min

class PlayerDirectionalPlaceAssistBehaviour(user: TNTWarsPlayer) : PlayerBehaviour(user) {
    override fun onActivate() {
        user.onBlockPlaced += ::handleBlockPlaced
    }

    override fun onDeactivate() {
        user.onBlockPlaced -= ::handleBlockPlaced
    }

    private fun handleBlockPlaced(event: BlockPlaceEvent) {
        val blockState = event.block.state
        if (blockState !is Dispenser) return

        var tntToAdd = TNTWars.instance.config.gameConfig.tntInDispenser
        val map = GameManager.instance.activeMap
        if (map.tntCount >= 0) {
            tntToAdd = map.tntCount
        }

        while (tntToAdd > 0) {
            val stackSize = min(tntToAdd, 64)
            blockState.inventory.addItem(ItemStack(Material.TNT, stackSize))
            tntToAdd -= stackSize
        }

        when (user.settings.dispenserAssistLevel) {
            DispenserPlaceAssistLevel.None -> return
            DispenserPlaceAssistLevel.ShiftOnly -> if (!player.isSneaking) return
            DispenserPlaceAssistLevel.Full -> {}
        }

        val blockData = blockState.blockData
        if (blockData !is Directional) return

        val blockPos = blockState.location
        val playerPos = player.location

        val dirToPlayer = playerPos.toBlockLocation().toVector().subtract(blockPos.toVector())
        val blockFace = event.blockAgainst.getFace(event.block)
        if (dirToPlayer.y == -1.0 && blockFace == BlockFace.UP) {
            dirToPlayer.y = 0.0
        }

        if (dirToPlayer.y == 0.0 && dirToPlayer.length() == 1.0) {
            if (dirToPlayer.x == 1.0) {
                blockData.facing = BlockFace.EAST
            } else if (dirToPlayer.x == -1.0) {
                blockData.facing = BlockFace.WEST
            } else if (dirToPlayer.z == 1.0) {
                blockData.facing = BlockFace.SOUTH
            } else {
                blockData.facing = BlockFace.NORTH
            }
            blockState.block.blockData = blockData
            return
        }

        val pitch = playerPos.pitch
        val yaw = (player.yaw + 180 + 45) % 360
        val yDiv = blockPos.y - playerPos.y

        if (pitch > 65 && yDiv > -.2f) {
            blockData.facing = BlockFace.UP
        } else if (pitch > 45 && yDiv < - .1999f) {
            blockData.facing = BlockFace.UP
        } else if (pitch < -45) {
            blockData.facing = BlockFace.DOWN
        } else if (yaw < 90) {
            blockData.facing = BlockFace.SOUTH
        } else if (yaw < 180) {
            blockData.facing = BlockFace.WEST
        } else if (yaw < 270) {
            blockData.facing = BlockFace.NORTH
        } else {
            blockData.facing = BlockFace.EAST
        }

        blockState.block.blockData = blockData
    }
}