package com.jeroenvdg.tntwars.managers

import com.jeroenvdg.tntwars.Schematic
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setTeam
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.intersects
import com.jeroenvdg.minigame_utilities.isInsideIgnoreBottom
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.TypeDescription
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.File

class SchematicManager(val plugin: JavaPlugin) {
    private val yaml: Yaml

    var groups = emptyArray<SchematicGroup>(); private set

    companion object {
        val instance get() = TNTWars.instance.schematicManager
    }

    init {
        val constructor = Constructor(SchematicConfig::class.java, LoaderOptions())
        val description = TypeDescription(SchematicConfig::class.java)
        description.addPropertyParameters("groups", SchematicGroup::class.java)
        description.addPropertyParameters("groups.schematics", TNTWarsSchematic::class.java)

        constructor.addTypeDescription(description)
        yaml = Yaml(constructor)
        load()
    }

    fun save() {
        val file = File(plugin.dataFolder, "shop.yml")
        val config = SchematicConfig()
        config.groups = groups
        yaml.dump(config, file.writer())
    }

    fun load() {
        val file = File(plugin.dataFolder, "shop.yml")
        if (!file.exists()) {
            file.createNewFile()
            if (!file.exists()) throw Exception("File '${file.absolutePath}' could not be created")
            save()
        }

        val config = yaml.loadAs(file.reader(), SchematicConfig::class.java)
        groups = config.groups
    }

    fun pasteSchematic(user: TNTWarsPlayer, schematic: TNTWarsSchematic, sendFeedback: Boolean = true): Boolean {
        val player = user.bukkitPlayer
        if (user.team.isSpectatorTeam) {
            if (sendFeedback) player.sendMessage(Textial.msg.parse("&cYou must join the game to paste schematics"))
            return false
        }

        var transform = AffineTransform()
        if (user.team == Team.Red) transform = transform.rotateY(180.0)

        val clipboard = schematic.clipboard
        if (clipboard == null) {
            player.sendMessage(Textial.msg.parse("&cSchematic &lWAS NOT FOUND"))
            return false
        }

        val region = clipboard.region
        val origin = clipboard.origin
        val spawnPoint = BukkitAdapter.adapt(player.location).toBlockPoint()

        val a = transform.apply(region.minimumPoint.subtract(origin).toVector3()).add(spawnPoint.toVector3())
        val b = transform.apply(region.maximumPoint.subtract(origin).toVector3()).add(spawnPoint.toVector3())

        val bounds = CuboidRegion(a.toBlockPoint(), b.toBlockPoint())
        val map = GameManager.instance.activeMap
        if (!bounds.isInsideIgnoreBottom(map.teamRegions[user.team]!!)) {
            if (sendFeedback) player.sendMessage(Textial.msg.parse("&cThe cannon must be fully on your side"))
            return false
        }
        if (map.protectedRegions.any { it.region!!.intersects(bounds) }) {
            if (sendFeedback) player.sendMessage(Textial.msg.parse("&cThe cannon overlaps with protected areas"))
            return false
        }

        val weWorld = BukkitAdapter.adapt(player.world)
        val bannedMaterials = arrayOf(BukkitAdapter.asBlockType(Material.DISPENSER), BukkitAdapter.asBlockType(Material.REDSTONE_WIRE))
        if (bounds.any { val blockType = weWorld.getBlock(it).blockType; bannedMaterials.any { it == blockType } }) {
            if (sendFeedback) player.sendMessage(Textial.msg.parse("&cThe cannon is overlapping an existing cannon"))
            return false
        }

        val world = player.world
        val blockPositions = bounds.map { blockPoint ->
            BlockPosition(blockPoint.x, blockPoint.y, blockPoint.z)
        }
        val previousBlockData = blockPositions.associateWith { blockPoint ->
            world.getBlockAt(blockPoint.x, blockPoint.y, blockPoint.z).blockData.asString
        }

        WorldEdit.getInstance().newEditSessionBuilder()
            .world(weWorld)
            .checkMemory(false)
            .allowedRegionsEverywhere()
            .limitUnlimited()
            .changeSetNull()
            .build()
            .use { editSession ->
                val holder = ClipboardHolder(clipboard)
                holder.transform = transform

                val operation = holder.createPaste(editSession)
                    .to(spawnPoint)
                    .ignoreAirBlocks(false)
                    .copyEntities(true)
                    .build()

                Operations.complete(operation)
                editSession.flushQueue()
            }

        for (blockPoint in blockPositions) {
            val block = world.getBlockAt(blockPoint.x, blockPoint.y, blockPoint.z)
            if (previousBlockData[blockPoint] != block.blockData.asString) {
                ReplayManager.instance.recordBlockChange(block)
            }

            val type = block.type
            if (type != Material.TNT && type != Material.DISPENSER) continue
            block.setOwner(player)
            block.setTeam(user.team)
        }
        return true
    }

    private data class BlockPosition(val x: Int, val y: Int, val z: Int)
}

class SchematicConfig {
    var groups = emptyArray<SchematicGroup>()
}

class SchematicGroup() {

    var name = ""
    var description = ""
    var schematics = emptyArray<TNTWarsSchematic>()

    fun isReady(): Boolean {
        return name.isNotBlank() && schematics.any { it.isReady() }
    }
}

class TNTWarsSchematic() {

    var name: String = ""
    var description = ""
    var schematic: String = ""
    var price: Int = -1
    var material = Material.TNT
    var enchanted = false

    val clipboard: Clipboard?
        get() {
            return Schematic.get(schematic)
        }

    fun isReady(): Boolean {
        return name.isNotBlank() && clipboard != null && price > 0
    }
}
