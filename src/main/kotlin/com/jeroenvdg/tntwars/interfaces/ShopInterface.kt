package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.SoundHelper
import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.guibuilders.IMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.gui.slots.addItem
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.managers.PlayerStatsManager
import com.jeroenvdg.tntwars.managers.SchematicManager
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import kotlin.math.max

class ShopInterface : IPlayerGUI {
    companion object : GUISingleton<ShopInterface>("ShopInterface")

    override val name get() = guiName

    lateinit var menu: IMenu

    override fun create() {

        val schematicManager = SchematicManager.instance
        schematicManager.load()
        val schematicGroups = schematicManager.groups.filter { it.isReady() }

        val rows = max(schematicGroups.size + 2, 3)
        menu = ChestMenu("Shop", rows) {
            val grayPane = makeItem(Material.GRAY_STAINED_GLASS_PANE) { named(" ") }
            val whitePane = makeItem(Material.WHITE_STAINED_GLASS_PANE) { named(" ") }
            for (i in (rows * 9 - 9) until (rows * 9)) { addItem(i, grayPane) }
            for (i in 0 until 9) { addItem(i, grayPane) }
            for (i in 1 until (rows - 1)) {
                addItem(i * 9, whitePane)
                addItem(i * 9 + 8, whitePane)
            }

            for (groupIndex in schematicGroups.indices) {
                val row = (groupIndex+1) * 9 + 1
                val schematicGroup = schematicGroups[groupIndex]

                addItem(row, makeItem(Material.SEA_LANTERN) {
                    named("&e${schematicGroup.name}")
                    setLore(schematicGroup.description)
                    enchant(Enchantment.LUCK_OF_THE_SEA, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                })

                for (index in schematicGroup.schematics.indices) {
                    val schematic = schematicGroup.schematics[index]
                    addButton(row + index + 1) {
                        playSound = false
                        displayItem = makeItem(schematic.material) {
                            named("&f${schematic.name}")
                            setLore {
                                line("&6Price: &e\u26C3${schematic.price}")
                                empty()
                                line(schematic.description)
                            }
                            if (schematic.enchanted) {
                                enchant(Enchantment.LUCK_OF_THE_SEA, 1)
                                flag(ItemFlag.HIDE_ENCHANTS)
                            }
                        }

                        onClick { event ->
                            event.player.closeInventory()
                            val user = PlayerManager.instance.get(event.player) ?: return@onClick
                            if (user.stats.coins < schematic.price) {
                                SoundHelper.play(user.bukkitPlayer, SoundHelper.Sounds.Fail)
                                event.player.sendMessage(TextHelper.error("You don't have enough coins to buy this"))
                                return@onClick
                            }
                            SoundHelper.play(user.bukkitPlayer, SoundHelper.Sounds.Success)
                            PlayerStatsManager.instance.removeCoins(user, schematic.price)
                            SchematicManager.instance.pasteSchematic(user, schematic)
                        }
                    }
                }
            }
        }
    }

    override fun open(player: Player) {
        menu.open(player)
        SoundHelper.play(player, SoundHelper.Sounds.UIOpen)
    }

}