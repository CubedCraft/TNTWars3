package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.SoundHelper
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.commands.BrigadierCommand
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.tntwars.player.PlayerManager
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

object TeamChatCommand : BrigadierCommand {

    private fun teamChat(ctx: CommandContext<CommandSourceStack>): Int {
        val message = try{ StringArgumentType.getString(ctx, "message") }catch(e: IllegalArgumentException) {null}
        val player = ctx.source.sender
        if(player !is Player) return Command.SINGLE_SUCCESS
        val user = PlayerManager.instance.get(player) ?: throw CommandError("You must be entangled in the system to use this command")

        if (!message.isNullOrEmpty()) {
            val wasInTeamChat = user.teamChatEnabled
            user.teamChatEnabled = true
            player.chat(message)
            user.teamChatEnabled = wasInTeamChat
        } else {
            user.teamChatEnabled = !user.teamChatEnabled
            if (user.teamChatEnabled) {
                player.sendMessage(Textial.deserialize("&6Team chat &aEnabled"))
            } else {
                player.sendMessage(Textial.deserialize("&6Team chat &cDisabled"))
            }
            SoundHelper.play(player, SoundHelper.Sounds.Success)
        }
        return Command.SINGLE_SUCCESS
    }

    override fun create(registrar: Commands) {
        val node = LiteralArgumentBuilder.literal<CommandSourceStack>("teamchat").executes(::teamChat).build()
        registrar.register(node)
        registrar.register(LiteralArgumentBuilder.literal<CommandSourceStack>("tc").redirect(node).build())
    }

}