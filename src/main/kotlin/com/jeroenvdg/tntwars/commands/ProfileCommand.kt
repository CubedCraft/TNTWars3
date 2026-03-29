package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.commands.BrigadierCommand
import com.jeroenvdg.tntwars.interfaces.ProfileInterface
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

object ProfileCommand : BrigadierCommand {
    override fun create(registrar: Commands) {
        val node = LiteralArgumentBuilder.literal<CommandSourceStack>("profile").executes(::openProfileMenu).build()
        registrar.register(node)
    }

    private fun openProfileMenu(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender
        if(player !is Player) return com.mojang.brigadier.Command.SINGLE_SUCCESS
        ProfileInterface.open(player)
        return com.mojang.brigadier.Command.SINGLE_SUCCESS
    }
}