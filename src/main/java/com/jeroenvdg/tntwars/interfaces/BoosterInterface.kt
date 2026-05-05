package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.tntwars.managers.BoosterManager
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.minigame_utilities.Soundial
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.launchCoroutine
import com.jeroenvdg.minigame_utilities.makeItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class BoosterInterface : IPlayerGUI {
    companion object : GUISingleton<BoosterInterface>("BoosterInterface")
    override val name: String = guiName

    private val boosterItem = makeItem(Material.POTION) {
        named("&e&lTNTWars Booster - 1 Hour")
        flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
        setLore("&aClick to activate this booster!")
    }

    override fun open(player: Player) {
        val user = PlayerManager.instance.get(player) ?: return
        val menu = ChestMenu("Boosters", 4) {
            for (i in user.boosters.indices) {
                addButton(i) {
                    displayItem = boosterItem
                    playSound = false
                    onClick {
                        player.closeInventory()
                        launchCoroutine {
                            val result = BoosterManager.instance.activateBooster(user, user.boosters[i]).await()
                            if (result.isFailure) {
                                player.sendMessage(Textial.msg.format("&wCould not activate the booster"))
                                Soundial.play(player, Soundial.UIFail)
                            }
                        }
                    }
                }
            }
        }
        menu.open(player)
    }

    override fun create() {
    }
}