package com.jeroenvdg.tntwars.managers

import com.jeroenvdg.minigame_utilities.Debug
import org.bukkit.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class WorldManager(containerPath: String) : Collection<ManagedWorld> {
    private val containerFolder = File(containerPath)
    private val elements = ArrayList<ManagedWorld>()

    override val size get() = elements.size
    override fun isEmpty() = elements.isEmpty()
    override fun iterator() = elements.iterator()
    override fun containsAll(elements: Collection<ManagedWorld>) = this.elements.containsAll(elements)
    override fun contains(element: ManagedWorld) = elements.contains(element)


    fun load() {
        if(!Files.exists(containerFolder.toPath())) {
            Files.createDirectory(containerFolder.toPath())
        }
        if (!containerFolder.isDirectory) {
            throw Exception("Given path is not a directory")
        }

        elements.clear()

        Debug.broadcast("&m+-----------------------+")
        Debug.broadcast("Found Worlds:")
        for (worldFolder in containerFolder.listFiles()!!) {
            if (!worldFolder.isDirectory) {
                Debug.error("path '${worldFolder.path}' is not a directory")
                continue
            }

            val manWorld = ManagedWorld(worldFolder, this)
            if (manWorld.isLoaded) {
                Debug.broadcast("World: &p${manWorld.worldName} &7Loaded: &cYes &7Name: &s${manWorld.world!!.name}")
            } else {
                Debug.broadcast("World: &p${manWorld.worldName} &7Loaded: &aNo")
            }
            elements.add(manWorld)
        }

        Debug.broadcast("")
        Debug.broadcast("Bukkit Worlds:")
        for (world in Bukkit.getWorlds()) {
            Debug.broadcast("World &p${world.name}")
        }

        Bukkit.getWorlds()
        Debug.broadcast("&m+-----------------------+")
    }


    fun find(name: String): ManagedWorld? {
        val lName = name.lowercase()
        return elements.find { it.name == lName }
    }


    fun delete(name: String) {
        find(name)?.delete()
    }

    fun getWorldName(file: File): String {
        return file.relativeTo(containerFolder.parentFile).path.replace(File.separatorChar, '/')
    }

    fun create(name: String): ManagedWorld {
        if (name.contains(Regex("[\\\\/]"))) throw Exception("Illegal name!")

        val file = File("${containerFolder.path}${File.separatorChar}$name")
        val wc = WorldCreator(getWorldName(file))
        wc.type(WorldType.FLAT)
        wc.generateStructures(false)
        wc.generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\"}")
        val world = wc.createWorld()!!
        world.getBlockAt(0,0,0).type = Material.BEDROCK
        world.spawnLocation = Location(world, 0.5,1.0,0.5)

        val managedWorld = ManagedWorld(world.worldFolder, this, world)
        elements.add(managedWorld)
        return managedWorld
    }


    fun copy(name: String, target: String): ManagedWorld {
        if (target.contains(Regex("[\\\\/]"))) throw Exception("Illegal name!")

        val og = find(name) ?: throw Exception("$name doesn't exist")
        val target = File("${containerFolder.path}${File.separatorChar}$target")
        val newWorld = og.clone(target, false)
        elements.add(newWorld)

        return newWorld
    }
}


class ManagedWorld(val file: File, val worldManager: WorldManager, var world: World? = null) {
    var isLoaded: Boolean private set
    val name = file.name
    val worldName = worldManager.getWorldName(file)

    private val dataFile = File("${file.path}${File.separatorChar}cubed-data.yml")
    private var yamlObject = YamlConfiguration.loadConfiguration(dataFile)


    init {
        if (world == null) {
            world = Bukkit.getWorlds().find { it.name == worldName }
        }

        isLoaded = world != null
    }


    fun load() {
        if (isLoaded) return
        try {
            world = WorldCreator(worldManager.getWorldName(file)).createWorld() // Yes I know, a crime against humanity, but I need this to work
            isLoaded = world != null
        } catch (e: Exception) {
            Debug.error(e)
            isLoaded = false
        }

        if (!isLoaded) {
            Debug.error("Worldcreator could not load $worldName")
        }
    }


    fun unload(save: Boolean) {
        if (!isLoaded) return
        val world = world ?: return
        val defaultWorld = Bukkit.getWorld("world") ?: Bukkit.getWorlds().first()

        for (player in world.players) {
            player.teleport(defaultWorld.spawnLocation)
        }

        Bukkit.unloadWorld(world, save)
    }


    fun delete() {
        if (!isLoaded) load()
        unload(false)
        file.deleteRecursively()
    }


    fun clone(name: String, override: Boolean): ManagedWorld {
        return clone(File("${file.parentFile.parentFile.path}${File.separatorChar}$name"), override)
    }


    fun loadConfig() {
        yamlObject.load(dataFile)
    }


    fun getConfigSection(name: String): ConfigurationSection {
        return yamlObject.getConfigurationSection(name) ?: yamlObject.createSection(name)
    }


    fun saveConfig() {
        yamlObject.save(dataFile)
    }


    fun clone(file: File, override: Boolean): ManagedWorld {
        val world = ManagedWorld(file, worldManager)
        if (override && world.file.exists()) {
            world.file.deleteRecursively()
        } else if (world.file.exists()) {
            throw Exception("A world with that name already exists")
        }

        if (!this.file.copyRecursively(world.file, false)) {
            world.file.deleteRecursively()
            throw Exception("File has not been copied over successfully")
        }

        val uid = world.file.list { _, s -> s == "uid.dat" }
        if (uid != null && uid.size == 1) {
            val uidFile = File(Path.of(world.file.path, uid[0]).toString())
            uidFile.delete()
        }

        world.loadConfig()
        return world
    }
}