package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandError
import org.bukkit.entity.Player

class OptionsCommandParameter(name: String, required: Boolean, val action: (data: CommandData) -> List<String>) : CommandParameter(name, required) {

    override fun execute(data: CommandData, sender: Player) {
        val s = (nextWord(data) ?: return).lowercase()
        val value = action(data).firstOrNull { it.lowercase() == s } ?: throw CommandError("Could not find &s$s")
        data.setParam(name, value)
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        val s = data.consumer.consumeWord().lowercase()
        if (data.consumer.hasNext()) return null
        return action(data).filter { it.lowercase().startsWith(s) }
    }

}