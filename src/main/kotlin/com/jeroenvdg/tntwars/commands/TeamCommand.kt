package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.commands.BrigadierCommand
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.interfaces.TeamSelector
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

object TeamCommand : BrigadierCommand {
    override fun create(registrar: Commands) {
        val node = LiteralArgumentBuilder.literal<CommandSourceStack>("team").executes(::openTeamMenu).build()
        registrar.register(node)
        registrar.register(LiteralArgumentBuilder.literal<CommandSourceStack>("t").redirect(node).build())
    }

    private fun openTeamMenu(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender
        if(player !is Player) return com.mojang.brigadier.Command.SINGLE_SUCCESS
        if (!GameManager.instance.teamSelectMode.isJoinable) {
            throw CommandError("You cannot open the teamselector right now")
        }
        TeamSelector.open(player)
        return com.mojang.brigadier.Command.SINGLE_SUCCESS
    }
}