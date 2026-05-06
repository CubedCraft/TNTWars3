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

class AchievementsInterface : IPlayerGUI {

    companion object : GUISingleton<AchievementsInterface>("AchievementsInterface")
    override val name get() = guiName

    override fun create() { }

    override fun open(player: Player) {
        val user = PlayerManager.instance.get(player) ?: return
        val achievements = AchievementsManager.instance.enabledAchievements
        val rows = 1 + ceil(achievements.size / 9f).toInt()

        val menu = ChestMenu("Achievements", rows) {
            val pane = makeItem(Material.GRAY_STAINED_GLASS_PANE) { named(" ") }
            for (i in rows * 9 - 9 until rows * 9) { addItem(i, pane) }

            val achievementCompletedMaterial = Material.EXPERIENCE_BOTTLE
            val achievementUncompletedMaterial = Material.GLASS_BOTTLE

            for (achievement in achievements) {
                val hasAchievement = user.achievements[achievement.index] != null
                val material = if (hasAchievement) achievementCompletedMaterial else achievementUncompletedMaterial
                val item = makeItem(material) {
                    named("&${if (hasAchievement) 'a' else 'c'}${achievement.title}")
                    setLore {
                        for (line in achievement.description.split("\n")) {
                            line(line)
                        }
                    }
                }

                addItem(achievement.index, item)
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