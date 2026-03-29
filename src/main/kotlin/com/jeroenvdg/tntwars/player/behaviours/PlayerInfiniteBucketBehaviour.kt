package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.player.PlayerBehaviour
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.Waterlogged
import org.bukkit.event.player.PlayerInteractEvent

class PlayerInfiniteBucketBehaviour(user: TNTWarsPlayer) : PlayerBehaviour(user) {

    override fun onActivate() {
        user.onInteract += ::handleInteract
    }

    override fun onDeactivate() {
        user.onInteract -= ::handleInteract
    }

    private fun handleInteract(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return

        val player = event.player
        val item = event.item ?: return
        if (item.type != Material.WATER_BUCKET) return
        val isSneaking = player.isSneaking

        if (!isSneaking && event.clickedBlock?.type?.asBlockType()?.isInteractable == true) {
            return
        }

        event.isCancelled = true

        val world = player.location.world
        val playerLocation = player.eyeLocation
        val direction = event.interactionPoint?.toVector()?.subtract(playerLocation.toVector())?.normalize() ?: playerLocation.direction
        val result = world.rayTraceBlocks(playerLocation, direction, 4.0, FluidCollisionMode.SOURCE_ONLY, false)

        val rayResultBlock = result?.hitBlock
        if (rayResultBlock != null) {
            val data = rayResultBlock.blockData
            if (!isSneaking && data is Waterlogged && data.isWaterlogged) {
                data.isWaterlogged = false
                rayResultBlock.blockData = data
                return
            } else if (data is Levelled) {
                result.hitBlock!!.type = Material.AIR
                return
            }
        }

        val block = event.clickedBlock ?: return

        val currentBlockData = block.blockData
        if (!isSneaking && currentBlockData is Waterlogged) {
            currentBlockData.isWaterlogged = !currentBlockData.isWaterlogged
            block.blockData = currentBlockData
            block.fluidTick()
            return
        }

        var targetBlock = block.location.add(event.blockFace.direction).block

        if (targetBlock.type == Material.AIR) {
            targetBlock.type = Material.WATER
            return
        }

        val targetBlockData = targetBlock.blockData
        if (!isSneaking && targetBlockData is Waterlogged) {
            targetBlockData.isWaterlogged = !targetBlockData.isWaterlogged
            targetBlock.blockData = targetBlockData
            targetBlock.fluidTick()
            return
        }

        val blockAbove = block.location.add(BlockFace.UP.direction).block
        if (blockAbove.type == Material.AIR && targetBlock.type != Material.WATER) {
            blockAbove.type = Material.WATER
            return
        }

        val data: BlockData
        if (targetBlock.type == Material.WATER) {
            data = targetBlockData
        } else if (blockAbove.type == Material.WATER) {
            targetBlock = blockAbove
            data = targetBlock.blockData
        } else {
            return
        }

        if (data !is Levelled) return

        if (data.level != 0) {
            targetBlock.type = Material.WATER
        } else {
            targetBlock.type = Material.AIR
        }
    }
}