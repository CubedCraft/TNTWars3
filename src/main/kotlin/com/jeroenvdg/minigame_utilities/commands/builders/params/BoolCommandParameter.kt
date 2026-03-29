package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.parseBoolean
import org.bukkit.entity.Player

class BoolCommandParameter(name: String, required: Boolean, private val tabCompletionType: TabCompletionType) : CommandParameter(name, required) {

    override fun execute(data: CommandData, sender: Player) {
        val s = nextWord(data) ?: return
        val b = parseBoolean(s) ?: throw error("&s'$s' &ris not a boolean")
        data.setParam(name, b)
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        val s = data.consumer.consumeWord()
        if (data.consumer.hasNext()) return null
        val b = parseBoolean(s)
        if (b != null) return null
        return listOf(tabCompletionType.trueText, tabCompletionType.falseText)
    }


    enum class TabCompletionType(val trueText: String, val falseText: String) {
        TrueFalse("True", "False"),
        YesNo("Yes", "No"),
        OnOff("On", "Off");
    }
}