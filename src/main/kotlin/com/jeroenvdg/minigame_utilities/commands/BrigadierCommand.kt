package com.jeroenvdg.minigame_utilities.commands

import com.jeroenvdg.minigame_utilities.CommandHelper
import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.MapManager
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

interface BrigadierCommand {
    fun create(registrar: Commands)
    fun map(mapManager: MapManager?): RequiredArgumentBuilder<CommandSourceStack, String> {
        val node = RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
        if (mapManager != null) {
            node.suggests { ctx, builder ->
                mapManager.forEach { map -> builder.suggest(map.id) }
                builder.buildFuture()
            }
        }
        return node
    }
    fun map(mapManager: MapManager?, executes: (CommandContext<CommandSourceStack>) -> Int): RequiredArgumentBuilder<CommandSourceStack, String> {
        val node = RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
        node.executes(executes)
        if (mapManager != null) {
            node.suggests { ctx, builder ->
                mapManager.forEach { map -> builder.suggest(map.id) }
                builder.buildFuture()
            }
        }
        return node
    }
    fun team(): RequiredArgumentBuilder<CommandSourceStack, String> {
        val node = RequiredArgumentBuilder.argument<CommandSourceStack, String>("team", StringArgumentType.word())
        node.suggests { ctx, builder ->
            Team.entries.forEach { team -> builder.suggest(team.name) }
            builder.buildFuture()
        }
        return node
    }
    fun team(executes: (CommandContext<CommandSourceStack>) -> Int): RequiredArgumentBuilder<CommandSourceStack, String> {
        val node = RequiredArgumentBuilder.argument<CommandSourceStack, String>("team", StringArgumentType.word())
        node.executes(executes)
        node.suggests { ctx, builder ->
            Team.entries.forEach { team -> builder.suggest(team.name) }
            builder.buildFuture()
        }
        return node
    }
    fun help(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = ctx.source.sender
        val root = ctx.nodes.last().node
        val name = root.name
        val page = CommandHelper.getIntegerOrNull(ctx, "page") ?: 1
        val pageData = PageData(page, 7, root.children.size)
        paginate(TextHelper.deserialize("&f/$name"), pageData) {
            i ->
            val command = root.children.elementAt(i)
            TextHelper.deserialize()
        }
        return Command.SINGLE_SUCCESS
    }
}