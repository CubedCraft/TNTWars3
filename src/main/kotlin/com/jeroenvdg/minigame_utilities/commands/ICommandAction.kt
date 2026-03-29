package com.jeroenvdg.minigame_utilities.commands

import com.jeroenvdg.minigame_utilities.TextialParser
import net.kyori.adventure.text.TextComponent
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission

@Deprecated("Since 1.21.11, use BrigadierCommand()")
interface ICommandAction {

    val hidden: Boolean
    val parent: ICommandAction?
    val name: String
    val description: String?
    val permissions: Permission?

    fun execute(data: CommandData, sender: Player)
    fun tabCompletion(data: CommandData, sender: Player): List<String>
    fun getFullCommand(data: CommandData, sender: Player): String
    fun buildHelpComponent(textial: TextialParser, fullCommand: String): TextComponent
}