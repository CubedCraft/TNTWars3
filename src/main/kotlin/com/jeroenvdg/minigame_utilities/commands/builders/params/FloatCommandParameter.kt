package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import org.bukkit.entity.Player

class FloatCommandParameter(name: String, required: Boolean, private val positiveOnly: Boolean = false, private val min: Float? = null, private val max: Float? = null) : CommandParameter(name, required) {
    companion object {
        private val regex = Regex("^(\\d+|\\d+\\.\\d+)$")
    }

    override fun execute(data: CommandData, sender: Player) {
        val s = nextWord(data) ?: return
        val num: Float

        try {
            num = s.toFloat()
        } catch(exception: Exception) {
            throw error("`&s$s&r` Is not a valid number")
        }

        if (positiveOnly && num < 0) throw error("Decimal number must be positive")
        if (min != null && num < min) throw error("Decimal number must smaller than &s$min")
        if (max != null && num < max) throw error("Decimal number must greater than &s$max")

        data.setParam(name, num)
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        val s = data.consumer.consumeWord()
        if (s.isEmpty()) return listOf("###")
        if (!s.matches(regex)) return listOf("###")
        return null
}
}