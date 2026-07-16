package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.Soundial
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.gui.slots.addItem
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.math.ceil

class MasteriesInterface : IPlayerGUI {

    companion object : GUISingleton<MasteriesInterface>("MasteriesInterface")

    override val name get() = guiName

    override fun create() {}

    override fun open(player: Player) {
        val user = PlayerManager.instance.get(player) ?: return
        val rows = 6

        val menu = ChestMenu("Masteries", rows) {
            val pane = makeItem(Material.GRAY_STAINED_GLASS_PANE) { named(" ") }
            for (i in rows * 9 - 9 until rows * 9) {
                addItem(i, pane)
            }

            addItem(3, makeItem(Material.TOTEM_OF_UNDYING) {
                named("&aStair Mastery")
                setLore("&7Kill players using stair cannons to unlock this mastery.")
            })
            addItem(5, makeItem(Material.TOTEM_OF_UNDYING) {
                named("&aFence Mastery")
                setLore("&7Kill players using fence cannons to unlock this mastery.")
            })

            repeat(3) {
                addItem(9 * (1 + it) + 3, makeItem(Material.STONE_BRICK_STAIRS) {
                    amount(it + 1)
                    val romanNumeral = when (it) {
                        0 -> "I"
                        1 -> "II"
                        2 -> "III"
                        else -> "IV"
                    }
                    named("&aStairs $romanNumeral")
                    glow()
                })
            }

            repeat(3) {
                addItem(9 * (1 + it) + 5, makeItem(Material.OAK_FENCE) {
                    amount(it + 1)
                    val romanNumeral = when (it) {
                        0 -> "I"
                        1 -> "II"
                        2 -> "III"
                        else -> "IV"
                    }
                    named("&aFence $romanNumeral")
                    glow()
                })
            }

            addButton(rows * 9 - 5) {
                displayItem = makeItem(Material.BOOK) { named("&9Go Back") }
                playSound = false
                onClick { ProfileInterface.open(player) }
            }
        }

        menu.open(player)
        Soundial.play(player, Soundial.UIOpen)
    }
}