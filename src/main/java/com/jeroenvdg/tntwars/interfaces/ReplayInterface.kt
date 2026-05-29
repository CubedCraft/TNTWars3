package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.gui.guibuilders.HopperMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.GenericItemListener
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.pondwader.replay_engine.replay.GameReplay
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
        val displayReplays = replays.take(5)

        val menu = HopperMenu("Replays") {
            for ((index, replayFile) in displayReplays.withIndex()) {
                addButton(index) {
                    displayItem = replayDisplayItem(Material.FILLED_MAP, replayFile)

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
        loadReplayMeta(player, displayReplays)
    }

    private fun loadReplayMeta(player: Player, displayReplays: List<File>) {
        val plugin = TNTWars.instance
        val inventory = player.openInventory.topInventory

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val replayMaps = displayReplays.mapNotNull { replayFile ->
                try {
                    val mapId = GameReplay.readHeader(replayFile).mapId ?: return@mapNotNull null
                    val replayMap = plugin.mapManager.find(mapId) ?: return@mapNotNull null
                    replayMap
                } catch (_: Exception) {
                    null
                }
            }

            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                if (player.openInventory.topInventory != inventory) return@Runnable

                for ((index, replayMap) in replayMaps.withIndex()) {
                    val material = replayMap.itemMaterial.takeUnless { it == Material.AIR } ?: continue
                    inventory.setItem(index, replayDisplayItem(material, displayReplays[index], replayMap.name))
                }
            })
        })
    }

    private fun replayDisplayItem(material: Material, replayFile: File, mapName: String? = null): ItemStack {
        return makeItem(material) {
            named("&a${replayFile.nameWithoutExtension}")
            setLore {
                if (mapName != null) line("&7Map: &f$mapName")
                line("&7Recorded: &f${formatModifiedTime(replayFile)}")
                empty()
                line("&eClick to play")
            }
        }
    }

    private fun formatModifiedTime(file: File): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(file.lastModified()))
    }
}
