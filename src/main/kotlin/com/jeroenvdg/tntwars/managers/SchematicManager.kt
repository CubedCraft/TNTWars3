package com.jeroenvdg.tntwars.managers

import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.intersects
import com.jeroenvdg.minigame_utilities.isInsideIgnoreBottom
import com.jeroenvdg.tntwars.Schematic
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setTeam
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.TypeDescription
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import java.io.File

class SchematicManager(val plugin: JavaPlugin) {
    private val yaml: Yaml

    var groups = emptyArray<SchematicGroup>(); private set

    companion object {
        val instance get() = TNTWars.instance.schematicManager
    }

    init {
        val loaderOptions = LoaderOptions()

        val constructor = Constructor(SchematicConfig::class.java, loaderOptions)

        val configDesc = TypeDescription(SchematicConfig::class.java)
        configDesc.addPropertyParameters("groups", SchematicGroup::class.java)

        val groupDesc = TypeDescription(SchematicGroup::class.java)
        groupDesc.addPropertyParameters("schematics", TNTWarsSchematic::class.java)

        constructor.addTypeDescription(configDesc)
        constructor.addTypeDescription(groupDesc)

        val representer = Representer(DumperOptions())
        representer.addClassTag(SchematicConfig::class.java, Tag.MAP)

        yaml = Yaml(constructor, representer)
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
            if (sendFeedback) player.sendMessage(TextHelper.format("&cYou must join the game to paste schematics"))
            return false
        }

        var transform = AffineTransform()
        if (user.team == Team.Red) transform = transform.rotateY(180.0)

        val clipboard = schematic.clipboard
        if (clipboard == null) {
            player.sendMessage(TextHelper.format("&cSchematic &lWAS NOT FOUND"))
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
            if (sendFeedback) player.sendMessage(TextHelper.format("&cThe cannon must be fully on your side"))
            return false
        }
        if (map.protectedRegions.any { it.region!!.intersects(bounds) }) {
            if (sendFeedback) player.sendMessage(TextHelper.format("&cThe cannon overlaps with protected areas"))
            return false
        }

        val weWorld = BukkitAdapter.adapt(player.world)
        val bannedMaterials = arrayOf(BukkitAdapter.asBlockType(Material.DISPENSER), BukkitAdapter.asBlockType(Material.REDSTONE_WIRE))
        if (bounds.any { val blockType = weWorld.getBlock(it).blockType; bannedMaterials.any { it == blockType } }) {
            if (sendFeedback) player.sendMessage(TextHelper.format("&cThe cannon is overlapping an existing cannon"))
            return false
        }

        clipboard.paste(weWorld, spawnPoint, false, true, transform)

        val world = player.world
        for (blockPoint in bounds) {
            val block = world.getBlockAt(blockPoint.x, blockPoint.y, blockPoint.z)
            val type = block.type
            if (type != Material.TNT && type != Material.DISPENSER) continue
            block.setOwner(player)
            block.setTeam(user.team)
        }
        return true
    }
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