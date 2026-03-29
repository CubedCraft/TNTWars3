package com.jeroenvdg.minigame_utilities.commands.builders

import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.TextialParser
import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.minigame_utilities.commands.HelpCommand
import com.jeroenvdg.minigame_utilities.commands.ICommandAction
import com.jeroenvdg.minigame_utilities.commands.builders.params.CommandParameter
import com.jeroenvdg.minigame_utilities.commands.builders.params.CommandValidator
import com.jeroenvdg.minigame_utilities.commands.builders.params.IntCommandParameter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission

@Deprecated("Since 1.21.11, use BrigadierCommand()")
class CommandBuilder(override val name: String, callback: CommandBuilder.() -> Unit) : ICommandBuilder {
    override var depth = 0
    override var permissions: Permission? =null; private set

    private var hidden = false
    private var description: String? = null
    private val commands = ArrayList<ICommandBuilder>()
    private val validators = ArrayList<CommandParameter>();

    init {
        callback(this)
    }

    fun helpCommand() {
        commands.add(object : ICommandBuilder {
            override val depth = this@CommandBuilder.depth+1
            override val name = "help"
            override val permissions: Permission? = null
            private val pageParam = IntCommandParameter("page", false, min = 1)

            override fun build(parent: ICommandAction?): ICommandAction {
                return HelpCommand(parent as Action, pageParam)
            }
        })
    }

    fun hidden() {
        hidden = true
    }

    fun permissions(permission: Permission) {
        this.permissions = permission
    }

    fun permissions(permissions: String) {
        this.permissions = Permission(permissions)
    }

    fun subCommand(name: String, callback: SingleCommandBuilder.() -> Unit) {
        val cmd = SingleCommandBuilder(name, callback)
        cmd.depth = depth+1
        commands.add(cmd)
    }

    fun commandGroup(name: String, callback: CommandBuilder.() -> Unit) {
        val cmd = CommandBuilder(name, callback)
        cmd.depth = depth+1
        commands.add(cmd)
    }

    fun validator(action: SingleCommandAction) {
        validators.add(CommandValidator("", true, action))
    }

    fun validator(validator: CommandValidator) {
        validators.add(validator)
    }

    override fun build(parent: ICommandAction?): ICommandAction {
        return Action(parent, this)
    }

    class Action(override val parent: ICommandAction?, builder: CommandBuilder) : ICommandAction {
        override val hidden = builder.hidden
        override val name = builder.name
        override val description = builder.description
        override val permissions = builder.permissions

        val visibleCommands: Array<ICommandAction>
        private val subcommands: Map<String, ICommandAction>
        private var helpCommand: ICommandAction
        private val validators = builder.validators.toTypedArray()

        init {
            subcommands = HashMap()

            val commands = ArrayList<ICommandAction>(builder.commands.size)
            var helpcmd: ICommandAction? = null

            for (command in builder.commands) {
                val name = command.name.lowercase()
                subcommands[name] = command.build(this)
                commands.add(subcommands[name]!!)
                if (name == "help") helpcmd = subcommands[name]
            }

            if (helpcmd == null) helpcmd = subcommands.values.first()
            helpCommand = helpcmd
            this.visibleCommands = commands.filter { !it.hidden }.toTypedArray()
        }

        override fun execute(data: CommandData, sender: Player) {
            if (permissions != null && !sender.hasPermission(permissions)) {
                throw CommandError("You are not allowed to execute this command")
            }

            val argument = if (data.consumer.hasNext()) data.consumer.consumeWord() else ""
            val subcmd = subcommands[argument.lowercase()]

            for (validator in validators) validator.execute(data, sender)

            if (argument.isEmpty()) {
                data.currentAction = helpCommand
                return helpCommand.execute(data, sender)
            }

            if (subcmd != null && (subcmd.permissions == null || sender.hasPermission(subcmd.permissions!!))) {
                data.currentAction = subcmd
                return subcmd.execute(data, sender)
            }

            val fullCommand = getFullCommand(data, sender)
            sender.sendMessage(Component.text()
                .append(TextHelper.deserialize("Unknown subcommand /&s$fullCommand $argument")).appendNewline()
                .append(TextHelper.deserialize(" Please run /&p$fullCommand help").setCommand("/$fullCommand help")))
        }

        override fun tabCompletion(data: CommandData, sender: Player): List<String> {
            if (!data.consumer.hasNext()) return emptyList()
            val s = data.consumer.consumeWord().lowercase()
            val cmd = subcommands[s]

            for (validator in validators) validator.tabComplete(data, sender)

            return if (cmd != null && (cmd.permissions == null || sender.hasPermission(cmd.permissions!!))) {
                if (data.consumer.hasNext()) {
                    data.currentAction = cmd
                    cmd.tabCompletion(data, sender)
                }
                else emptyList()
            } else {
                if (data.consumer.hasNext()) return emptyList()
                else visibleCommands
                    .filter { it.name.lowercase().startsWith(s) && (it.permissions == null || sender.hasPermission(it.permissions!!)) }
                    .map { it.name }
            }
        }

        override fun getFullCommand(data: CommandData, sender: Player): String {
            return if (parent == null) name
            else "${parent.getFullCommand(data, sender)} $name"
        }

        override fun buildHelpComponent(textial: TextialParser, fullCommand: String): TextComponent {
            return if (description == null) textial.parse("&p$name").setCommand("/$fullCommand $name")
            else textial.parse("&p$name &r- $description").setCommand("/$fullCommand $name")
        }
    }
}