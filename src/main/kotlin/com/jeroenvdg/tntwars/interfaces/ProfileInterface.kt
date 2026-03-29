package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.SoundHelper
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.gui.slots.addItem
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.listeners.GenericItemListener
import com.jeroenvdg.tntwars.managers.achievements.AchievementsManager
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.max

class ProfileInterface : IPlayerGUI {

    companion object : GUISingleton<ProfileInterface>("ProfileInterface") {
        fun makeProfileItem(player: Player) = makeItem(Material.PLAYER_HEAD) {
            named("&aProfile &7(Right Click)")
            withPersistentData(GenericItemListener.guiKey, guiName)
            withPersistentData(GenericItemListener.movableKey, PersistentDataType.BOOLEAN, false)
            withPersistentData(GenericItemListener.droppableKey, PersistentDataType.BOOLEAN, false)

            skullMeta.owningPlayer = player
        }
    }

    private lateinit var boostItem: ItemStack
    private lateinit var hatItem: ItemStack
    private lateinit var particlesItem: ItemStack
    override val name get() = guiName

    override fun create() {
        boostItem = makeItem(Material.POTION) {
            named("&aBoosters")
            setLore {
                line("Your TNTWars Boosters will")
                line("be listed here")
            }
            flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
        }

        hatItem = makeItem(Material.DIAMOND_HELMET) {
            named("&aHats")
            setLore("Donator hats")
            flag(ItemFlag.HIDE_ATTRIBUTES)
        }

        particlesItem = makeItem(Material.ARROW) {
            named("&aParticles")
            setLore("Donator only particle effects")
        }
    }

    override fun open(player: Player) {
        val user = PlayerManager.instance.get(player) ?: return
        val stats = user.stats

        val profileItem = makeItem(Material.PLAYER_HEAD) {
            named("&aMy Profile")
            setLore {
                val rank = user.getRank()
                line("Level: &a${rank.replace("[", "").replace("]", "")}")
                line("Wins: &a${stats.wins}")
                line("Kills: &a${stats.kills}")
                line("Deaths: &a${stats.deaths}")
                line("K/D: &a${(String.format("%.${2}f", stats.kills / max(1f, stats.deaths.toFloat())))}")
            }
            flag(ItemFlag.HIDE_ATTRIBUTES)
            skullMeta.owningPlayer = player
        }

        val achievementItem = makeItem(Material.BOOK) {
            named("&aAchievements")
            setLore {
                line("You can find all your achievements here")
                line("You have completed: &a${user.achievements.count { it != null }}/${AchievementsManager.instance.enabledAchievements.size}")
            }
        }

        val menu = ChestMenu("Profile", 3) {
            val pane = makeItem(Material.GRAY_STAINED_GLASS_PANE) { named("") }
            for (i in (2*9) until (3*9)) addItem(i, pane)

            addItem(4, profileItem)

            addButton(10) {
                displayItem = boostItem
                onClick { event ->
                    BoosterInterface.open(event.player)
                }
            }

            addButton(12) {
                displayItem = hatItem
                onClick { event ->
                    event.player.closeInventory()
                    event.player.performCommand("hats")
                }
            }

            addButton(14) {
                displayItem = particlesItem
                onClick { event ->
                    event.player.closeInventory()
                    event.player.performCommand("pp gui")
                }
            }

            addButton(16) {
                displayItem = achievementItem
                playSound = false
                onClick { event ->
                    AchievementsInterface.open(event.player)
                }
            }
        }

        menu.open(player)
        SoundHelper.play(player, SoundHelper.Sounds.UIOpen)
    }

}