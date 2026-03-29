package com.jeroenvdg.minigame_utilities

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.MapManager
import com.jeroenvdg.tntwars.managers.mapManager.TNTWarsMap
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

object CommandHelper {
    inline fun<reified T> getSender(ctx: CommandContext<CommandSourceStack>): T? {
        val sender = ctx.source.sender
        if(sender !is T) {
            sender.sendMessage(Component.text("This command can only be executed by ${if(sender is Player) "players" else "console"}").color(
                NamedTextColor.RED))
            return null
        }
        return sender
    }

    fun getPlayer(ctx: CommandContext<CommandSourceStack>): Player? {
        return getSender<Player>(ctx)
    }

    fun getConsole(ctx: CommandContext<CommandSourceStack>): ConsoleCommandSender? {
        return getSender<ConsoleCommandSender>(ctx)
    }

    fun getMap(ctx: CommandContext<CommandSourceStack>, mapManager: MapManager): TNTWarsMap? {
        val mapName = StringArgumentType.getString(ctx, "map")
        val map = mapManager.find(mapName)
        if(map == null) {
            ctx.source.sender.sendMessage(TextHelper.prefix + "&6$mapName &cdoes not exist!")
        }
        return map
    }

    fun getTeam(ctx: CommandContext<CommandSourceStack>): Team {
        val team = Team.valueOf(StringArgumentType.getString(ctx, "team"))
        return team
    }

    fun getTWPlayer(ctx: CommandContext<CommandSourceStack>): TNTWarsPlayer? {
        val twPlayerName = getStringOrNull(ctx, "player") ?: return null
        val bukkitPlayer = TNTWars.instance.server.onlinePlayers.find{it.name == twPlayerName}
        return PlayerManager.instance.get(bukkitPlayer)
    }

    fun getTWPlayers(ctx: CommandContext<CommandSourceStack>): List<TNTWarsPlayer> {
        val twPlayerNames = getStringOrNull(ctx, "player")?.split(" ") ?: return emptyList()
        val twPlayers = twPlayerNames.map{
                name ->
            val twPlayer = PlayerManager.instance.get(TNTWars.instance.server.onlinePlayers.find { it.name == name  })
            twPlayer
                ?: throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("$name does not exist!")
        }
        return twPlayers
    }

    fun getStringOrNull(ctx: CommandContext<CommandSourceStack>, name: String): String? {
        return try {
            StringArgumentType.getString(ctx, name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun getIntegerOrNull(ctx: CommandContext<CommandSourceStack>, name: String): Int? {
        return try {
            IntegerArgumentType.getInteger(ctx, name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}