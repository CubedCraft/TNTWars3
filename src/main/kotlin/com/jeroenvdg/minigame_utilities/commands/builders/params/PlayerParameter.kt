package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class PlayerParameter(name: String, required: Boolean, private val online: Boolean = true, private val allowAll: Boolean = false) : CommandParameter(name, required) {
    override fun execute(data: CommandData, sender: Player) {
        val s = nextWord(data) ?: return

        if (allowAll && s.lowercase() == "@a") {
            return data.setParam(name, Bukkit.getOnlinePlayers())
        }

        val player: OfflinePlayer? = if (online) Bukkit.getPlayer(s) else Bukkit.getOfflinePlayer(s)
        if (player == null) throw error("Player $name was not found")
        data.setParam(name, player)
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        val s = data.consumer.consumeWord().lowercase()
        if (data.consumer.hasNext()) return null
        val players = Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        if (allowAll) players.add("@a")
        return players.filter { it.lowercase().startsWith(s) }
    }
}