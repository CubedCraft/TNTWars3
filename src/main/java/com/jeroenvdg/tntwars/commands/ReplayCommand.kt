package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.minigame_utilities.commands.CommandHandler
import com.jeroenvdg.minigame_utilities.commands.builders.SingleCommandBuilder
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.ReplayManager
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.Material
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReplayCommand : CommandHandler() {
    init {
        builder(SingleCommandBuilder("replay") {
            execute { _, player ->
                val user = PlayerManager.instance.get(player) ?: throw CommandError("You must be in the game to view replays")
                if (user.team != Team.Spectator) throw CommandError("You can only view replays while spectating")

                openReplayMenu(player, ReplayManager.instance.listReplays())
            }
        })
    }

    private fun openReplayMenu(player: org.bukkit.entity.Player, replays: List<File>) {
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
                            ReplayManager.instance.startReplay(event.player, replayFile)
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
