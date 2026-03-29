package com.jeroenvdg.tntwars.managers.mapManager

import com.jeroenvdg.minigame_utilities.manager.Manager
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.managers.WorldManager
import java.io.File
import java.util.*

class MapManager(val worldManager: WorldManager) : Manager<TNTWarsMap>() {

    companion object {
        val instance get() = TNTWars.instance.mapManager
    }

    fun loadAll() {
        clear()
        worldManager.load()
        for (world in worldManager) {
            add(TNTWarsMap(world, world.name))
        }
    }

    fun saveAll() {
        for (map in this) {
            map.saveToConfig()
        }
    }

    fun create(name: String) {
        val world = worldManager.create(name)
        add(TNTWarsMap(world, name))
    }

    fun activateMap(map: TNTWarsMap): ActiveMap {
        if (!map.enabled) throw IllegalStateException("Map ${map.id} is not enabled")

        val world = map.managedWorld.clone("active${File.separatorChar}${map.id}__${UUID.randomUUID()}", false)
        world.load()
        return ActiveMap(map, world)
    }

    fun cleanupLeftoverMaps() {
        val file = File(".${File.separatorChar}active")
        if (file.exists()) {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }

        File(".${File.separatorChar}active").mkdir()
    }
}