package com.jeroenvdg.minigame_utilities.commands

import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.TextialParser
import com.jeroenvdg.minigame_utilities.commands.builders.CommandBuilder
import com.jeroenvdg.minigame_utilities.commands.builders.params.IntCommandParameter
import com.jeroenvdg.minigame_utilities.commands.builders.setCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.entity.Player

val chars = arrayOf(
    '┣',
    '┃',
    '┗'
)


class HelpCommand(override val parent: CommandBuilder.Action, private val pageParam: IntCommandParameter, private val pageSize: Int = 10) : ICommandAction {
    override val hidden = false
    override val name = "help"
    override val description = "Get all the info about this command"
    override val permissions = null


    override fun execute(data: CommandData, sender: Player) {
        pageParam.execute(data, sender)

        val fullCommand = parent.getFullCommand(data.copy(), sender)
        val subCommands = parent.visibleCommands.filter { it.permissions == null || sender.hasPermission(it.permissions!!) }

        val index = data.getParam<Int>("page", 1)
        val pageData = PageData(index, pageSize -3, subCommands.size)

        if (pageData.isInvalidPage) return sender.sendMessage(TextHelper.deserialize("Help page &p$index &rdoes not exist."))

        val header = Component.text()
            .append(TextHelper.deserialize("/"))
            .append(Component.text(fullCommand)
                .setCommand("/$fullCommand help")
                .color(data.textial.primary.color))
            .append(Component.text("  "))

        if (pageData.pageCount > 1) {
            header.append(pageData.pageTextComponent("/$fullCommand help _index_"))
        }

        sender.sendMessage(paginate(header.build(), pageData){ i ->
            return@paginate subCommands[i].buildHelpComponent(data.textial, fullCommand)
        })
    }


    override fun tabCompletion(data: CommandData, sender: Player): List<String> {
        return if (!data.consumer.hasNext()) parent.visibleCommands.map { it.name } else emptyList()
    }


    override fun getFullCommand(data: CommandData, sender: Player): String {
        return parent.getFullCommand(data, sender) + " help"
    }


    override fun buildHelpComponent(textial: TextialParser, fullCommand: String): TextComponent {
        return textial.parse("&p$name &s[page] &r- $description").setCommand("/$fullCommand $name")
    }
}


fun parseBoolean(arg: String) = when (arg.lowercase()) {
    "true" -> true
    "false" -> false
    "t" -> true
    "f" -> false
    "yes" -> true
    "no" -> false
    "y" -> true
    "n" -> false
    else -> null
}