package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.commands.BrigadierCommand
import com.jeroenvdg.tntwars.interfaces.BoosterInterface
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

object BoosterCommand : BrigadierCommand {

    private fun openSettings(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender
        if(player !is Player) return com.mojang.brigadier.Command.SINGLE_SUCCESS
        BoosterInterface.open(player)
        return com.mojang.brigadier.Command.SINGLE_SUCCESS
    }

    override fun create(registrar: Commands) {
        val node = LiteralArgumentBuilder.literal<CommandSourceStack>("booster").executes(::openSettings).build()
        registrar.register(node)
    }

}
