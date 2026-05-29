package xyz.pondwader.replay_engine.replay

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.event.world.TimeSkipEvent

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

    @EventHandler
    private fun onEntityDamage(event: EntityDamageEvent) {
        if (!GameReplay.isReplayWorld(event.entity.world)) return

        event.isCancelled = true
    }

    @EventHandler
    private fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        if (!GameReplay.isReplayWorld(player.world)) return

        event.isCancelled = true
        player.foodLevel = 20
    }

    @EventHandler
    private fun onPlayerMove(event: PlayerMoveEvent) {
        if (!GameReplay.isReplayWorld(event.player.world)) return
        if (event.to.y >= event.player.world.minHeight) return

        event.player.teleport(event.player.world.spawnLocation)
    }

    @EventHandler
    private fun onWeatherChange(event: WeatherChangeEvent) {
        if (!GameReplay.isReplayWorld(event.world)) return

        event.isCancelled = true
    }

    @EventHandler
    private fun onTimeSkip(event: TimeSkipEvent) {
        if (!GameReplay.isReplayWorld(event.world)) return

        event.isCancelled = true
    }
}
