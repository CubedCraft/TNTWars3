package xyz.pondwader.replay_engine

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.ItemSpawnEvent

class ReplayWorldListener : Listener {
    @EventHandler
    private fun onEntitySpawn(event: EntitySpawnEvent) {
        if (!GameReplay.isReplayWorld(event.entity.world)) return
        if (event.entity is Player) return

        event.isCancelled = true
    }

    @EventHandler
    private fun onItemSpawn(event: ItemSpawnEvent) {
        if (!GameReplay.isReplayWorld(event.entity.world)) return

        event.isCancelled = true
    }
}
