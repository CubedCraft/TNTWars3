package com.jeroenvdg.minigame_utilities.commands.builders

import com.jeroenvdg.minigame_utilities.commands.ICommandAction
import org.bukkit.permissions.Permission

@Deprecated("Since 1.21.11, use BrigadierCommand()")
interface ICommandBuilder {

    val depth: Int
    val name: String
    val permissions: Permission?

    fun build(parent: ICommandAction?): ICommandAction
}