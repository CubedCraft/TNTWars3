package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.commands.BrigadierCommand
import com.jeroenvdg.tntwars.interfaces.SettingsInterface
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

object SettingsCommand : BrigadierCommand {

    override fun create(registrar: Commands) {
        val node = LiteralArgumentBuilder.literal<CommandSourceStack>("settings").executes(::openSettings).build()
        registrar.register(node)
    }

    private fun openSettings(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender
        if(player !is Player) return Command.SINGLE_SUCCESS
        SettingsInterface.open(player)
        return Command.SINGLE_SUCCESS
    }

}
