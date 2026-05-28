package xyz.pondwader.replay_engine.replay

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

internal class ReplayControlListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onPlayerInteract(event: PlayerInteractEvent) {
        val replay = GameReplay.getReplay(event.player) ?: return
        event.isCancelled = true
        handleControl(event.player.inventory.heldItemSlot, replay)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onInventoryClick(event: InventoryClickEvent) {
        val menu = event.inventory.holder as? ReplayPlayerMenu
        if (menu != null) {
            event.isCancelled = true
            menu.handleClick(event)
            return
        }

        val player = event.whoClicked as? Player ?: return
        val replay = GameReplay.getReplay(player) ?: return
        event.isCancelled = true
        if (event.clickedInventory != player.inventory) return
        handleControl(event.slot, replay)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is ReplayPlayerMenu) {
            event.isCancelled = true
            return
        }

        val player = event.whoClicked as? Player ?: return
        if (GameReplay.isReplayViewer(player)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (GameReplay.isReplayViewer(event.player)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        if (GameReplay.isReplayViewer(event.player)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        GameReplay.getReplay(event.player)?.stop()
    }

    private fun handleControl(slot: Int, replay: GameReplay) {
        when (slot) {
            ReplayViewerUi.PLAYER_COMPASS_SLOT -> replay.viewerUi.openPlayerTeleportMenu()
            ReplayViewerUi.PLAY_PAUSE_SLOT -> if (replay.paused) replay.resume() else replay.pause()
            ReplayViewerUi.EXIT_SLOT -> replay.stop()
        }
    }
}
