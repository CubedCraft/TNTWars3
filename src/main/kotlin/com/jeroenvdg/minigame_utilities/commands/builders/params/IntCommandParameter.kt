package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import org.bukkit.entity.Player

class IntCommandParameter(name: String, required: Boolean, private val positiveOnly: Boolean = false, private val min: Int? = null, private val max: Int? = null) : CommandParameter(name, required) {
    companion object {
        private val regex = Regex("^\\d+$")
    }

    override fun execute(data: CommandData, sender: Player) {
        val s = nextWord(data) ?: return
        val num: Int

        try {
            num = Integer.parseInt(s)
        } catch(exception: Exception) {
            throw error("`&s$s&r` Is not a valid number")
        }

        if (positiveOnly && num < 0) throw error("Number must be positive")
        if (min != null && num < min) throw error("Number must smaller than &s$min")
        if (max != null && num < max) throw error("Number must greater than &s$max")

        data.setParam(name, num)
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        val s = data.consumer.consumeWord()
        if (s.isEmpty()) return listOf("###")
        if (!s.matches(regex)) return listOf("###")
        return null
    }
}