package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandError
import org.bukkit.entity.Player

class EnumCommandParameter<T : Enum<T>>(name: String, required: Boolean, cls: Class<T>) : CommandParameter(name, required) {

    private val values: Array<T> = cls.enumConstants

    override fun execute(data: CommandData, sender: Player) {
        val s = (nextWord(data) ?: return).lowercase()
        val enumValue = values.firstOrNull { it.name.lowercase() == s } ?: throw CommandError("&s$s &rIs not a valid key")
        data.setParam(name, enumValue)
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        val s = data.consumer.consumeWord().lowercase()
        if (data.consumer.hasNext()) return null // Basically an optimization
        return values.filter { it.name.lowercase().startsWith(s) }.map { it.name }
    }
}