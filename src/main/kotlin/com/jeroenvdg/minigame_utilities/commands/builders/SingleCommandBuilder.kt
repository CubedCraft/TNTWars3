package com.jeroenvdg.minigame_utilities.commands.builders

import com.jeroenvdg.minigame_utilities.TextialParser
import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.minigame_utilities.commands.ICommandAction
import com.jeroenvdg.minigame_utilities.commands.builders.params.*
import net.kyori.adventure.text.TextComponent
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import java.util.*

typealias SingleCommandAction = (data: CommandData, sender: Player) -> Unit

@Deprecated("Since 1.21.11, use BrigadierCommand")
class SingleCommandBuilder(override val name: String, callback: SingleCommandBuilder.() -> Unit) : ICommandBuilder {

    companion object {
        val paramNameRegex = Regex("^[a-zA-Z0-9 ]{1,42}$")
    }

    private var hidden = false;
    private var description: String? = null
    private var executer: SingleCommandAction? = null
    val arguments = ArrayList<CommandParameter>()
    override var depth = 0
    override var permissions: Permission? = null; private set

    init {
        callback(this)
    }

    fun hidden() {
        hidden = true
    }

    fun permissions(permissions: Permission) {
        this.permissions = permissions
    }

    fun permissions(permissions: String) {
        this.permissions = Permission(permissions)
    }

    fun add(parameter: CommandParameter) {
        if (!paramNameRegex.matches(parameter.name)) throw Exception("parameter name `${parameter.name}` is not valid!")
        arguments.add(parameter)
    }

    fun validator(action: SingleCommandAction) {
        add(CommandValidator(UUID.randomUUID().toString().replace('-', ' '), true, action))
    }

    fun validator(parameter: CommandParameter) {
        add(parameter)
    }

    fun param(parameter: CommandParameter) {
        add(parameter)
    }

    fun stringParam(name: String, required: Boolean, regex: Regex? = null, regexFailMessage: String? = null) {
        add(StringCommandParameter(name, required, regex, regexFailMessage ?: ""))
    }

    fun intParam(name: String, required: Boolean, min: Int? = null, max: Int? = null, positiveOnly: Boolean? = null) {
        add(IntCommandParameter(name, required, positiveOnly ?: false, min, max))
    }

    fun floatParam(name: String, required: Boolean, min: Float? = null, max: Float? = null, positiveOnly: Boolean? = null) {
        add(FloatCommandParameter(name, required, positiveOnly ?: false, min, max))
    }

    fun boolParam(name: String, required: Boolean, tabCompletionType: BoolCommandParameter.TabCompletionType = BoolCommandParameter.TabCompletionType.TrueFalse) {
        add(BoolCommandParameter(name, required, tabCompletionType))
    }

    fun playerParam(name: String, required: Boolean, online: Boolean = true, allowAll: Boolean = false) {
        add(PlayerParameter(name, required, online, allowAll))
    }

    fun idParam(name: String, required: Boolean) {
        stringParam(name, required, regex = Regex("^[a-zA-Z0-9_]{3,20}$"), regexFailMessage = "Ids can only contain numbers, letters and underscores")
    }

    fun colorParam(name: String, required: Boolean) {
        add(ColorCommandParameter(name, required))
    }

    inline fun <reified T : Enum<T>> enumParam(name: String, required: Boolean) {
        add(EnumCommandParameter(name, required, T::class.java))
    }

    fun optionsParam(name: String, required: Boolean, action: (data: CommandData) -> List<String>) {
        add(OptionsCommandParameter(name, required, action))
    }

    fun description(description: String) {
        this.description = description
    }

    fun execute(action: SingleCommandAction) {
        executer = action
    }

    override fun build(parent: ICommandAction?): ICommandAction {
        return Action(parent, this)
    }

    class Action(override val parent: ICommandAction?, builder: SingleCommandBuilder) : ICommandAction {
        private val params = builder.arguments.toTypedArray()
        private val executer: SingleCommandAction = builder.executer!!

        override val hidden = builder.hidden
        override val name = builder.name
        override val description = builder.description
        override val permissions = builder.permissions

        init {
            params.lastOrNull()?.isLast = true
        }

        override fun execute(data: CommandData, sender: Player) {
            if (permissions != null && !sender.hasPermission(permissions)) {
                throw CommandError("You are not allowed to execute this command")
            }

            for (arg in params) arg.execute(data, sender)
            executer(data, sender)
        }

        override fun tabCompletion(data: CommandData, sender: Player): List<String> {
            for (arg in params) {
                val result = arg.tabComplete(data, sender) ?: continue
                if (data.consumer.hasNext()) continue
                return result
            }

            return emptyList()
        }

        override fun getFullCommand(data: CommandData, sender: Player): String {
            var n = parent?.getFullCommand(data, sender) ?: name

            n += " $name"
            data.consumer.consumeWord()

            for (arg in params) {
                n += " ${data.getParam<Any>(arg.name)}"
            }

            return n
        }

        override fun buildHelpComponent(textial: TextialParser, fullCommand: String): TextComponent {
            val builder = StringBuilder()
            builder.append("&p$name")
            if (params.isNotEmpty()) builder.append(" &s").append(params.filter { it.addToHelpCommand }.joinToString(" ") { "${if (it.required) "(" else "["}${it.name}${if (it.required) ")" else "]"}" })
            if (description != null) builder.append(" &r- $description")
            return if (params.isEmpty()) textial.parse(builder.toString()).setCommand("/$fullCommand $name")
            else textial.parse(builder.toString()).setSuggestion("/$fullCommand $name")
        }
    }
}