package com.jeroenvdg.minigame_utilities.commands

import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.TextialParser
import com.jeroenvdg.minigame_utilities.commands.builders.ArgsConsumer
import com.jeroenvdg.minigame_utilities.commands.builders.ICommandBuilder
import net.kyori.adventure.text.Component
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

@Deprecated("Since 1.21.11, use BrigadierCommand()")
abstract class CommandHandler : CommandExecutor, TabCompleter {
    var commandAction: ICommandAction? = null; private set
    val name get() = commandAction!!.name
    var textial: TextialParser = Textial.cmd


    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        try {
            if (sender !is Player) {
                sender.sendMessage(Component.text("Only players can execute this command!"))
                return true
            }

            val params = args.joinToString(" ")
            val data = CommandData(command.name,  "${command.name} $params", ArgsConsumer(args), textial, commandAction!!)
            commandAction!!.execute(data, sender)
        }
        catch (e: CommandError) {
            sender.sendMessage(format(e.message ?: "An error occurred."))
        }
        catch (e: Exception) {
            sender.sendMessage(format("An error occurred while executing this command."))
            Debug.error(e)
        }

        return true
    }


    protected fun builder(builder: ICommandBuilder) {
        commandAction = builder.build(null)
    }


    fun format(msg: String) = textial.format(msg)
    fun parse(msg: String) = textial.parse(msg)


    fun notImplemented(arg: CommandData, sender: CommandSender) {
        sender.sendMessage(format("Command was not implemented"))
    }
}