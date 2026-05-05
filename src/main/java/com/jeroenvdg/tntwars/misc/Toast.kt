package com.jeroenvdg.tntwars.misc

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import io.papermc.paper.advancement.AdvancementDisplay
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import java.util.*

class Toast {
    private val message: String
    private val style: AdvancementDisplay.Frame
    private val icon: Material
    private val key: NamespacedKey = NamespacedKey(TNTWars.instance, UUID.randomUUID().toString())

    private constructor(icon: Material, message: String, style: AdvancementDisplay.Frame) {
        this.icon = icon
        this.message = message
        this.style = style
    }

    fun start(player: TNTWarsPlayer) {
        createAdvancement()
        grantAdvancement(player)

        Bukkit.getScheduler().runTaskLater(TNTWars.instance, Runnable{
            revokeAdvancement(player)
        }, 10L)
    }

    private fun createAdvancement() {
        Bukkit.getUnsafe().loadAdvancement(
            key, """
        {
            "criteria": {
                "trigger": {
                    "trigger": "minecraft:impossible"
                }
            },
            "display": {
                "icon": {
                    "id": "minecraft:${icon.key.key.lowercase()}"
                },
                "title": {
                    "text": "${message.replace("|", "\n")}"
                },
                "description": {
                    "text": ""
                },
                "background": "minecraft:textures/gui/advancements/backgrounds/adventure.png",
                "frame": "${style.name.lowercase()}",
                "announce_to_chat": false,
                "show_toast": true,
                "hidden": true
            },
            "requirements": [
                [
                    "trigger"
                ]
            ]
        }
        """.trimIndent()
        )
    }

    private fun grantAdvancement(player: TNTWarsPlayer) {
        player.bukkitPlayer.getAdvancementProgress(Bukkit.getAdvancement(key)!!).awardCriteria("trigger")
    }

    private fun revokeAdvancement(player: TNTWarsPlayer) {
        player.bukkitPlayer.getAdvancementProgress(Bukkit.getAdvancement(key)!!).revokeCriteria("trigger")
    }

    companion object {
        fun show(player: TNTWarsPlayer, icon: Material, message: String, style: AdvancementDisplay.Frame) {
            Toast(icon, message, style).start(player)
        }
    }
}