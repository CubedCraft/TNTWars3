package com.jeroenvdg.tntwars.misc

import com.jeroenvdg.minigame_utilities.Textial.Companion.deserialize
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPlaceEvent
import org.bukkit.event.entity.EntitySpawnEvent
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch

class EntityLimiter(val plugin: TNTWars) {
    @OptIn(ExperimentalAtomicApi::class)
    class LimitationListener : Listener {
        private var entityCount: Map<EntityType, AtomicInt> = limitations.keys.associateWith { AtomicInt(0) }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onEntitySpawn(event: EntitySpawnEvent) {
            val entityType = event.entityType
            if(entityType !in limitations.keys) return
            increaseEntityCount(entityType)

            val entities = GameManager.instance.activeMap.managedWorld.world?.entities!!.filter{it.type == entityType}.size

            val limit = limitations[entityType] ?: 0
            if(entities >= limit){
                event.isCancelled = true
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onEntitySpawn(event: EntityPlaceEvent) {
            val entityType = event.entityType
            if(entityType !in limitations.keys) return
            increaseEntityCount(entityType)

            val entities = GameManager.instance.activeMap.managedWorld.world?.entities!!.filter{it.type == entityType}.size

            val limit = limitations[entityType] ?: 0
            if(entities >= limit){
                event.player?.sendMessage(deserialize("&cEntity limit is reached, please wait until they have exploded or despawned"))
                event.isCancelled = true
            }
        }

        private fun isLimitReached(entityType: EntityType): Boolean {
            val limit = limitations[entityType] ?: 0
            return (entityCount[entityType]?.load() ?: 0) >= limit
        }

        private fun increaseEntityCount(entityType: EntityType) {
            entityCount[entityType]?.incrementAndFetch()
        }

        private fun decreaseEntityCount(entityType: EntityType) {
            entityCount[entityType]?.decrementAndFetch()
        }
    }

    companion object {
        lateinit var instance: EntityLimiter
        private val limitations: HashMap<EntityType, Int> = hashMapOf(
            EntityType.TNT_MINECART to 200
        )
    }

    init {
        instance = this
    }

    fun init() {
        registerEvents()
    }

    private fun registerEvents() {
        plugin.addEventListener(LimitationListener())
    }
}