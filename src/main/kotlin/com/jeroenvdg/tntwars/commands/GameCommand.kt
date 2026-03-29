package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.CommandHelper
import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.commands.BrigadierCommand
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.tntwars.InfluenceType
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.MatchEndReason
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager
import com.jeroenvdg.tntwars.managers.mapManager.MapManager
import com.jeroenvdg.tntwars.player.PlayerManager
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import org.bukkit.GameMode

class GameCommand(
    private val mapManager: MapManager
) : BrigadierCommand {

    private fun start(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        TNTWars.instance.gameManager.startMatch()
        sender.sendMessage(TextHelper.deserialize("Started game"))
        return Command.SINGLE_SUCCESS
    }

    private fun stop(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        TNTWars.instance.gameManager.endMatch(MatchEndReason.StaffInterference)
        sender.sendMessage(TextHelper.deserialize("Stopped game"))
        return Command.SINGLE_SUCCESS
    }

    private fun leaveSystem(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        if (PlayerManager.instance.get(sender) == null) throw CommandError("You have not been released from the shackles of god")
        PlayerManager.instance.removePlayer(sender, false)
        sender.gameMode = GameMode.CREATIVE
        sender.inventory.clear()
        sender.sendMessage(TextHelper.deserialize("You have been freed from the shackles of god"))
        return Command.SINGLE_SUCCESS
    }

    private fun joinSystem(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        if (PlayerManager.instance[sender.uniqueId] != null) throw CommandError("You are already shackled by god")
        PlayerManager.instance.addPlayer(sender, false)
        sender.sendMessage(TextHelper.deserialize("The shackles of god have restrained you to the game"))
        return Command.SINGLE_SUCCESS
    }

    private fun setUserTeam(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val team = CommandHelper.getTeam(ctx)
        val twPlayer = CommandHelper.getTWPlayer(ctx)
        if (twPlayer != null) {
            if (twPlayer.team == team) throw CommandError("&p${twPlayer.bukkitPlayer.name}&r is already in team &s${team.name}")
            twPlayer.team = team
            sender.sendMessage(TextHelper.deserialize("Changed the team of &p${twPlayer.bukkitPlayer.name}&r to &s${team.name}"))
            return Command.SINGLE_SUCCESS
        }

        val users = CommandHelper.getTWPlayers(ctx)
        for (user in users) {
            if (user.team == team) continue
            user.team = team
            sender.sendMessage(TextHelper.deserialize("Changed the team of &p${user.bukkitPlayer.name}&r to &s${team.name}"))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun loadMap(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        TNTWars.instance.gameManager.loadMap(map)
        player.sendMessage(TextHelper.deserialize("Loaded map &p${map.id}"))
        return Command.SINGLE_SUCCESS
    }

    private fun loadRandomMap(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        TNTWars.instance.gameManager.loadRandomMap()
        player.sendMessage(TextHelper.deserialize("Loaded a random map"))
        return Command.SINGLE_SUCCESS
    }

    private fun getInspector(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        player.inventory.addItem(BlockOwnershipManager.tool)
        player.sendMessage(TextHelper.deserialize("Added ownership inspector tool to your inventory"))
        return Command.SINGLE_SUCCESS
    }

    private fun toggleBorder(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val user = PlayerManager.instance.get(player) ?: throw CommandError("You must obay god's will before exeuting this command")
        user.ignoreTeamBounds = !user.ignoreTeamBounds
        if (user.ignoreTeamBounds){
            player.sendMessage(TextHelper.deserialize("You now ignore team bounds"))
        } else {
            player.sendMessage(TextHelper.deserialize("You no longer ignore team bounds"))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun refreshGUI(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        try {
            TNTWars.instance.recreateGuis()
            player.sendMessage(TextHelper.deserialize("Refreshed guis"))
        } catch (e: Exception) {
            player.sendMessage(TextHelper.deserialize("Error refreshing guis, check console"))
            Debug.error(e)
        }
        return Command.SINGLE_SUCCESS
    }

    override fun create(registrar: Commands) {
        val node = literal<CommandSourceStack>("game")

                // /game start
                .then(literal<CommandSourceStack>("start")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }
                    .executes(::start)
                )

                // /game stop
                .then(literal<CommandSourceStack>("stop")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }
                    .executes(::stop)
                )

                // /game leave
                .then(literal<CommandSourceStack>("leave")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }
                    .executes(::leaveSystem)
                )

                // /game join
                .then(literal<CommandSourceStack>("join")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }
                    .executes(::joinSystem)
                )

                // /game setTeam <User> <Team>
                .then(literal<CommandSourceStack>("setTeam")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }

                    .then(argument<CommandSourceStack, String>("player", StringArgumentType.greedyString())

                        .then(argument<CommandSourceStack, String>("team", StringArgumentType.word())

                            .executes(::setUserTeam)
                        )
                    )
                )

                // /game loadMap <Map>
                .then(literal<CommandSourceStack>("loadMap")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }

                    .then(map(mapManager, ::loadMap))
                )

                // /game loadRandomMap
                .then(literal<CommandSourceStack>("loadRandomMap")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }
                    .executes(::loadRandomMap)
                )

                // /game setTNTStrength <Strength>
                .then(literal<CommandSourceStack>("setTNTStrength")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }

                    .then(argument<CommandSourceStack, Float>(
                        "strength",
                        FloatArgumentType.floatArg()
                    )

                        .executes {

                            val strength =
                                FloatArgumentType.getFloat(
                                    it,
                                    "strength"
                                )

                            GameManager.instance.activeMap.tntStrength =
                                if (strength < 0) -1f else strength

                            it.source.sender.sendMessage(
                                TextHelper.deserialize(
                                    "The tnt explosion strength has been overridden to &p${GameManager.instance.activeMap.tntStrength}"
                                )
                            )

                            1
                        }
                    )
                )

                // /game setFuseTicks <Ticks>
                .then(literal<CommandSourceStack>("setFuseTicks")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }

                    .then(argument<CommandSourceStack, Int>(
                        "ticks",
                        IntegerArgumentType.integer()
                    )

                        .executes {

                            val ticks =
                                IntegerArgumentType.getInteger(
                                    it,
                                    "ticks"
                                )

                            GameManager.instance.activeMap.fuseTicks =
                                if (ticks < 0) -1 else ticks

                            it.source.sender.sendMessage(
                                TextHelper.deserialize(
                                    "The tnt fuse ticks has been overridden to &p${GameManager.instance.activeMap.fuseTicks}"
                                )
                            )

                            1
                        }
                    )
                )

                // /game setTntCount <Count>
                .then(literal<CommandSourceStack>("setTntCount")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }

                    .then(argument<CommandSourceStack, Int>(
                        "count",
                        IntegerArgumentType.integer()
                    )

                        .executes {

                            val count =
                                IntegerArgumentType.getInteger(
                                    it,
                                    "count"
                                )

                            GameManager.instance.activeMap.tntCount =
                                if (count < 0) -1 else count

                            it.source.sender.sendMessage(
                                TextHelper.deserialize(
                                    "The tnt count has been overridden to &p${GameManager.instance.activeMap.tntCount}"
                                )
                            )

                            1
                        }
                    )
                )

                // /game giveCoins <Amount> <Player>
                .then(literal<CommandSourceStack>("giveCoins")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }

                    .then(argument<CommandSourceStack, Int>(
                        "amount",
                        IntegerArgumentType.integer(1)
                    )

                        .then(argument<CommandSourceStack, String>(
                            "player",
                            StringArgumentType.word()
                        )

                            .executes { ctx ->

                                val amount =
                                    IntegerArgumentType.getInteger(
                                        ctx,
                                        "amount"
                                    )

                                val playerName =
                                    StringArgumentType.getString(
                                        ctx,
                                        "player"
                                    )

                                val target =
                                    Bukkit.getPlayer(playerName)
                                        ?: throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("User must be online")

                                val targetUser =
                                    PlayerManager.instance.get(target)
                                        ?: throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("User must be online")

                                targetUser.stats.coins += amount

                                ctx.source.sender.sendMessage(
                                    TextHelper.deserialize(
                                        "You have given &p${target.name} &s$amount&r coins"
                                    )
                                )

                                target.sendMessage(
                                    TextHelper.deserialize(
                                        "You have been given &p$amount&r coins"
                                    )
                                )

                                1
                            }
                        )
                    )
                )

                // /game getInspector
                .then(literal<CommandSourceStack>("getInspector")
                    .executes(::getInspector)
                )

                // /game toggleBorder
                .then(literal<CommandSourceStack>("toggleBorder")
                    .executes(::toggleBorder)
                )

                // /game refreshGuis
                .then(literal<CommandSourceStack>("refreshGuis")
                    .requires { it.sender.hasPermission("tntwars.game.admin") }
                    .executes(::refreshGUI)
                )

        for(entry in InfluenceType.entries) {
            val additionalNode = literal<CommandSourceStack>(entry.name).then(entry.params)
            node.then(
                additionalNode
            )
        }

        registrar.register(node.build())
    }
}