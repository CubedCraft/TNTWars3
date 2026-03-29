package com.jeroenvdg.minigame_utilities.commands.builders.params

import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.builders.ArgsConsumer
import org.bukkit.entity.Player

class StringCommandParameter(name: String, required: Boolean, private val regex: Regex?, private val regexFailMessage: String) : CommandParameter(name, required) {

    override fun execute(data: CommandData, sender: Player) {
        val consumer = data.consumer
        if (!consumer.hasNext()) {
            if (required) {
                throw error("Please provide some text")
            } else {
                return
            }
        }

        val string = extractString(consumer)

        if (regex?.matches(string) == false) {
            throw error(regexFailMessage)
        }

        data.setParam(name, string)
    }

    override fun tabComplete(data: CommandData, sender: Player): List<String>? {
        return try {
            val string = extractString(data.consumer)
            listOf(string)
        } catch (e: Exception) {
            return null
        }
    }


   private fun extractString(consumer: ArgsConsumer): String {
        val string: String

        if (consumer.peek().startsWith('"')) {
            val builder = StringBuilder()
            builder.append(consumer.consumeWord().substring(1))

            try {
                while (true) {
                    val word = consumer.consumeWord()
                    if (word.endsWith('"')) {
                        builder.append(word.subSequence(0, word.length))
                        break
                    }
                    builder.append(word)
                }
            } catch (exception: Exception) {
                throw error("string `&s$builder&r` does not close with a &s\"")
            }

            string = builder.toString()
        } else if (isLast) {
            string = consumer.consumeToEnd()
        } else {
            string = consumer.consumeWord()
        }

        return string
    }
}