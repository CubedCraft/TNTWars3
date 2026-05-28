package xyz.pondwader.replay_engine.replay

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

internal class ReplayViewerUi(private val replay: GameReplay) {
    private var actionBarTask: BukkitTask? = null

    fun start() {
        installControls()
        actionBarTask = Bukkit.getScheduler().runTaskTimer(replay.plugin, Runnable {
            if (!replay.ended) sendProgressActionBar()
        }, 0L, 20L)
    }

    fun stop() {
        actionBarTask?.cancel()
        actionBarTask = null
    }

    fun updatePlaybackState() {
        val item = if (replay.paused) {
            namedItem(Material.LIME_DYE, "Resume Replay", NamedTextColor.GREEN)
        } else {
            namedItem(Material.RED_DYE, "Pause Replay", NamedTextColor.RED)
        }
        replay.viewer.inventory.setItem(PLAY_PAUSE_SLOT, item)
    }

    fun openPlayerTeleportMenu() {
        val menu = ReplayPlayerMenu(replay::teleportToReplayPlayer, replay.replayPlayers())
        replay.viewer.openInventory(menu.inventory)
    }

    private fun installControls() {
        replay.viewer.inventory.clear()
        replay.viewer.inventory.setItem(
            PLAYER_COMPASS_SLOT,
            namedItem(Material.COMPASS, "Player Teleporter", NamedTextColor.GREEN)
        )
        updatePlaybackState()
        replay.viewer.inventory.setItem(EXIT_SLOT, namedItem(Material.BARRIER, "Exit Replay", NamedTextColor.RED))
    }

    private fun namedItem(material: Material, name: String, color: NamedTextColor): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
        }
        return item
    }

    private fun sendProgressActionBar() {
        var actionBar = Component.text("Watching Replay", NamedTextColor.DARK_AQUA)
            .append(Component.text(" • ", NamedTextColor.DARK_GRAY))
            .append(Component.text(formatReplayTime(replay.currentTick), NamedTextColor.WHITE))

        if (replay.paused) {
            actionBar = actionBar.append(Component.text(" (paused)", NamedTextColor.GRAY))
        }

        replay.viewer.sendActionBar(actionBar)
    }

    private fun formatReplayTime(tick: Long): String {
        val totalSeconds = tick / 20L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d".format(minutes, seconds)
    }

    companion object {
        const val PLAYER_COMPASS_SLOT = 0
        const val PLAY_PAUSE_SLOT = 4
        const val EXIT_SLOT = 8
    }
}
