package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.builders.SingleCommandAction
import org.bukkit.entity.Player

open class CommandValidator(name: String, required: Boolean, private val action: SingleCommandAction) : CommandParameter(name, required) {

    init {
        addToHelpCommand = false
    }

    override fun execute(data: CommandData, sender: Player) {
        action(data, sender)
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        action(data, sender)
        return null
    }
}