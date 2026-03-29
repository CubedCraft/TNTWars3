package com.jeroenvdg.minigame_utilities.commands

import com.jeroenvdg.minigame_utilities.TextialParser
import com.jeroenvdg.minigame_utilities.commands.builders.ArgsConsumer

@Deprecated("Since 1.21.11, use CommandContext")
class CommandData(
    val cmdName: String,
    val originalCommand: String,
    val consumer: ArgsConsumer,
    val textial: TextialParser,
    var currentAction: ICommandAction
) {
    val args = HashMap<String, Any>()


    fun format(message: String) = textial.format(message)
    fun parse(message: String) = textial.parse(message)


    fun setParam(name: String, value: Any?) {
        if (value == null) return
        args[name.lowercase()] = value
    }


    inline fun <reified T> getParam(name: String): T {
        val v = args[name.lowercase()]
        if (v !is T) throw Exception("Could not cast $name to ${T::class.java.name}")
        return v
    }


    inline fun <reified T> getParam(name: String, default: T): T {
        val v = args[name.lowercase()]
        return if (v is T) v else default
    }

    inline fun <reified T> hasParam(name: String) : Boolean {
        val v = args[name.lowercase()]
        return v is T
    }


    fun copy() = CommandData(cmdName, originalCommand, consumer.copy(), textial, currentAction)
}