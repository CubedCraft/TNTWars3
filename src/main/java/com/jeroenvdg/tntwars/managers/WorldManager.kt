package com.jeroenvdg.tntwars.managers

import com.jeroenvdg.minigame_utilities.Debug
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import java.io.File

class WorldManager(val worldContainer: File) {
    fun create(name: String, environment: World.Environment): ManagedWorld {
        if (name.contains(Regex("[\\\\/]"))) throw IllegalArgumentException("Illegal name")
        val worldName = "maps/$name"
        val creator = worldCreator(worldName, environment)
        val world = creator.createWorld() ?: throw IllegalStateException("Could not create world $worldName")
        world.getBlockAt(0, 0, 0).type = Material.BEDROCK
        world.spawnLocation = Location(world, 0.5, 1.0, 0.5)
        return ManagedWorld(this, name, world.name, world.key, environment, world)
    }

    fun managedWorld(name: String, environment: World.Environment): ManagedWorld {
        val worldName = name.replace(File.separatorChar, '/')
        val key = WorldCreator(worldName).key()
        return ManagedWorld(this, name.substringAfterLast('/'), worldName, key, environment)
    }

    fun cleanupRuntimeWorlds() {
        deleteUnloadedChildren(File(worldContainer, "active"))
        val overworld = Bukkit.getWorld(NamespacedKey.minecraft("overworld")) ?: Bukkit.getWorlds().firstOrNull()
            ?: return
        deleteUnloadedChildren(File(overworld.worldFolder, "dimensions/minecraft/active"))
    }

    private fun deleteUnloadedChildren(folder: File) {
        val loadedPaths = Bukkit.getWorlds().map { it.worldFolder.canonicalFile.toPath() }
        for (child in folder.listFiles() ?: return) {
            val childPath = child.canonicalFile.toPath()
            if (loadedPaths.any { it.startsWith(childPath) }) continue
            child.deleteRecursively()
        }
        if (folder.listFiles()?.isEmpty() == true) folder.delete()
    }

    internal fun worldCreator(worldName: String, environment: World.Environment): WorldCreator {
        val creator = WorldCreator(worldName)
        creator.type(WorldType.FLAT)
        if (environment != World.Environment.NORMAL) {
            creator.environment(environment)
            creator.generator(object : org.bukkit.generator.ChunkGenerator() {})
        }
        creator.generateStructures(false)
        creator.generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\"}")
        return creator
    }
}

class ManagedWorld(
    private val worldManager: WorldManager,
    val name: String,
    val worldName: String,
    val worldKey: NamespacedKey,
    val environment: World.Environment,
    var world: World? = null,
) {
    private var storageFolder: File? = world?.worldFolder
    val isLoaded get() = world != null

    val dimension get() = world?.environment ?: environment
    val file: File
        get() = world?.worldFolder ?: storageFolder ?: File(worldManager.worldContainer, worldName)

    init {
        if (world == null) {
            world = findLoadedWorld()
            storageFolder = world?.worldFolder
        }
    }

    fun load() {
        if (isLoaded) return
        try {
            val loadedWorld = findLoadedWorld() ?: worldManager.worldCreator(worldName, environment).createWorld()
            if (loadedWorld != null && loadedWorld.key != worldKey) {
                throw IllegalStateException("World $worldName loaded with key ${loadedWorld.key}, expected $worldKey")
            }
            world = loadedWorld
            storageFolder = world?.worldFolder
        } catch (exception: Exception) {
            Debug.error(exception)
            world = null
        }
        if (!isLoaded) Debug.error("WorldCreator could not load $worldName ($worldKey)")
    }

    private fun findLoadedWorld(): World? {
        return Bukkit.getWorld(worldKey) ?: Bukkit.getWorld(worldName)?.takeIf { it.key == worldKey }
    }

    fun unload(save: Boolean): Boolean {
        if (!isLoaded) return true
        val loadedWorld = world ?: return true
        val defaultWorld = Bukkit.getWorld(NamespacedKey.minecraft("overworld")) ?: Bukkit.getWorlds().first()
        for (player in loadedWorld.players) player.teleport(defaultWorld.spawnLocation)
        if (!Bukkit.unloadWorld(loadedWorld, save)) return false
        world = null
        return true
    }

    fun delete() {
        val storageFolder = world?.worldFolder ?: file
        if (!unload(false)) throw IllegalStateException("Could not unload world $worldName")
        storageFolder.deleteRecursively()
    }

    fun clone(name: String): ManagedWorld {
        val target = prepareClone(name).copyFiles()
        try {
            target.load()
            check(target.isLoaded) { "Could not load runtime world ${target.worldName}" }
            return target
        } catch (exception: Exception) {
            target.cleanupFailedClone()
            throw exception
        }
    }

    // Bukkit preparation must run on the server thread; only copyFiles() may run asynchronously.
    fun prepareClone(name: String): PreparedWorldClone {
        val wasLoaded = isLoaded
        val target = worldManager.managedWorld(name, environment)
        try {
            load()
            val source = world ?: throw IllegalStateException("Could not load source world $worldName")
            source.save()
            target.load()
            val targetWorld = target.world ?: throw IllegalStateException("Could not create runtime world ${target.worldName}")
            copySettings(source, targetWorld)
            val targetFolder = targetWorld.worldFolder
            val targetDataFolder = chunkFolder(targetWorld)
            if (!target.unload(true)) throw IllegalStateException("Could not unload runtime world ${target.worldName}")
            return PreparedWorldClone(target, chunkFolder(source), targetDataFolder, targetFolder)
        } catch (exception: Exception) {
            target.cleanupFailedClone()
            throw exception
        } finally {
            if (!wasLoaded && !unload(false)) Debug.error("Could not unload template world $worldName")
        }
    }

    private fun cleanupFailedClone() {
        runCatching { delete() }
            .onFailure { Debug.error("Could not clean up failed runtime world $worldName: ${it.message}") }
    }

    private fun chunkFolder(world: World): File {
        // Older servers nest Nether/End chunks; keyed dimensions store them directly in worldFolder.
        val dimensionFolder = when (world.environment) {
            World.Environment.NETHER -> File(world.worldFolder, "DIM-1")
            World.Environment.THE_END -> File(world.worldFolder, "DIM1")
            else -> world.worldFolder
        }
        return dimensionFolder.takeIf { it.isDirectory } ?: world.worldFolder
    }

    private fun copySettings(source: World, target: World) {
        for (gameRule in Registry.GAME_RULE) copyGameRule(source, target, gameRule)
        target.difficulty = source.difficulty
        target.spawnLocation = source.spawnLocation.apply { world = target }
        target.fullTime = source.fullTime
        target.setStorm(source.hasStorm())
        target.isThundering = source.isThundering
        target.weatherDuration = source.weatherDuration
        target.thunderDuration = source.thunderDuration
        target.clearWeatherDuration = source.clearWeatherDuration
        val border = source.worldBorder
        target.worldBorder.apply {
            setCenter(border.center.x, border.center.z)
            size = border.size
            damageAmount = border.damageAmount
            damageBuffer = border.damageBuffer
            warningDistance = border.warningDistance
            warningTimeTicks = border.warningTimeTicks
        }
    }

    private fun <T : Any> copyGameRule(source: World, target: World, gameRule: GameRule<T>) {
        val key = gameRule.key.toString()
        if (!source.isGameRule(key) || !target.isGameRule(key)) return
        val value = source.getGameRuleValue(gameRule) ?: return
        target.setGameRule(gameRule, value)
    }
}

class PreparedWorldClone internal constructor(
    private val target: ManagedWorld,
    private val sourceFolder: File,
    private val targetDataFolder: File,
    private val targetFolder: File,
) {
    fun copyFiles(): ManagedWorld {
        try {
            for (directoryName in listOf("region", "entities", "poi")) {
                val sourceDirectory = File(sourceFolder, directoryName)
                val targetDirectory = File(targetDataFolder, directoryName)
                check(targetDirectory.deleteRecursively()) { "Could not clear $targetDirectory" }
                if (sourceDirectory.exists()) sourceDirectory.copyRecursively(targetDirectory, overwrite = false)
            }
            return target
        } catch (exception: Exception) {
            // The target is unloaded, so failure cleanup here must only touch the filesystem.
            targetFolder.deleteRecursively()
            throw exception
        }
    }
}
