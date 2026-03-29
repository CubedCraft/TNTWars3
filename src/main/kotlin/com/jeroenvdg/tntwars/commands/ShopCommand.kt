package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.commands.BrigadierCommand
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.tntwars.interfaces.ShopInterface
import com.jeroenvdg.tntwars.player.PlayerManager
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

object ShopCommand : BrigadierCommand {

    fun openShop(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender
        if(player !is Player) return Command.SINGLE_SUCCESS
        val user = PlayerManager.instance.get(player) ?: throw CommandError("You are not in the game")
        if (user.team.isSpectatorTeam) throw CommandError("Spectators cannot open the shop")
        ShopInterface.open(player)
        return Command.SINGLE_SUCCESS
    }

    override fun create(registrar: Commands) {
        val node = LiteralArgumentBuilder.literal<CommandSourceStack>("shop").executes(::openShop).build()
        registrar.register(node)
    }

}