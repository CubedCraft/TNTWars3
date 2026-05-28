package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.GenericItemListener
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReplayInterface : IPlayerGUI {
    companion object : GUISingleton<ReplayInterface>("ReplayInterface") {
        lateinit var replayItem: ItemStack
            private set
    }

    override val name get() = guiName

    override fun create() {
        replayItem = makeItem(Material.MUSIC_DISC_WAIT) {
            named("&aReplays &7(Right Click)")
            setLore("&7View recorded games")
            withPersistentData(GenericItemListener.guiKey, name)
            withPersistentData(GenericItemListener.movableKey, PersistentDataType.BOOLEAN, false)
            withPersistentData(GenericItemListener.droppableKey, PersistentDataType.BOOLEAN, false)
        }
    }

    override fun open(player: Player) {
        val user = PlayerManager.instance.get(player)
        if (user?.team != Team.Spectator) {
            player.sendMessage(Textial.msg.parse("&cYou can only view replays while spectating"))
            return
        }

        openReplayMenu(player, TNTWars.instance.replayManager.listReplays())
    }

    private fun openReplayMenu(player: Player, replays: List<File>) {
        val displayReplays = replays.take(54)
        val rows = 6

        val menu = ChestMenu("Replays", rows) {
            for ((index, replayFile) in displayReplays.withIndex()) {
                addButton(index) {
                    displayItem = makeItem(Material.FILLED_MAP) {
                        named("&a${replayFile.nameWithoutExtension}")
                        setLore {
                            line("&7Recorded: &f${formatModifiedTime(replayFile)}")
                            line("&7Size: &f${replayFile.length() / 1024} KB")
                            empty()
                            line("&eClick to play")
                        }
                    }

                    onClick { event ->
                        event.player.closeInventory()
                        try {
                            TNTWars.instance.replayManager.startReplay(event.player, replayFile)
                        } catch (exception: Exception) {
                            event.player.sendMessage(Textial.msg.parse("&cCould not start replay: ${exception.message}"))
                        }
                    }
                }
            }
        }

        menu.open(player)
    }

    private fun formatModifiedTime(file: File): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(file.lastModified()))
    }
}
