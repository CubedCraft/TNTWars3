package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.minigame_utilities.toColor
import org.bukkit.Color
import org.bukkit.entity.Player

class ColorCommandParameter(name: String, required: Boolean) : CommandParameter(name, required) {

    companion object {
        private val colorCompletions: Array<String>
        private val hexRegex = Regex("^#[a-fA-F0-9]{0,6}$")
        private val characters = "0123456789abcdef".map { it.toString() }

        init {
            val colors = mutableListOf<String>()
            colors.addAll(Textial.colorMap.keys)
            colors.add("#")
            colors.remove(Textial.Primary.name)
            colors.remove(Textial.Secondary.name)
            colors.remove(Textial.Warning.name)
            colors.remove(Textial.Reset.name)
            colorCompletions = colors.toTypedArray()
        }
    }


    override fun execute(data: CommandData, sender: Player) {
        val s = nextWord(data) ?: return
        try {
            if (s.startsWith("#")) {
                if (!s.matches(hexRegex)) throw CommandError("'&p$s&r' is not a valid color code")
                data.setParam(name, Color.fromRGB(Integer.parseInt(s.substring(1), 16)))
                return
            }
        } catch (e: Exception) {
            throw CommandError("Could not parse color &p${s}")
        }
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        val s = data.consumer.consumeWord().lowercase()
        if (data.consumer.hasNext()) return null
        if (s.startsWith("#")) {
            if (!s.matches(hexRegex)) throw CommandError("'$s' is not a valid color code")
            if (s.length < 7) return characters.map { "$s$it" }
            return null
        }
        return colorCompletions.filter { it.lowercase().startsWith(s) }.toList()
    }
}