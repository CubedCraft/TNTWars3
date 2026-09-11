package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.player.PlayerBehaviour
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.Waterlogged
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerInteractEvent

class PlayerInfiniteBucketBehaviour(user: TNTWarsPlayer) : PlayerBehaviour(user) {

    override fun onActivate() {
        user.onInteract += ::handleInteract
        user.onBucketEmpty += ::handleBucketEmpty
    }

    override fun onDeactivate() {
        user.onInteract -= ::handleInteract
        user.onBucketEmpty -= ::handleBucketEmpty
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

        val world = player.location.world
        val playerLocation = player.eyeLocation
        val direction = event.interactionPoint?.toVector()?.subtract(playerLocation.toVector())?.normalize()
            ?: playerLocation.direction
        val result = world.rayTraceBlocks(playerLocation, direction, 4.0, FluidCollisionMode.SOURCE_ONLY, false)

        val rayResultBlock = result?.hitBlock
        if (rayResultBlock != null) {
            val data = rayResultBlock.blockData
            if (!isSneaking && data is Waterlogged && data.isWaterlogged) {
                event.isCancelled = true
                data.isWaterlogged = false
                rayResultBlock.blockData = data
                TNTWars.instance.replayManager.recordBlockChange(rayResultBlock)
                return
            } else if (data is Levelled) {
                event.isCancelled = true
                setType(rayResultBlock, Material.AIR)
                return
            }
        }
    }

    private fun handleBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (event.bucket != Material.WATER_BUCKET) return

        event.isCancelled = true
        val targetBlock = event.block
        val targetBlockData = targetBlock.blockData
        if (targetBlockData is Waterlogged) {
            targetBlockData.isWaterlogged = true
            targetBlock.blockData = targetBlockData
            targetBlock.fluidTick()
            TNTWars.instance.replayManager.recordBlockChange(targetBlock)
            return
        }

        setType(targetBlock, Material.WATER)
    }

    private fun setType(block: Block, material: Material) {
        block.type = material
        TNTWars.instance.replayManager.recordBlockChange(block)
    }
}
