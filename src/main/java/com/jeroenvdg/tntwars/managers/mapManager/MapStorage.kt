package com.jeroenvdg.tntwars.managers.mapManager

import com.jeroenvdg.minigame_utilities.Debug
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class MapDocument(val file: File, val yaml: YamlConfiguration) {
    val name = file.nameWithoutExtension
    val id = MapStorage.mapId(name)
    val environment = World.Environment.valueOf(
        yaml.getString("world.dimension") ?: yaml.getString("world.environment") ?: World.Environment.NORMAL.name
    )
}

class MapStorage(plugin: Plugin, private val worldContainer: File) {
    private val metadataFolder = File(plugin.dataFolder, "maps")
    private val legacyMapsFolder = File(worldContainer, "maps")
    private val keyedMapsFolder: File?
        get() = (Bukkit.getWorld(NamespacedKey.minecraft("overworld")) ?: Bukkit.getWorlds().firstOrNull())
            ?.let { File(it.worldFolder, "dimensions/minecraft/maps") }

    fun discoverAndMigrate(): List<MapDocument> {
        val documents = mutableListOf<MapDocument>()
        for (file in metadataFolder.listFiles().orEmpty().sortedBy { it.name.lowercase() }) {
            if (!file.isFile || file.extension.lowercase() !in listOf("yml", "yaml")) continue
            try {
                documents.add(MapDocument(file, loadYaml(file)))
            } catch (exception: Exception) {
                Debug.error("Could not load map metadata '${file.path}': ${exception.message}")
            }
        }

        migrateFolders(legacyMapsFolder, documents, keyed = false)
        keyedMapsFolder?.let { migrateFolders(it, documents, keyed = true) }

        val ids = mutableSetOf<String>()
        val keys = mutableSetOf<NamespacedKey>()
        return documents.filter { document ->
            if (!ids.add(document.id) || !keys.add(worldKey(document.name))) {
                Debug.error("Ignoring duplicate map '${document.name}' in ${document.file.path}")
                false
            } else {
                !hasMigrationConflict(document)
            }
        }
    }

    fun create(name: String, environment: World.Environment): MapDocument {
        val file = metadataFile(name)
        check(!file.exists()) { "Map metadata for $name already exists" }
        val yaml = YamlConfiguration()
        yaml.set("world.dimension", environment.name)
        return MapDocument(file, yaml).also(::save)
    }

    fun save(document: MapDocument) {
        document.file.parentFile.mkdirs()
        val temporary = File(document.file.parentFile, ".${document.file.name}.tmp")
        document.yaml.save(temporary)
        try {
            Files.move(temporary.toPath(), document.file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), document.file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun migrateFolders(folder: File, documents: MutableList<MapDocument>, keyed: Boolean) {
        val knownKeys = documents.mapTo(mutableSetOf()) { worldKey(it.name) }
        for (worldFolder in folder.listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }) {
            val name = worldFolder.name
            val key = worldKey(name)
            if (key in knownKeys || metadataFile(name).exists()) continue
            val oldConfig = sequenceOf(
                File(legacyMapsFolder, "$name/cubed-data.yml"),
                File(worldFolder, "cubed-data.yml"),
            ).firstOrNull(File::isFile)
            if (!keyed && oldConfig == null && !hasLevelData(worldFolder)) continue
            try {
                val yaml = oldConfig?.let(::loadYaml) ?: YamlConfiguration()
                if (keyed && oldConfig == null) {
                    val environment = Bukkit.getWorld(key)?.environment
                        ?: error("Cannot infer the environment; add the map YAML manually")
                    yaml.set("world.dimension", environment.name)
                }
                val document = MapDocument(metadataFile(name), yaml)
                save(document)
                documents.add(document)
                knownKeys.add(key)
                Debug.log("Migrated map $name metadata to ${document.file.path}")
            } catch (exception: Exception) {
                Debug.error("Could not migrate map metadata from ${worldFolder.path}: ${exception.message}")
            }
        }
    }

    private fun hasMigrationConflict(document: MapDocument): Boolean {
        val legacyFolder = File(legacyMapsFolder, document.name)
        val keyedFolder = keyedMapsFolder?.let { File(it, worldKey(document.name).key.removePrefix("maps/")) }
            ?: return false
        if (!hasLevelData(legacyFolder) || !hasChunkData(legacyFolder) || !hasChunkData(keyedFolder)) return false
        Debug.error("Map ${document.name} exists in both ${legacyFolder.path} and ${keyedFolder.path}; " +
            "resolve the duplicate before loading it")
        return true
    }

    private fun hasLevelData(folder: File): Boolean =
        File(folder, "level.dat").isFile || File(folder, "level.dat_old").isFile

    private fun hasChunkData(folder: File): Boolean =
        folder.walkTopDown().maxDepth(4).any { it.isFile && it.extension == "mca" }

    private fun metadataFile(name: String): File = File(metadataFolder, "$name.yaml")
    private fun loadYaml(file: File): YamlConfiguration = YamlConfiguration().apply { load(file) }
    private fun worldKey(name: String): NamespacedKey = WorldCreator("maps/$name").key()

    companion object {
        fun mapId(name: String): String = name.lowercase().replace(' ', '_')
    }
}
