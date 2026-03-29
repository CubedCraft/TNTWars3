package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandError
import org.bukkit.entity.Player

abstract class CommandParameter(val name: String, val required: Boolean) : Cloneable {
    var isLast = false
    var addToHelpCommand = true; protected set

    abstract fun execute(data: CommandData, sender: Player)
    abstract fun tabComplete(data: CommandData, sender: Player): List<String>?


    protected fun nextWord(cmd: CommandData): String? {
        if (!cmd.consumer.hasNext()) {
            if (required) {
                throw error("None provided")
            } else {
                return null
            }
        }
        return cmd.consumer.consumeWord()
    }


    protected fun error(message: String) = CommandError("&p$name &rparameter: $message")
}