package com.jeroenvdg.tntwars

import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setOwner
import com.fastasyncworldedit.core.Fawe
import com.jeroenvdg.minigame_utilities.Debug
import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.extent.transform.BlockTransformExtent
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.math.transform.Transform
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.World
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockType
import org.bukkit.Material
import java.io.File
import java.util.*

class Schematic private constructor() {
    companion object {
        lateinit var fawe: Fawe private set
        lateinit var folder: String private set
        private val replaceMap = HashMap<BlockType, BlockState>()
        private lateinit var replaceValues: Set<BlockType>


        fun get(fileName: String) : Clipboard? {
            val file = getFile(fileName)

            return try {
                ClipboardFormats.findByFile(file)?.load(file)
            } catch (e: Exception) {
                Debug.error(e)
                null
            }
        }


        fun getFile(path: String): File {
            return File("$folder${File.separator}$path")
        }


        fun setup() {
            fawe = Fawe.instance()
            folder = fawe.worldEdit.schematicsFolderPath.toString()

            replaceMap.clear()
            replaceMap[BukkitAdapter.adapt(Material.BLUE_STAINED_GLASS.createBlockData()).blockType] = BukkitAdapter.adapt(Material.RED_STAINED_GLASS.createBlockData())
            replaceMap[BukkitAdapter.adapt(Material.BLUE_GLAZED_TERRACOTTA.createBlockData()).blockType] = BukkitAdapter.adapt(Material.RED_GLAZED_TERRACOTTA.createBlockData())
            replaceMap[BukkitAdapter.adapt(Material.CYAN_STAINED_GLASS.createBlockData()).blockType] = BukkitAdapter.adapt(Material.ORANGE_STAINED_GLASS.createBlockData())
            replaceMap[BukkitAdapter.adapt(Material.BLUE_CONCRETE.createBlockData()).blockType] = BukkitAdapter.adapt(Material.RED_CONCRETE.createBlockData())

            replaceValues = replaceMap.keys
        }


        fun replaceBlueBlocks(session: EditSession, region: Region, center: BlockVector3, location: Vector3, transform: Transform) {
            val a = transform.apply(region.minimumPoint.subtract(center).toVector3()).add(location)
            val b = transform.apply(region.maximumPoint.subtract(center).toVector3()).add(location)
            val newRegion = CuboidRegion(a.toBlockPoint(), b.toBlockPoint())

            session.replaceBlocks(newRegion, BlueMask(session, replaceValues)) {
                replaceMap[session.getBlock(it).blockType]!!.applyBlock(it)
            }
        }


        fun pasteEntities(world: World, clipboard: Clipboard, to: Vector3, transform: Transform) {
            val editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(world)
                .checkMemory(false)
                .allowedRegionsEverywhere()
                .limitUnlimited()
                .changeSetNull()
                .build()

            val extent = BlockTransformExtent(editSession, transform)
            val origin = clipboard.origin.toVector3()

            for (entity in clipboard.entities) {
                if (entity.state != null && entity.state!!.type.id == "minecraft:player") break
                val pos = entity.location
                val rPos = transform.apply(entity.location.subtract(origin)).add(to)
                val newPos = com.sk89q.worldedit.util.Location(extent, rPos.x, rPos.y, rPos.z, pos.yaw, pos.pitch)

                extent.createEntity(newPos, entity.state)
            }

            editSession.flushQueue()
            editSession.close()
        }


        fun applyMetadataTagsToTNT(bukkitWorld: org.bukkit.World, owner: UUID, clipboard: Clipboard, from: BlockVector3, transform: Transform) {
            for (vector in clipboard.region) {
                val currentCoordinate = transform.apply(vector.subtract(clipboard.origin).toVector3()).add(from.toVector3())
                val bukkitBlock = bukkitWorld.getBlockAt(currentCoordinate.blockX, currentCoordinate.blockY, currentCoordinate.blockZ)

                if (bukkitBlock.type != Material.TNT && bukkitBlock.type != Material.DISPENSER) { continue }

                bukkitBlock.setOwner(owner.toString())
            }
        }


        private class BlueMask(val session: EditSession, val type: Set<BlockType>) : Mask {
            override fun test(vector: BlockVector3?): Boolean {
                val b = session.getBlock(vector).blockType
                return type.any { it.equals(b) }
            }

            override fun copy(): Mask {
                return BlueMask(session, type)
            }
        }
    }
}