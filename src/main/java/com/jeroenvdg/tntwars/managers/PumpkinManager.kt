package com.jeroenvdg.tntwars.managers

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.listeners.OwnershipData
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.tntwars.misc.EventPumpkins
import com.jeroenvdg.tntwars.misc.PumpkinSpawner
import com.jeroenvdg.tntwars.services.pumpkins.HikariPumpkinStatsService
import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.logging.Level

class PumpkinManager(private val plugin: TNTWars) : Listener {
    val stats = plugin.services.getServiceOptional(HikariPumpkinStatsService::class.java)
    private val pumpkins = EventPumpkins()
    private val roundTotals = HashMap<UUID, Int>()
    private val notifications = HashMap<UUID, Notification>()

    @Volatile
    private var disposed = false
    val isEnabled get() = stats != null && plugin.config.gameConfig.season.equals("halloween", ignoreCase = true)

    init {
        if (stats == null && plugin.config.gameConfig.season.equals("halloween", ignoreCase = true)) {
            plugin.logger.warning("Pumpkin event disabled: mySQL.enabled must be true to persist pumpkin totals.")
        }
        EventBus.onMapChanged += ::handleMapChanged
        plugin.addEventListener(this)
    }

    private fun handleMapChanged(map: ActiveMap) {
        pumpkins.clear()
        roundTotals.clear()
        clearNotifications()
        if (isEnabled) PumpkinSpawner().spawn(map, plugin.config.gameConfig.pumpkinsPerTeam, pumpkins)
    }

    internal fun onBlocksDestroyed(blocks: List<Block>, tntOwnership: OwnershipData?) {
        val map = plugin.gameManager.activeMap
        val scoring = isEnabled && plugin.gameManager.isActive && map.startedTime > 0 && !map.gracePeriodActive
        val owner = tntOwnership?.owner?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val attackingTeam = tntOwnership?.team.takeIf { scoring && owner != null }
        var amount = 0
        for (block in blocks) {
            if (block.type !in pumpkinMaterials) {
                pumpkins.remove(block.position)
                continue
            }
            if (pumpkins.destroy(block.position, attackingTeam)) amount++
        }
        if (amount == 0 || owner == null) return

        // Count destruction immediately so the round summary does not depend on pending database writes.
        roundTotals[owner] = getRoundTotal(owner) + amount
        val player = Bukkit.getPlayer(owner)
        val name = player?.name ?: Bukkit.getOfflinePlayer(owner).name
        stats?.add(owner, name, amount)?.whenComplete { total, error ->
            if (error != null) {
                plugin.logger.log(Level.SEVERE, "Could not save $amount pumpkin points for $owner", error)
            } else if (!disposed && plugin.isEnabled) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (disposed || plugin.gameManager.currentMap !== map) return@Runnable
                    Bukkit.getPlayer(owner)?.let { showNotification(it, amount, total) }
                })
            }
        }
    }

    private fun showNotification(player: Player, amount: Int, total: Long) {
        val previous = notifications.remove(player.uniqueId)
        previous?.hideTask?.cancel()
        val bar = previous?.bar ?: Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID)
        bar.setTitle(Textial.bossbar.str("&a+$amount &6Pumpkin${if (amount == 1) "" else "s"} &8• &7Total: &e$total"))
        bar.progress = 1.0
        bar.addPlayer(player)
        bar.isVisible = true
        val hideTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable { removeNotification(player.uniqueId) }, 60L)
        notifications[player.uniqueId] = Notification(bar, hideTask)
    }

    fun getRoundTotal(playerId: UUID): Int = roundTotals[playerId] ?: 0

    private fun removeNotification(playerId: UUID) {
        val notification = notifications.remove(playerId) ?: return
        notification.hideTask.cancel()
        notification.bar.removeAll()
    }

    private fun clearNotifications() {
        notifications.keys.toList().forEach(::removeNotification)
    }

    private fun forget(block: Block) {
        if (plugin.gameManager.isGameWorld(block.world)) pumpkins.remove(block.position)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockBreak(event: BlockBreakEvent) = forget(event.block)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockPlace(event: BlockPlaceEvent) = forget(event.blockPlaced)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockDestroy(event: BlockDestroyEvent) = forget(event.block)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockBurn(event: BlockBurnEvent) = forget(event.block)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockChange(event: EntityChangeBlockEvent) = forget(event.block)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onPistonExtend(event: BlockPistonExtendEvent) = event.blocks.forEach(::forget)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onPistonRetract(event: BlockPistonRetractEvent) = event.blocks.forEach(::forget)

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) = removeNotification(event.player.uniqueId)

    fun dispose() {
        disposed = true
        EventBus.onMapChanged -= ::handleMapChanged
        HandlerList.unregisterAll(this)
        pumpkins.clear()
        roundTotals.clear()
        clearNotifications()
    }

    private data class Notification(val bar: BossBar, val hideTask: BukkitTask)

    private val Block.position get() = BlockVector3.at(x, y, z)

    companion object {
        private val pumpkinMaterials = setOf(Material.PUMPKIN, Material.CARVED_PUMPKIN, Material.JACK_O_LANTERN)
    }
}
