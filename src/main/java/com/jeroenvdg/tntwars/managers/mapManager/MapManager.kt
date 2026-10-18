package com.jeroenvdg.tntwars.managers.mapManager

import com.jeroenvdg.minigame_utilities.manager.Manager
import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.managers.WorldManager
import org.bukkit.World
import java.util.*

class MapManager(val worldManager: WorldManager, private val mapStorage: MapStorage) : Manager<TNTWarsMap>() {
    companion object {
        val instance get() = TNTWars.instance.mapManager
    }

    fun loadAll() {
        clear()
        for (document in mapStorage.discoverAndMigrate()) {
            try {
                val world = worldManager.managedWorld("maps/${document.name}", document.environment)
                add(TNTWarsMap(world, document, mapStorage))
            } catch (exception: Exception) {
                Debug.error("Could not load map ${document.name}: ${exception.message}")
            }
        }
    }

    fun saveAll() {
        for (map in this) {
            map.saveToConfig()
        }
    }

    fun create(name: String, environment: World.Environment) {
        val id = MapStorage.mapId(name)
        if (exists(id)) throw IllegalStateException("Map $id already exists")
        val world = worldManager.create(name, environment)
        val document = mapStorage.create(name, environment)
        val map = TNTWarsMap(world, document, mapStorage)
        map.saveToConfig()
        world.world?.save()
        add(map)
    }

    fun activateMap(map: TNTWarsMap): ActiveMap {
        if (!map.enabled) throw IllegalStateException("Map ${map.id} is not enabled")

        val world = map.managedWorld.clone("active/${map.id}__${UUID.randomUUID()}")
        return ActiveMap(map, world)
    }

    fun cleanupLeftoverMaps() {
        worldManager.cleanupRuntimeWorlds()
    }

    fun findByWorld(world: World): TNTWarsMap? {
        return find { it.managedWorld.world === world || it.managedWorld.worldKey == world.key }
    }
}
